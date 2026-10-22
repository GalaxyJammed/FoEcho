package com.example.foecho

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.fragment.app.FragmentActivity
import com.example.foecho.data.Appearance
import com.example.foecho.data.FilePicker
import com.example.foecho.data.NotificationPermission
import com.example.foecho.data.loadSecurity
import com.example.foecho.ui.screens.EditorScreen
import com.example.foecho.ui.screens.HomeScreen
import com.example.foecho.ui.screens.LocalEditRequest
import com.example.foecho.ui.screens.LocalScreenActive
import com.example.foecho.ui.screens.LockScreen
import com.example.foecho.ui.screens.SettingsScreen
import com.example.foecho.ui.theme.FoEchoTheme
import kotlin.math.roundToInt

enum class Screen { Home, Settings, Editor }

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Appearance.load(this)
        val freshLaunch = savedInstanceState == null
        setContent {
            var screen by rememberSaveable { mutableStateOf(Screen.Home) }
            var overlay by rememberSaveable { mutableStateOf(Screen.Home) }
            var editUri by rememberSaveable { mutableStateOf("") }
            var editName by rememberSaveable { mutableStateOf("") }
            var editDate by rememberSaveable { mutableStateOf(0L) }
            var locked by rememberSaveable {
                mutableStateOf(freshLaunch && loadSecurity(this@MainActivity).enabled)
            }
            val slide = remember { Animatable(0f) }
            var widthPx by remember { mutableIntStateOf(0) }

            LaunchedEffect(screen) {
                if (screen == Screen.Home) {
                    if (overlay != Screen.Home) {
                        slide.animateTo(-1f, tween(260, easing = FastOutSlowInEasing))
                        overlay = Screen.Home
                        slide.snapTo(0f)
                    }
                } else {
                    overlay = screen
                    slide.snapTo(-1f)
                    slide.animateTo(0f, tween(260, easing = FastOutSlowInEasing))
                }
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
                        CompositionLocalProvider(LocalScreenActive provides (screen == Screen.Home)) {
                            HomeScreen(onSettingsClick = { screen = Screen.Settings })
                        }

                        if (overlay != Screen.Home) {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .onSizeChanged { widthPx = it.width }
                                    .offset { IntOffset((slide.value * widthPx).roundToInt(), 0) }
                                    .background(bg)
                            ) {
                                when (overlay) {
                                    Screen.Settings -> SettingsScreen(
                                        onBackClick = { screen = Screen.Home }
                                    )
                                    Screen.Editor -> key(editUri) {
                                        EditorScreen(
                                            uri = editUri,
                                            name = editName,
                                            dateCreated = editDate,
                                            onBack = { screen = Screen.Home }
                                        )
                                    }
                                    else -> Unit
                                }
                            }
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
    }
}