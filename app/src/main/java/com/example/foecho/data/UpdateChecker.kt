package com.example.foecho.data

import android.Manifest
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
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import android.app.Activity
import androidx.core.app.ActivityCompat

private const val RELEASES_URL = "https://api.github.com/repos/GalaxyJammed/FoEcho/releases/latest"
private const val PREFS = "foecho_updates"
private const val KEY_NOTIFIED = "notified_version"
private const val CHANNEL_ID = "foecho_updates"
private const val NOTIFICATION_ID = 1001

data class ReleaseInfo(val version: String, val url: String)

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
            ReleaseInfo(json.getString("tag_name"), json.getString("html_url"))
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

fun notifyUpdate(context: Context, release: ReleaseInfo) {
    val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
    if (prefs.getString(KEY_NOTIFIED, null) == release.version) return
    if (Build.VERSION.SDK_INT >= 33 &&
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) !=
        PackageManager.PERMISSION_GRANTED
    ) return

    val manager = context.getSystemService(NotificationManager::class.java)
    if (Build.VERSION.SDK_INT >= 26) {
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, "App updates", NotificationManager.IMPORTANCE_DEFAULT)
        )
    }
    val openIntent = PendingIntent.getActivity(
        context,
        0,
        Intent(Intent.ACTION_VIEW, Uri.parse(release.url)),
        PendingIntent.FLAG_IMMUTABLE
    )
    val notification = NotificationCompat.Builder(context, CHANNEL_ID)
        .setSmallIcon(android.R.drawable.stat_sys_download_done)
        .setContentTitle("FoEcho update available")
        .setContentText("Version ${release.version} is ready to download.")
        .setContentIntent(openIntent)
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