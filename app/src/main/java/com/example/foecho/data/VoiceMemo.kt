package com.example.foecho.data

import android.content.Context
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class VoiceMemo(
    val id: Long,
    val name: String,
    val durationMs: Long,
    val dateAdded: Long,
    val dateModified: Long,
    val sizeBytes: Long
)

suspend fun loadVoiceMemos(context: Context): List<VoiceMemo> = withContext(Dispatchers.IO) {
    val projection = arrayOf(
        MediaStore.Audio.Media._ID,
        MediaStore.Audio.Media.DISPLAY_NAME,
        MediaStore.Audio.Media.DURATION,
        MediaStore.Audio.Media.DATE_ADDED,
        MediaStore.Audio.Media.DATE_MODIFIED,
        MediaStore.Audio.Media.SIZE
    )
    val memos = mutableListOf<VoiceMemo>()
    context.contentResolver.query(
        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
        projection,
        null,
        null,
        "${MediaStore.Audio.Media.DATE_ADDED} DESC"
    )?.use { cursor ->
        val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
        val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME)
        val durationCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
        val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_ADDED)
        val dateModifiedCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATE_MODIFIED)
        val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
        while (cursor.moveToNext()) {
            memos += VoiceMemo(
                id = cursor.getLong(idCol),
                name = cursor.getString(nameCol) ?: "Unknown",
                durationMs = cursor.getLong(durationCol),
                dateAdded = cursor.getLong(dateAddedCol),
                dateModified = cursor.getLong(dateModifiedCol),
                sizeBytes = cursor.getLong(sizeCol)
            )
        }
    }
    memos
}