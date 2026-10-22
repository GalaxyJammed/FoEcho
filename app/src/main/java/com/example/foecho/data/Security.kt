package com.example.foecho.data

import android.content.Context
import java.security.MessageDigest
import java.security.SecureRandom

const val PIN_LENGTH = 4

private const val SEC_PREFS = "foecho_security"
private const val KEY_FINGERPRINT = "fingerprint"
private const val KEY_PIN_ON = "pin_on"
private const val KEY_PIN_HASH = "pin_hash"
private const val KEY_PIN_SALT = "pin_salt"

data class SecuritySettings(val fingerprint: Boolean, val pin: Boolean) {
    val enabled: Boolean get() = fingerprint || pin
}

private fun prefs(context: Context) =
    context.getSharedPreferences(SEC_PREFS, Context.MODE_PRIVATE)

fun loadSecurity(context: Context): SecuritySettings {
    val p = prefs(context)
    return SecuritySettings(
        fingerprint = p.getBoolean(KEY_FINGERPRINT, false),
        pin = p.getBoolean(KEY_PIN_ON, false)
    )
}

fun setFingerprintEnabled(context: Context, on: Boolean) {
    prefs(context).edit().putBoolean(KEY_FINGERPRINT, on).apply()
}

fun savePin(context: Context, pin: String) {
    val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
    val saltHex = salt.joinToString("") { "%02x".format(it) }
    prefs(context).edit()
        .putString(KEY_PIN_SALT, saltHex)
        .putString(KEY_PIN_HASH, hashPin(pin, saltHex))
        .putBoolean(KEY_PIN_ON, true)
        .apply()
}

fun checkPin(context: Context, pin: String): Boolean {
    val p = prefs(context)
    val salt = p.getString(KEY_PIN_SALT, null) ?: return false
    val stored = p.getString(KEY_PIN_HASH, null) ?: return false
    return hashPin(pin, salt) == stored
}

fun clearPin(context: Context) {
    prefs(context).edit()
        .remove(KEY_PIN_HASH)
        .remove(KEY_PIN_SALT)
        .putBoolean(KEY_PIN_ON, false)
        .apply()
}

private fun hashPin(pin: String, saltHex: String): String {
    val digest = MessageDigest.getInstance("SHA-256").digest((saltHex + pin).toByteArray())
    return digest.joinToString("") { "%02x".format(it) }
}

object AppLock {
    private var skipPending = false

    fun skipNextStop() {
        skipPending = true
    }

    fun consumeSkip(): Boolean {
        val was = skipPending
        skipPending = false
        return was
    }
}