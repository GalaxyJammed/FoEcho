package com.example.foecho

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import com.example.foecho.ui.screens.EditorScreen
import com.example.foecho.ui.screens.HomeScreen
import com.example.foecho.ui.screens.LocalEditRequest
import com.example.foecho.ui.screens.SettingsScreen
import com.example.foecho.ui.theme.FoEchoTheme

enum class Screen { Home, Settings, Editor }

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val systemDark = isSystemInDarkTheme()
            var darkMode by rememberSaveable { mutableStateOf(systemDark) }
            var screen by rememberSaveable { mutableStateOf(Screen.Home) }
            var editUri by rememberSaveable { mutableStateOf("") }
            var editName by rememberSaveable { mutableStateOf("") }
            var editDate by rememberSaveable { mutableStateOf(0L) }
            FoEchoTheme(darkTheme = darkMode) {
                BackHandler(enabled = screen != Screen.Home) {
                    screen = Screen.Home
                }
                CompositionLocalProvider(
                    LocalEditRequest provides { uri, name, date ->
                        editUri = uri
                        editName = name
                        editDate = date
                        screen = Screen.Editor
                    }
                ) {
                    when (screen) {
                        Screen.Home -> HomeScreen(
                            onSettingsClick = { screen = Screen.Settings }
                        )
                        Screen.Settings -> SettingsScreen(
                            darkMode = darkMode,
                            onDarkModeChange = { darkMode = it },
                            onBackClick = { screen = Screen.Home }
                        )
                        Screen.Editor -> EditorScreen(
                            uri = editUri,
                            name = editName,
                            dateCreated = editDate,
                            onBack = { screen = Screen.Home }
                        )
                    }
                }
            }
        }
    }
}