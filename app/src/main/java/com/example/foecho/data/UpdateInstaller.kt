package com.example.foecho.data

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Environment
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

private const val KEY_READY_URI = "ready_uri"
private const val KEY_READY_VERSION = "ready_version"

data class ReadyInstall(val uri: String, val version: String)

object InstallReady {
    var current by mutableStateOf<ReadyInstall?>(null)

    fun set(context: Context, item: ReadyInstall) {
        current = item
        context.getSharedPreferences(UPDATE_PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_READY_URI, item.uri)
            .putString(KEY_READY_VERSION, item.version)
            .apply()
    }

    fun load(context: Context) {
        if (current != null) return
        val prefs = context.getSharedPreferences(UPDATE_PREFS, Context.MODE_PRIVATE)
        val uri = prefs.getString(KEY_READY_URI, null) ?: return
        val version = prefs.getString(KEY_READY_VERSION, "") ?: ""
        if (!isStillNeeded(context, uri, version)) {
            finish(context)
            return
        }
        current = ReadyInstall(uri, version)
    }

    private fun isStillNeeded(context: Context, uri: String, version: String): Boolean {
        val alreadyInstalled = version.isNotBlank() &&
                !isNewer(version, installedVersion(context))
        if (alreadyInstalled) return false
        val apkStillThere = runCatching {
            context.contentResolver.openFileDescriptor(Uri.parse(uri), "r")?.use { true }
        }.getOrNull() == true
        return apkStillThere
    }

    fun dismissForNow() {
        current = null
    }

    fun finish(context: Context) {
        current = null
        context.getSharedPreferences(UPDATE_PREFS, Context.MODE_PRIVATE).edit()
            .remove(KEY_READY_URI)
            .remove(KEY_READY_VERSION)
            .apply()
    }
}

fun startUpdateDownload(context: Context) {
    val release = pendingRelease(context)
    val apkUrl = release?.apkUrl
    if (release == null || apkUrl == null) {
        Toast.makeText(context, "No download found for this release", Toast.LENGTH_SHORT).show()
        return
    }

    val request = DownloadManager.Request(Uri.parse(apkUrl))
        .setTitle("FoEcho update")
        .setDescription("Downloading version ${release.version}")
        .setMimeType("application/vnd.android.package-archive")
        .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        .setDestinationInExternalPublicDir(
            Environment.DIRECTORY_DOWNLOADS,
            "FoEcho-${release.version}.apk"
        )

    val id = context.getSystemService(DownloadManager::class.java).enqueue(request)
    context.getSharedPreferences(UPDATE_PREFS, Context.MODE_PRIVATE).edit()
        .putLong(KEY_DOWNLOAD_ID, id)
        .apply()
    Toast.makeText(context, "Downloading update...", Toast.LENGTH_SHORT).show()
}

fun installApk(context: Context, uri: String) {
    val intent = Intent(Intent.ACTION_VIEW)
        .setDataAndType(Uri.parse(uri), "application/vnd.android.package-archive")
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    context.startActivity(intent)
}

class UpdateActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == ACTION_DOWNLOAD_UPDATE) startUpdateDownload(context)
    }
}

class DownloadCompleteReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
        val saved = context.getSharedPreferences(UPDATE_PREFS, Context.MODE_PRIVATE)
            .getLong(KEY_DOWNLOAD_ID, -1L)
        if (id == -1L || id != saved) return

        val manager = context.getSystemService(DownloadManager::class.java)
        val succeeded = manager.query(DownloadManager.Query().setFilterById(id))?.use { cursor ->
            cursor.moveToFirst() &&
                    cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS)) ==
                    DownloadManager.STATUS_SUCCESSFUL
        } ?: false
        if (!succeeded) return

        val uri = manager.getUriForDownloadedFile(id) ?: return
        val version = pendingRelease(context)?.version ?: ""
        InstallReady.set(context, ReadyInstall(uri.toString(), version))
    }
}