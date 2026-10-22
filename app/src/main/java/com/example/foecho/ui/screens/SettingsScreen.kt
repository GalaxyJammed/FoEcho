package com.example.foecho.ui.screens

import android.content.Context
import android.widget.Toast
import androidx.biometric.BiometricManager
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.example.foecho.data.AppTheme
import com.example.foecho.data.Appearance
import com.example.foecho.data.PIN_LENGTH
import com.example.foecho.data.checkPin
import com.example.foecho.data.clearPin
import com.example.foecho.data.loadSecurity
import com.example.foecho.data.savePin
import com.example.foecho.data.setFingerprintEnabled
import com.example.foecho.ui.components.ScreenHeader
import com.example.foecho.ui.components.SettingRow
import com.example.foecho.ui.components.ShieldCard

private val SettingsRed = Color(0xFFD32F2F)

@Composable
fun SettingsScreen(onBackClick: () -> Unit) {
    val context = LocalContext.current
    var security by remember { mutableStateOf(loadSecurity(context)) }
    var showSetPin by remember { mutableStateOf(false) }
    var showVerifyPin by remember { mutableStateOf(false) }
    val scrollState = rememberScrollState()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    if (showSetPin) {
        SetPinDialog(
            onDismiss = { showSetPin = false },
            onSaved = { pin ->
                savePin(context, pin)
                security = loadSecurity(context)
                showSetPin = false
                Toast.makeText(context, "PIN turned on", Toast.LENGTH_SHORT).show()
            }
        )
    }

    if (showVerifyPin) {
        VerifyPinDialog(
            onDismiss = { showVerifyPin = false },
            onVerified = {
                clearPin(context)
                security = loadSecurity(context)
                showVerifyPin = false
                Toast.makeText(context, "PIN turned off", Toast.LENGTH_SHORT).show()
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
            .padding(16.dp)
    ) {
        ScreenHeader(
            title = "Settings",
            navIcon = Icons.AutoMirrored.Filled.ArrowBack,
            navDescription = "Back",
            onNavClick = onBackClick
        )
        Spacer(Modifier.height(16.dp))

        Box(Modifier.weight(1f).padding(bottom = navBottom)) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(end = 14.dp, bottom = 16.dp)
            ) {
                ShieldCard {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            text = "Appearance",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        Text(
                            text = "Choose your preferred theme style",
                            style = MaterialTheme.typography.bodyMedium
                        )
                        Spacer(Modifier.height(8.dp))
                        AppTheme.entries.forEach { option ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { Appearance.setTheme(context, option) }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(selected = Appearance.theme == option, onClick = null)
                                Spacer(Modifier.width(12.dp))
                                Text(option.label, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                        HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        SettingRow(
                            icon = Icons.Filled.Star,
                            title = "Scatter Icons",
                            subtitle = "Add tiny decorative icons to cards",
                            checked = Appearance.scatter,
                            onCheckedChange = { Appearance.setScatter(context, it) }
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))

                ShieldCard {
                    Column(Modifier.padding(16.dp)) {
                        Text(
                            text = "Security",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        HorizontalDivider(Modifier.padding(vertical = 8.dp))
                        SettingRow(
                            icon = Icons.Filled.Fingerprint,
                            title = "Fingerprint",
                            subtitle = "Ask for your fingerprint when you open the app",
                            checked = security.fingerprint,
                            onCheckedChange = { on ->
                                if (on && !fingerprintAvailable(context)) {
                                    Toast.makeText(
                                        context,
                                        "No fingerprint is set up on this phone",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                } else {
                                    setFingerprintEnabled(context, on)
                                    security = loadSecurity(context)
                                }
                            }
                        )
                        SettingRow(
                            icon = Icons.Filled.Lock,
                            title = "PIN",
                            subtitle = "Ask for a $PIN_LENGTH-digit PIN when you open the app",
                            checked = security.pin,
                            onCheckedChange = { on ->
                                if (on) showSetPin = true else showVerifyPin = true
                            }
                        )
                    }
                }
            }

            SettingsScrollbar(
                state = scrollState,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .fillMaxHeight()
                    .padding(end = 2.dp)
            )
        }
    }
}

@Composable
private fun SettingsScrollbar(state: ScrollState, modifier: Modifier) {
    val thumbColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
    Canvas(modifier.width(5.dp)) {
        val max = state.maxValue
        if (max <= 0) return@Canvas
        val track = size.height
        val content = track + max
        val thumbHeight = (track * track / content).coerceAtLeast(40.dp.toPx())
        val thumbTop = (state.value.toFloat() / max) * (track - thumbHeight)
        drawRoundRect(
            color = thumbColor,
            topLeft = Offset(0f, thumbTop),
            size = Size(size.width, thumbHeight),
            cornerRadius = CornerRadius(size.width / 2f)
        )
    }
}

private fun fingerprintAvailable(context: Context): Boolean =
    BiometricManager.from(context)
        .canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG) ==
            BiometricManager.BIOMETRIC_SUCCESS

@Composable
private fun SetPinDialog(onDismiss: () -> Unit, onSaved: (String) -> Unit) {
    var first by remember { mutableStateOf("") }
    var second by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Set a PIN") },
        text = {
            Column {
                OutlinedTextField(
                    value = first,
                    onValueChange = { first = it.filter(Char::isDigit).take(PIN_LENGTH) },
                    label = { Text("PIN") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation()
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = second,
                    onValueChange = { second = it.filter(Char::isDigit).take(PIN_LENGTH) },
                    label = { Text("Confirm PIN") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation()
                )
                error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = SettingsRed)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = first.length == PIN_LENGTH && second.length == PIN_LENGTH,
                onClick = {
                    if (first != second) error = "PINs don't match" else onSaved(first)
                }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun VerifyPinDialog(onDismiss: () -> Unit, onVerified: () -> Unit) {
    val context = LocalContext.current
    var entry by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Turn off PIN") },
        text = {
            Column {
                Text("Enter your current PIN to turn it off.")
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = entry,
                    onValueChange = { entry = it.filter(Char::isDigit).take(PIN_LENGTH) },
                    label = { Text("PIN") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    visualTransformation = PasswordVisualTransformation()
                )
                error?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = SettingsRed)
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = entry.length == PIN_LENGTH,
                onClick = {
                    if (checkPin(context, entry)) onVerified() else error = "Wrong PIN"
                }
            ) {
                Text("Turn off", color = SettingsRed)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}