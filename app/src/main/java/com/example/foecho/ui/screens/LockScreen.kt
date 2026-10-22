package com.example.foecho.ui.screens

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.foecho.data.PIN_LENGTH
import com.example.foecho.data.SecuritySettings
import com.example.foecho.data.checkPin

private val LockRed = Color(0xFFD32F2F)

@Composable
fun LockScreen(settings: SecuritySettings, onUnlock: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    var pin by remember { mutableStateOf("") }
    var message by remember { mutableStateOf<String?>(null) }

    BackHandler {
        (context as? Activity)?.moveTaskToBack(true)
    }

    fun startFingerprint() {
        val act = activity ?: return
        showFingerprint(
            activity = act,
            canUsePin = settings.pin,
            onSuccess = onUnlock,
            onError = { message = it }
        )
    }

    LaunchedEffect(Unit) {
        if (settings.fingerprint) startFingerprint()
    }

    fun onDigit(digit: String) {
        if (pin.length >= PIN_LENGTH) return
        val updated = pin + digit
        pin = updated
        if (updated.length == PIN_LENGTH) {
            if (checkPin(context, updated)) {
                onUnlock()
            } else {
                message = "Wrong PIN"
                pin = ""
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "FoEcho is locked",
            style = MaterialTheme.typography.headlineMedium
        )
        Spacer(Modifier.height(24.dp))

        if (settings.pin) {
            Text("Enter your PIN", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                repeat(PIN_LENGTH) { i ->
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .background(
                                if (i < pin.length) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outline,
                                CircleShape
                            )
                    )
                }
            }
            Spacer(Modifier.height(24.dp))
            PinPad(
                onDigit = { onDigit(it) },
                onDelete = { if (pin.isNotEmpty()) pin = pin.dropLast(1) }
            )
            if (settings.fingerprint) {
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = {
                    message = null
                    startFingerprint()
                }) {
                    Text("Use fingerprint")
                }
            }
        } else {
            Text("Use your fingerprint to unlock", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(16.dp))
            Button(onClick = { startFingerprint() }) {
                Text("Unlock with fingerprint")
            }
        }

        message?.let {
            Spacer(Modifier.height(16.dp))
            Text(it, color = LockRed, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun PinPad(onDigit: (String) -> Unit, onDelete: () -> Unit) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("", "0", "⌫")
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                row.forEach { key ->
                    when (key) {
                        "" -> Spacer(Modifier.size(64.dp))
                        "⌫" -> TextButton(
                            onClick = onDelete,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Text("⌫", style = MaterialTheme.typography.titleLarge)
                        }
                        else -> OutlinedButton(
                            onClick = { onDigit(key) },
                            shape = CircleShape,
                            modifier = Modifier.size(64.dp)
                        ) {
                            Text(key, style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }
            }
        }
    }
}

private fun showFingerprint(
    activity: FragmentActivity,
    canUsePin: Boolean,
    onSuccess: () -> Unit,
    onError: (String) -> Unit
) {
    val callback = object : BiometricPrompt.AuthenticationCallback() {
        override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
            onSuccess()
        }

        override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
            when (errorCode) {
                BiometricPrompt.ERROR_NEGATIVE_BUTTON,
                BiometricPrompt.ERROR_USER_CANCELED,
                BiometricPrompt.ERROR_CANCELED -> Unit
                else -> onError(errString.toString())
            }
        }
    }
    val prompt = BiometricPrompt(activity, ContextCompat.getMainExecutor(activity), callback)
    val info = BiometricPrompt.PromptInfo.Builder()
        .setTitle("Unlock FoEcho")
        .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        .setNegativeButtonText(if (canUsePin) "Use PIN" else "Cancel")
        .build()
    prompt.authenticate(info)
}