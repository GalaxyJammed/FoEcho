package com.example.foecho.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.foecho.ui.components.ScreenHeader
import com.example.foecho.ui.components.SettingRow
import com.example.foecho.ui.components.ShieldCard

@Composable
fun SettingsScreen(
    darkMode: Boolean,
    onDarkModeChange: (Boolean) -> Unit,
    onBackClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(16.dp)
    ) {
        ScreenHeader(
            title = "Settings",
            navIcon = Icons.AutoMirrored.Filled.ArrowBack,
            navDescription = "Back",
            onNavClick = onBackClick
        )
        Spacer(Modifier.height(16.dp))
        ShieldCard {
            Column(Modifier.padding(16.dp)) {
                Text(
                    text = "Appearance",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary
                )
                HorizontalDivider(Modifier.padding(vertical = 8.dp))
                SettingRow(
                    icon = Icons.Filled.DarkMode,
                    title = "Dark mode",
                    subtitle = "Switch between dark and light theme",
                    checked = darkMode,
                    onCheckedChange = onDarkModeChange
                )
            }
        }
    }
}