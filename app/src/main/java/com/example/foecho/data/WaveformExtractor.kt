package com.example.foecho.data

import android.content.Context
import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.nio.ByteOrder
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import kotlin.math.abs

private const val POINTS = 1200
private const val MIN_BLOCK = 16
private const val DEFAULT_BLOCK = 1024

data class AudioInfo(val durationMs: Long, val bars: FloatArray)

private val memoryCache = ConcurrentHashMap<String, AudioInfo>()
private val locks = ConcurrentHashMap<String, Mutex>()

suspend fun loadAudioInfo(context: Context, uri: Uri): AudioInfo? = withContext(Dispatchers.IO) {
    val key = uri.toString()
    memoryCache[key]?.let { return@withContext it }
    val lock = locks.getOrPut(key) { Mutex() }
    lock.withLock {
        memoryCache[key]?.let { return@withLock it }
        val file = cacheFile(context, key)
        val fromDisk = readDisk(file)
        if (fromDisk != null) {
            memoryCache[key] = fromDisk
            return@withLock fromDisk
        }
        val decoded = runCatching { decode(context, uri) }.getOrNull() ?: return@withLock null
        memoryCache[key] = decoded
        writeDisk(file, decoded)
        decoded
    }
}

suspend fun loadWaveform(context: Context, uri: Uri): FloatArray? =
    loadAudioInfo(context, uri)?.bars

private fun cacheFile(context: Context, key: String): File {
    val dir = File(context.cacheDir, "waveforms").apply { mkdirs() }
    val hash = MessageDigest.getInstance("SHA-256")
        .digest(key.toByteArray())
        .joinToString("") { "%02x".format(it) }
    return File(dir, "${hash}_v2_p$POINTS.txt")
}

private fun readDisk(file: File): AudioInfo? {
    if (!file.exists()) return null
    return runCatching {
        val lines = file.readLines()
        val duration = lines[0].toLong()
        val bars = lines[1].split(',').map { it.toFloat() }.toFloatArray()
        AudioInfo(duration, bars)
    }.getOrNull()
}

private fun writeDisk(file: File, info: AudioInfo) {
    runCatching {
        file.writeText("${info.durationMs}\n${info.bars.joinToString(",")}")
    }
}

private fun MediaFormat.intOr(key: String, default: Int): Int =
    if (containsKey(key)) getInteger(key) else default

private fun decode(context: Context, uri: Uri): AudioInfo? {
    val extractor = MediaExtractor()
    var codec: MediaCodec? = null
    try {
        extractor.setDataSource(context, uri, null)
        var trackIndex = -1
        var format: MediaFormat? = null
        for (i in 0 until extractor.trackCount) {
            val trackFormat = extractor.getTrackFormat(i)
            val mime = trackFormat.getString(MediaFormat.KEY_MIME) ?: continue
            if (mime.startsWith("audio/")) {
                trackIndex = i
                format = trackFormat
                break
            }
        }
        if (trackIndex < 0 || format == null) return null
        extractor.selectTrack(trackIndex)
        val mime = format.getString(MediaFormat.KEY_MIME) ?: return null
        val decoder = MediaCodec.createDecoderByType(mime)
        codec = decoder
        decoder.configure(format, null, null, 0)
        decoder.start()

        val rateGuess = format.intOr(MediaFormat.KEY_SAMPLE_RATE, 44100)
        val channelsGuess = format.intOr(MediaFormat.KEY_CHANNEL_COUNT, 1)
        val durationUs = if (format.containsKey(MediaFormat.KEY_DURATION)) {
            format.getLong(MediaFormat.KEY_DURATION)
        } else {
            0L
        }
        val expectedSamples = durationUs * rateGuess / 1_000_000L * channelsGuess
        val block = if (expectedSamples > 0L) {
            (expectedSamples / POINTS).coerceAtLeast(MIN_BLOCK.toLong()).toInt()
        } else {
            DEFAULT_BLOCK
        }

        var sampleRate = rateGuess
        var channels = channelsGuess
        var totalSamples = 0L
        var peaks = FloatArray(POINTS + 16)
        var peakCount = 0
        var blockMax = 0
        var blockCount = 0
        val info = MediaCodec.BufferInfo()
        var inputDone = false
        var outputDone = false

        while (!outputDone) {
            if (!inputDone) {
                val inIndex = decoder.dequeueInputBuffer(10_000)
                if (inIndex >= 0) {
                    val buffer = decoder.getInputBuffer(inIndex) ?: return null
                    val size = extractor.readSampleData(buffer, 0)
                    if (size < 0) {
                        decoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        inputDone = true
                    } else {
                        decoder.queueInputBuffer(inIndex, 0, size, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }
            }

            val outIndex = decoder.dequeueOutputBuffer(info, 10_000)
            when {
                outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    val out = decoder.outputFormat
                    sampleRate = out.intOr(MediaFormat.KEY_SAMPLE_RATE, sampleRate)
                    channels = out.intOr(MediaFormat.KEY_CHANNEL_COUNT, channels)
                }
                outIndex >= 0 -> {
                    if (info.size > 0) {
                        val raw = decoder.getOutputBuffer(outIndex)
                        if (raw != null) {
                            raw.position(info.offset)
                            raw.limit(info.offset + info.size)
                            val shorts = raw.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                            val samples = ShortArray(shorts.remaining())
                            shorts.get(samples)
                            totalSamples += samples.size
                            for (s in samples) {
                                val value = abs(s.toInt())
                                if (value > blockMax) blockMax = value
                                blockCount++
                                if (blockCount == block) {
                                    if (peakCount == peaks.size) peaks = peaks.copyOf(peaks.size * 2)
                                    peaks[peakCount++] = blockMax / 32768f
                                    blockMax = 0
                                    blockCount = 0
                                }
                            }
                        }
                    }
                    decoder.releaseOutputBuffer(outIndex, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
                }
            }
        }

        if (blockCount > 0) {
            if (peakCount == peaks.size) peaks = peaks.copyOf(peaks.size * 2)
            peaks[peakCount++] = blockMax / 32768f
        }
        if (peakCount == 0) return null

        val durationMs = if (sampleRate > 0 && channels > 0) {
            totalSamples * 1000L / (sampleRate.toLong() * channels)
        } else {
            0L
        }

        val resampled = resample(peaks, peakCount, POINTS)
        val top = resampled.maxOrNull() ?: return null
        if (top <= 0f) return null
        return AudioInfo(durationMs, FloatArray(POINTS) { resampled[it] / top })
    } finally {
        codec?.let {
            runCatching { it.stop() }
            it.release()
        }
        extractor.release()
    }
}

private fun resample(peaks: FloatArray, count: Int, target: Int): FloatArray {
    return if (count >= target) {
        FloatArray(target) { i ->
            val start = i * count / target
            val end = maxOf(start + 1, (i + 1) * count / target)
            var max = 0f
            for (k in start until minOf(end, count)) {
                if (peaks[k] > max) max = peaks[k]
            }
            max
        }
    } else {
        FloatArray(target) { i ->
            val pos = i * (count - 1).toFloat() / (target - 1).coerceAtLeast(1)
            val lo = pos.toInt().coerceIn(0, count - 1)
            val hi = (lo + 1).coerceAtMost(count - 1)
            val frac = pos - lo
            peaks[lo] * (1f - frac) + peaks[hi] * frac
        }
    }
}