package com.example.foecho.data

import android.content.ContentValues
import android.content.Context
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class SavedRecording(val uri: Uri, val baseName: String)

class VoiceRecorder(private val context: Context) {
    private var recorder: MediaRecorder? = null
    private var descriptor: ParcelFileDescriptor? = null
    private var pendingUri: Uri? = null
    private var pendingBase = ""
    private var accumulatedMs = 0L
    private var segmentStart = 0L
    private var paused = false

    val elapsedMs: Long
        get() {
            if (recorder == null) return 0L
            val live = if (paused) 0L else System.currentTimeMillis() - segmentStart
            return accumulatedMs + live
        }

    fun start(): Boolean {
        if (recorder != null) return false
        val base = "FoEcho_" + SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val values = ContentValues().apply {
            put(MediaStore.Audio.Media.DISPLAY_NAME, "$base.m4a")
            put(MediaStore.Audio.Media.MIME_TYPE, "audio/mp4")
            put(MediaStore.Audio.Media.RELATIVE_PATH, "Recordings/FoEcho")
            put(MediaStore.Audio.Media.IS_PENDING, 1)
        }
        val uri = context.contentResolver.insert(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, values)
            ?: return false
        val pfd = runCatching { context.contentResolver.openFileDescriptor(uri, "w") }.getOrNull()
        if (pfd == null) {
            context.contentResolver.delete(uri, null, null)
            return false
        }
        val mr = createRecorder()
        return try {
            mr.setAudioSource(MediaRecorder.AudioSource.MIC)
            mr.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            mr.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            mr.setAudioEncodingBitRate(128_000)
            mr.setAudioSamplingRate(44_100)
            mr.setOutputFile(pfd.fileDescriptor)
            mr.prepare()
            mr.start()
            recorder = mr
            descriptor = pfd
            pendingUri = uri
            pendingBase = base
            accumulatedMs = 0L
            paused = false
            segmentStart = System.currentTimeMillis()
            true
        } catch (e: Exception) {
            mr.release()
            pfd.close()
            context.contentResolver.delete(uri, null, null)
            false
        }
    }

    fun pause(): Boolean {
        val mr = recorder ?: return false
        if (paused) return false
        return try {
            mr.pause()
            accumulatedMs += System.currentTimeMillis() - segmentStart
            paused = true
            true
        } catch (e: Exception) {
            false
        }
    }

    fun resume(): Boolean {
        val mr = recorder ?: return false
        if (!paused) return false
        return try {
            mr.resume()
            segmentStart = System.currentTimeMillis()
            paused = false
            true
        } catch (e: Exception) {
            false
        }
    }

    fun stop(): SavedRecording? {
        val mr = recorder ?: return null
        val uri = pendingUri
        val base = pendingBase
        val ok = runCatching { mr.stop() }.isSuccess
        mr.release()
        descriptor?.close()
        recorder = null
        descriptor = null
        pendingUri = null
        pendingBase = ""
        accumulatedMs = 0L
        paused = false
        if (uri == null) return null
        if (!ok) {
            context.contentResolver.delete(uri, null, null)
            return null
        }
        context.contentResolver.update(
            uri,
            ContentValues().apply { put(MediaStore.Audio.Media.IS_PENDING, 0) },
            null,
            null
        )
        return SavedRecording(uri, base)
    }

    @Suppress("DEPRECATION")
    private fun createRecorder(): MediaRecorder =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            MediaRecorder(context)
        } else {
            MediaRecorder()
        }
}

fun renameRecording(context: Context, uri: Uri, newName: String): Boolean {
    val clean = newName.replace(Regex("[\\\\/:*?\"<>|]"), "").trim()
    if (clean.isEmpty()) return false
    val values = ContentValues().apply {
        put(MediaStore.Audio.Media.DISPLAY_NAME, "$clean.m4a")
    }
    return runCatching { context.contentResolver.update(uri, values, null, null) > 0 }
        .getOrDefault(false)
}