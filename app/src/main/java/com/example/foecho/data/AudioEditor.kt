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
private const val MaxPendingChunks = 16

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
    endMs: Long,
    onProgress: (Float) -> Unit = {}
): String? {
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
            streamExport(context, source, startMs, endMs, pfd, onProgress)
        }
    }.getOrDefault(false)
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
    onProgress(1f)
    return uri.toString()
}

private fun sanitizeName(name: String): String =
    name.replace(Regex("[\\\\/:*?\"<>|]"), "").trim()

private fun streamExport(
    context: Context,
    source: Uri,
    startMs: Long,
    endMs: Long,
    pfd: ParcelFileDescriptor,
    onProgress: (Float) -> Unit
): Boolean {
    val extractor = MediaExtractor()
    var decoder: MediaCodec? = null
    var encoder: MediaCodec? = null
    var muxer: MediaMuxer? = null
    try {
        extractor.setDataSource(context, source, null)
        val track = (0 until extractor.trackCount).firstOrNull { i ->
            extractor.getTrackFormat(i).getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true
        } ?: return false
        extractor.selectTrack(track)
        val inFormat = extractor.getTrackFormat(track)
        val mime = inFormat.getString(MediaFormat.KEY_MIME) ?: return false

        val startUs = startMs * 1000L
        val endUs = endMs * 1000L
        val spanUs = (endUs - startUs).coerceAtLeast(1L).toFloat()
        extractor.seekTo(startUs, MediaExtractor.SEEK_TO_PREVIOUS_SYNC)

        val dec = MediaCodec.createDecoderByType(mime)
        decoder = dec
        dec.configure(inFormat, null, null, 0)
        dec.start()

        var decInputDone = false
        fun feedDecoder() {
            val inIdx = dec.dequeueInputBuffer(0L)
            if (inIdx < 0) return
            val buf = dec.getInputBuffer(inIdx) ?: return
            val size = extractor.readSampleData(buf, 0)
            if (size < 0 || extractor.sampleTime > endUs) {
                dec.queueInputBuffer(inIdx, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                decInputDone = true
            } else {
                dec.queueInputBuffer(inIdx, 0, size, extractor.sampleTime, 0)
                extractor.advance()
            }
        }

        val info = MediaCodec.BufferInfo()
        var sampleRate = 0
        var channels = 0
        var tries = 0
        while (sampleRate == 0 && tries++ < 2000) {
            feedDecoder()
            val idx = dec.dequeueOutputBuffer(info, 10_000L)
            if (idx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                val f = dec.outputFormat
                sampleRate = f.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                channels = f.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            } else if (idx >= 0) {
                dec.releaseOutputBuffer(idx, false)
            }
        }
        if (sampleRate == 0 || channels <= 0) return false

        val frameBytes = 2 * channels
        val encFormat = MediaFormat.createAudioFormat(
            MediaFormat.MIMETYPE_AUDIO_AAC, sampleRate, channels
        ).apply {
            setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC)
            setInteger(MediaFormat.KEY_BIT_RATE, 128_000)
            setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16_384)
        }
        val enc = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC)
        encoder = enc
        enc.configure(encFormat, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE)
        enc.start()
        val mx = MediaMuxer(pfd.fileDescriptor, MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4)
        muxer = mx

        val pending = ArrayDeque<ByteArray>()
        var pendingOffset = 0
        var decOutputDone = false
        var encInputDone = false
        var encOutputDone = false
        var framesSent = 0L
        var outTrack = -1
        var muxerStarted = false
        val encInfo = MediaCodec.BufferInfo()

        while (!encOutputDone) {
            if (!decOutputDone && pending.size < MaxPendingChunks) {
                val idx = dec.dequeueOutputBuffer(info, 0L)
                if (idx >= 0) {
                    val pts = info.presentationTimeUs
                    if (info.size > 0 && pts < endUs) {
                        val buf = dec.getOutputBuffer(idx)
                        if (buf != null) {
                            val frames = info.size / frameBytes
                            val skip = ((startUs - pts) * sampleRate / 1_000_000L)
                                .coerceIn(0L, frames.toLong()).toInt()
                            val keep = ((endUs - pts) * sampleRate / 1_000_000L)
                                .coerceIn(0L, frames.toLong()).toInt()
                            if (keep > skip) {
                                val bytes = ByteArray((keep - skip) * frameBytes)
                                buf.limit(info.offset + info.size)
                                buf.position(info.offset + skip * frameBytes)
                                buf.get(bytes)
                                pending.addLast(bytes)
                            }
                        }
                        onProgress(((pts - startUs) / spanUs).coerceIn(0f, 0.99f))
                    }
                    if (pts >= endUs || (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        decOutputDone = true
                    }
                    dec.releaseOutputBuffer(idx, false)
                }
            }

            if (!decInputDone) feedDecoder()

            if (!encInputDone && (pending.isNotEmpty() || decOutputDone)) {
                val inIdx = enc.dequeueInputBuffer(0L)
                if (inIdx >= 0) {
                    val ptsUs = framesSent * 1_000_000L / sampleRate
                    if (pending.isEmpty()) {
                        enc.queueInputBuffer(inIdx, 0, 0, ptsUs, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                        encInputDone = true
                    } else {
                        val chunk = pending.first()
                        val buf = enc.getInputBuffer(inIdx)
                            ?: throw IllegalStateException("No encoder input buffer")
                        val n = minOf(chunk.size - pendingOffset, buf.capacity()) / frameBytes * frameBytes
                        if (n <= 0) throw IllegalStateException("Encoder buffer too small")
                        buf.clear()
                        buf.put(chunk, pendingOffset, n)
                        enc.queueInputBuffer(inIdx, 0, n, ptsUs, 0)
                        framesSent += n / frameBytes
                        pendingOffset += n
                        if (pendingOffset >= chunk.size) {
                            pending.removeFirst()
                            pendingOffset = 0
                        }
                    }
                }
            }

            val outIdx = enc.dequeueOutputBuffer(encInfo, 5_000L)
            when {
                outIdx == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    outTrack = mx.addTrack(enc.outputFormat)
                    mx.start()
                    muxerStarted = true
                }
                outIdx >= 0 -> {
                    val buf = enc.getOutputBuffer(outIdx)
                    if (buf != null && muxerStarted && encInfo.size > 0 &&
                        (encInfo.flags and MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0
                    ) {
                        buf.limit(encInfo.offset + encInfo.size)
                        buf.position(encInfo.offset)
                        mx.writeSampleData(outTrack, buf, encInfo)
                    }
                    enc.releaseOutputBuffer(outIdx, false)
                    if ((encInfo.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) {
                        encOutputDone = true
                    }
                }
            }
        }
        return muxerStarted && framesSent > 0
    } finally {
        runCatching { encoder?.stop() }
        encoder?.release()
        runCatching { muxer?.stop() }
        muxer?.release()
        runCatching { decoder?.stop() }
        decoder?.release()
        extractor.release()
    }
}