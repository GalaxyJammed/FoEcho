package com.example.foecho.data

import android.app.Activity
import android.content.IntentSender

object MediaDelete {
    const val REQUEST_CODE = 4203
    private var onResult: ((Boolean) -> Unit)? = null

    fun launch(activity: Activity, sender: IntentSender, callback: (Boolean) -> Unit) {
        onResult = callback
        try {
            activity.startIntentSenderForResult(sender, REQUEST_CODE, null, 0, 0, 0)
        } catch (e: IntentSender.SendIntentException) {
            onResult = null
            callback(false)
        }
    }

    fun handleResult(requestCode: Int, resultCode: Int) {
        if (requestCode != REQUEST_CODE) return
        val callback = onResult
        onResult = null
        callback?.invoke(resultCode == Activity.RESULT_OK)
    }
}