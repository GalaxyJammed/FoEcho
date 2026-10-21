package com.example.foecho.data

import android.content.ContentValues
import android.content.Context
import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaExtractor
import android.media.MediaFormat
import android.media.MediaMuxer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream

data class EditedSegment(
    val id: String,
    val sourceUri: String,
    val sourceName: String,
    val partName: String,
    val startMs: Long,
    val endMs: Long,
    val outputUri: String
)

private const val EDITS_PREFS = "foecho_edits"
private const val KEY_EDITS = "segments"

fun loadEdits(context: Context): List<EditedSegment> {
    val raw = context.getSharedPreferences(EDITS_PREFS, Context.MODE_PRIVATE)
        .getString(KEY_EDITS, null) ?: return emptyList()
    val array = JSONArray(raw)
    return List(array.length()) { i ->
        val o = array.getJSONObject(i)
        EditedSegment(
            id = o.getString("id"),
            sourceUri = o.getString("sourceUri"),
            sourceName = o.getString("sourceName"),
            partName = o.getString("partName"),
            startMs = o.getLong("startMs"),
            endMs = o.getLong("endMs"),
            outputUri = o.getString("outputUri")
        )
    }
}

fun saveEdits(context: Context, segments: List<EditedSegment>) {
    val array = JSONArray()
    segments.forEach { s ->
        array.put(
            JSONObject()
                .put("id", s.id)
                .put("sourceUri", s.sourceUri)
                .put("sourceName", s.sourceName)
                .put("partName", s.partName)
                .put("startMs", s.startMs)
                .put("endMs", s.endMs)
                .put("outputUri", s.outputUri)
        )
    }
    context.getSharedPreferences(EDITS_PREFS, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_EDITS, array.toString())
        .apply()
}

fun exportSegment(
    context: Context,
    source: Uri,
    sourceName: String,
    partName: String,
    startMs: Long,
    endMs: Long
): String? {
    val pcm = decodeRange(context, source, startMs, endMs) ?: return null
    val base = sanitizeName("$sourceName - $partName").ifBlank { "FoEcho_Part" }
    val resolver = context.contentResolver
    val values = ContentValues().apply {
        put(MediaStore.Audio.Media.DISPLAY_NAME, "$base.m4a")
        put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")
        put(MediaStore.Audio.Media.RELATIVE_PATH, "Recordings/FoEcho")
        put(MediaStore.Audio.Media.IS_PENDING, 1)
    }
    val uri = resolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values) ?: return null
    val ok = runCatching {
        resolver.openFileDescriptor(uri, "w")!!.use { pfd ->
            encodePcm(pcm.bytes, pcm.sampleRate, pcm.channels, pfd)
        }
    }.isSuccess
    if (!ok) {
        resolver.delete(uri, null, null)
        return null
    }
    resolver.update(
        uri,
        ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 0) },
        null,
        null
    )
    return uri.toString()
}

private fun sanitizeName(name: String): String =
    name.replace(Regex("[\\\\/:*?\"<>|]"), "").trim()

private class DecodedPcm(val bytes: ByteArray, val sampleRate: Int, val channels: Int)

private fun decodeRange(context: Context, uri: Uri, startMs: Long, endMs: Long): DecodedPcm? {
    val extractor = MediaExtractor()
    var codec: MediaCodec? = null
    try {
        extractor.setDataSource(context, uri, null)
        val track = (0 until extractor.trackCount).firstOrNull { i ->
            extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
        } ?: return null
        extractor.selectTrack(track)
        val format = extractor.getTrackFormat(track)
        val mime = format.getString(MediaFormat.KEY_MIME) ?: return null
        val startUs = startMs * 1000L
        val endUs = endMs * 1000L
        extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

        val decoder = MediaCodec.createDecoderByType(mime)
        codec = decoder
        decoder.configure(format, null, null, 0)
        decoder.start()

        var sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
        var channels = format.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
        val out = ByteArrayOutputStream()
        val info = MediaCodec.BufferInfo()
        var inputDone = false
        var outputDone = false

        while (!outputDone) {
            if (!inputDone) {
                val inIndex = decoder.dequeueInputBuffer(10_000L)
                if (inIndex >= 0) {
                    val buf = decoder.getInputBuffer(inIndex) ?: return null
                    val size = extractor.readSampleData(buf, 0)
                    if (size < 0 || extractor.sampleTime > endUs) {
                        decoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        inputDone = true
                    } else {
                        decoder.queueInputBuffer(inIndex, 0, size, extractor.sampleTime, 0)
                        extractor.advance()
                    }
                }
            }

            val outIndex = decoder.dequeueOutputBuffer(info, 10_000L)
            when {
                outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    val f = decoder.outputFormat
                    sampleRate = f.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    channels = f.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                }
                outIndex >= 0 -> {
                    val frameBytes = 2 * channels
                    if (info.size > 0 && info.presentationTimeUs < endUs) {
                        val buf = decoder.getOutputBuffer(outIndex)
                        if (buf != null) {
                            val frames = info.size / frameBytes
                            val chunkStart = info.presentationTimeUs
                            val skip = ((startUs - chunkStart) * sampleRate / 1_000_000L)
                                .coerceIn(0L, frames.toLong()).toInt()
                            val keep = ((endUs - chunkStart) * sampleRate / 1_000_000L)
                                .coerceIn(0L, frames.toLong()).toInt()
                            if (keep > skip) {
                                val bytes = ByteArray((keep - skip) * frameBytes)
                                buf.limit(info.offset + info.size)
                                buf.position(info.offset + skip * frameBytes)
                                buf.get(bytes)
                                out.write(bytes)
                            }
                        }
                    }
                    if (info.presentationTimeUs >= endUs ||
                        (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0
                    ) {
                        outputDone = true
                    }
                    decoder.releaseOutputBuffer(outIndex, false)
                }
            }
        }

        val bytes = out.toByteArray()
        if (bytes.isEmpty()) return null
        return DecodedPcm(bytes, sampleRate, channels)
    } finally {
        codec?.let {
            runCatching { it.stop() }
            it.release()
        }
        extractor.release()
    }
}

private fun encodePcm(pcm: ByteArray, sampleRate: Int, channels: Int, pfd: ParcelFileDescriptor) {
    val frameBytes = 2 * channels
    val format = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channels).apply {
        setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
        setInteger(MediaFormat.KEY_BIT_RATE, 128_000)
        setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16_384)
    }
    val encoder = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
    encoder.configure(format, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
    encoder.start()
    val muxer = MediaMuxer(pfd.fileDescriptor, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
    try {
        var track = -1
        var started = false
        var inOffset = 0
        var framesSent = 0L
        var inputDone = false
        var outputDone = false
        val info = MediaCodec.BufferInfo()

        while (!outputDone) {
            if (!inputDone) {
                val inIndex = encoder.dequeueInputBuffer(10_000L)
                if (inIndex >= 0) {
                    val buf = encoder.getInputBuffer(inIndex) ?: error("No encoder input buffer")
                    val remaining = pcm.size - inOffset
                    if (remaining <= 0) {
                        encoder.queueInputBuffer(inIndex, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        inputDone = true
                    } else {
                        val n = minOf(remaining, buf.capacity()) / frameBytes * frameBytes
                        buf.clear()
                        buf.put(pcm, inOffset, n)
                        encoder.queueInputBuffer(inIndex, 0, n, framesSent * 1_000_000L / sampleRate, 0)
                        framesSent += n / frameBytes
                        inOffset += n
                    }
                }
            }

            val outIndex = encoder.dequeueOutputBuffer(info, 10_000L)
            when {
                outIndex == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    track = muxer.addTrack(encoder.outputFormat)
                    muxer.start()
                    started = true
                }
                outIndex >= 0 -> {
                    val buf = encoder.getOutputBuffer(outIndex)
                    if (buf != null && started && info.size > 0 &&
                        (info.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0
                    ) {
                        buf.limit(info.offset + info.size)
                        buf.position(info.offset)
                        muxer.writeSampleData(track, buf, info)
                    }
                    encoder.releaseOutputBuffer(outIndex, false)
                    if ((info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) outputDone = true
                }
            }
        }
    } finally {
        runCatching { encoder.stop() }
        encoder.release()
        runCatching { muxer.stop() }
        muxer.release()
    }
}