package com.example.foecho.data

import android.Manifest
import android.app.Activity
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

private const val RELEASES_URL = "https://api.github.com/repos/GalaxyJammed/FoEcho/releases/latest"
internal const val UPDATE_PREFS = "foecho_updates"
internal const val KEY_DOWNLOAD_ID = "download_id"
private const val KEY_NOTIFIED = "notified_version"
private const val KEY_PENDING_VERSION = "pending_version"
private const val KEY_PENDING_URL = "pending_url"
private const val KEY_PENDING_APK = "pending_apk"
internal const val CHANNEL_ID = "foecho_updates"
internal const val ACTION_DOWNLOAD_UPDATE = "com.example.foecho.DOWNLOAD_UPDATE"
private const val NOTIFICATION_ID = 1001

data class ReleaseInfo(val version: String, val url: String, val apkUrl: String?)

object UpdateStatus {
    var checked = false
    var latestVersion by mutableStateOf<String?>(null)
    var releaseUrl by mutableStateOf<String?>(null)
}

suspend fun checkForUpdate(): ReleaseInfo? = withContext(Dispatchers.IO) {
    runCatching {
        val conn = (URL(RELEASES_URL).openConnection() as HttpURLConnection).apply {
            connectTimeout = 5000
            readTimeout = 5000
            setRequestProperty("Accept", "application/vnd.github+json")
        }
        try {
            if (conn.responseCode != 200) return@runCatching null
            val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
            val apk = json.optJSONArray("assets")?.let { assets ->
                (0 until assets.length())
                    .map { assets.getJSONObject(it) }
                    .firstOrNull { it.optString("name").endsWith(".apk", ignoreCase = true) }
                    ?.optString("browser_download_url")
                    ?.takeIf { it.isNotBlank() }
            }
            ReleaseInfo(json.getString("tag_name"), json.getString("html_url"), apk)
        } finally {
            conn.disconnect()
        }
    }.getOrNull()
}

fun installedVersion(context: Context): String =
    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "0.0.0"

fun isNewer(latest: String, current: String): Boolean {
    fun parts(v: String) = v.trim().removePrefix("v").split(".").map { it.toIntOrNull() ?: 0 }
    val a = parts(latest)
    val b = parts(current)
    for (i in 0 until maxOf(a.size, b.size)) {
        val x = a.getOrElse(i) { 0 }
        val y = b.getOrElse(i) { 0 }
        if (x != y) return x > y
    }
    return false
}

fun rememberRelease(context: Context, release: ReleaseInfo) {
    context.getSharedPreferences(UPDATE_PREFS, Context.MODE_PRIVATE).edit()
        .putString(KEY_PENDING_VERSION, release.version)
        .putString(KEY_PENDING_URL, release.url)
        .putString(KEY_PENDING_APK, release.apkUrl)
        .apply()
}

fun pendingRelease(context: Context): ReleaseInfo? {
    val prefs = context.getSharedPreferences(UPDATE_PREFS, Context.MODE_PRIVATE)
    val version = prefs.getString(KEY_PENDING_VERSION, null) ?: return null
    val url = prefs.getString(KEY_PENDING_URL, null) ?: return null
    return ReleaseInfo(version, url, prefs.getString(KEY_PENDING_APK, null))
}

internal fun ensureChannel(context: Context) {
    if (Build.VERSION.SDK_INT >= 26) {
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "App updates", NotificationManager.IMPORTANCE_DEFAULT)
        )
    }
}

fun notifyUpdate(context: Context, release: ReleaseInfo) {
    val prefs = context.getSharedPreferences(UPDATE_PREFS, Context.MODE_PRIVATE)
    if (prefs.getString(KEY_NOTIFIED, null) == release.version) return
    if (Build.VERSION.SDK_INT >= 33 &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED
    ) return

    ensureChannel(context)
    val manager = context.getSystemService(NotificationManager::class.java)
    val downloadIntent = Intent(context, UpdateActionReceiver::class.java)
        .setAction(ACTION_DOWNLOAD_UPDATE)
    val downloadPending = PendingIntent.getBroadcast(
        context, 0, downloadIntent, PendingIntent.FLAG_IMMUTABLE
    )
    val notesPending = PendingIntent.getActivity(
        context,
        1,
        Intent(Intent.ACTION_VIEW, Uri.parse(release.url)),
        PendingIntent.FLAG_IMMUTABLE
    )
    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.stat_sys_download_done)
        .setContentTitle("FoEcho ${release.version} is available")
        .setContentText("Tap Download to get the update.")
        .setContentIntent(downloadPending)
        .addAction(0, "Download", downloadPending)
        .addAction(0, "View ChangeNotes", notesPending)
        .setAutoCancel(true)
        .build()
    manager.notify(NOTIFICATION_ID, notification)
    prefs.edit().putString(KEY_NOTIFIED, release.version).apply()
}

object NotificationPermission {
    const val REQUEST_CODE = 4201
    private var onResult: ((Boolean) -> Unit)? = null

    fun request(activity: Activity, callback: (Boolean) -> Unit) {
        onResult = callback
        ActivityCompat.requestPermissions(
            activity,
            arrayOf(Manifest.permission.POST_NOTIFICATIONS),
            REQUEST_CODE
        )
    }

    fun handleResult(requestCode: Int, grantResults: IntArray) {
        if (requestCode != REQUEST_CODE) return
        onResult?.invoke(grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED)
        onResult = null
    }
}