package com.example.foecho.data

import android.app.Activity
import android.content.Intent
import android.net.Uri

object FilePicker {
    const val REQUEST_CODE = 4202
    private var onResult: ((List<Uri>) -> Unit)? = null

    fun open(activity: Activity, onPicked: (List<Uri>) -> Unit) {
        onResult = onPicked
        val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "audio/*"
            putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION)
        }
        activity.startActivityForResult(intent, REQUEST_CODE)
    }

    fun handleResult(requestCode: Int, resultCode: Int, data: Intent?) {
        if (requestCode != REQUEST_CODE) return
        val callback = onResult
        onResult = null
        if (callback == null) return
        if (resultCode != Activity.RESULT_OK || data == null) {
            callback(emptyList())
            return
        }
        val uris = mutableListOf<Uri>()
        data.clipData?.let { clip ->
            for (i in 0 until clip.itemCount) uris += clip.getItemAt(i).uri
        }
        if (uris.isEmpty()) data.data?.let { uris += it }
        callback(uris)
    }
}