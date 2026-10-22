package com.example.foecho

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.fragment.app.FragmentActivity
import com.example.foecho.data.Appearance
import com.example.foecho.data.FilePicker
import com.example.foecho.data.NotificationPermission
import com.example.foecho.data.loadSecurity
import com.example.foecho.ui.screens.EditorScreen
import com.example.foecho.ui.screens.HomeScreen
import com.example.foecho.ui.screens.LocalEditRequest
import com.example.foecho.ui.screens.LockScreen
import com.example.foecho.ui.screens.SettingsScreen
import com.example.foecho.ui.theme.FoEchoTheme
import com.example.foecho.data.MediaDelete

enum class Screen { Home, Settings, Editor }

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Appearance.load(this)
        val freshLaunch = savedInstanceState == null
        setContent {
            var screen by rememberSaveable { mutableStateOf(Screen.Home) }
            var editUri by rememberSaveable { mutableStateOf("") }
            var editName by rememberSaveable { mutableStateOf("") }
            var editDate by rememberSaveable { mutableStateOf(0L) }
            var locked by rememberSaveable {
                mutableStateOf(freshLaunch && loadSecurity(this@MainActivity).enabled)
            }

            FoEchoTheme(theme = Appearance.theme) {
                val bg = MaterialTheme.colorScheme.background
                BackHandler(enabled = screen != Screen.Home && !locked) {
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
                    Box(Modifier.fillMaxSize().background(bg)) {
                        when (screen) {
                            Screen.Home -> HomeScreen(
                                onSettingsClick = { screen = Screen.Settings }
                            )
                            Screen.Settings -> SettingsScreen(
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
                if (locked) {
                    LockScreen(
                        settings = loadSecurity(this@MainActivity),
                        onUnlock = { locked = false }
                    )
                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        NotificationPermission.handleResult(requestCode, grantResults)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        FilePicker.handleResult(requestCode, resultCode, data)
        MediaDelete.handleResult(requestCode, resultCode)
    }
}