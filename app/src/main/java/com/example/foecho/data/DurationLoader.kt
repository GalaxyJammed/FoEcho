package com.example.foecho.data

import android.content.Context
import android.net.Uri

suspend fun loadDuration(context: Context, uri: Uri, fallbackMs: Long): Long =
    loadAudioInfo(context, uri)?.durationMs?.takeIf { it > 0 } ?: fallbackMs