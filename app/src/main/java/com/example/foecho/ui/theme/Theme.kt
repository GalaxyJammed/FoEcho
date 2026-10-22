package com.example.foecho.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import com.example.foecho.data.AppTheme

private val DarkScheme = darkColorScheme(
    primary = Color.White,
    onPrimary = Color.Black,
    background = Color.Black,
    onBackground = Color.White,
    surface = Color.Black,
    onSurface = Color.White,
    surfaceContainerHighest = Color(0xFF1C1C1C),
    outline = Color(0xFF3A3A3A)
)

private val LightScheme = lightColorScheme(
    primary = Color.Black,
    onPrimary = Color.White,
    background = Color.White,
    onBackground = Color.Black,
    surface = Color.White,
    onSurface = Color.Black,
    surfaceContainerHighest = Color(0xFFF2F2F2),
    outline = Color(0xFFBDBDBD)
)

private val PlumScheme = darkColorScheme(
    primary = Color(0xFFF2B8D9),
    onPrimary = Color(0xFF3A0B27),
    primaryContainer = Color(0xFF6B2A4B),
    onPrimaryContainer = Color(0xFFFBD9EC),
    secondary = Color(0xFFD9A6C4),
    onSecondary = Color(0xFF3A0B27),
    secondaryContainer = Color(0xFF7A3A5E),
    onSecondaryContainer = Color(0xFFFBD9EC),
    background = Color(0xFF4A1432),
    onBackground = Color.White,
    surface = Color(0xFF4A1432),
    onSurface = Color(0xFFF7E9F1),
    surfaceVariant = Color(0xFF5E2344),
    onSurfaceVariant = Color(0xFFE3C2D6),
    surfaceContainerHighest = Color(0xFF5E2344),
    outline = Color(0xFF9C6E8C)
)

@Composable
fun FoEchoTheme(theme: AppTheme, content: @Composable () -> Unit) {
    val dark = when (theme) {
        AppTheme.SYSTEM -> isSystemInDarkTheme()
        AppTheme.WHITE -> false
        AppTheme.DARK, AppTheme.PLUM -> true
    }
    val scheme = when {
        theme == AppTheme.PLUM -> PlumScheme
        dark -> DarkScheme
        else -> LightScheme
    }
    MaterialTheme(colorScheme = scheme) {
        CompositionLocalProvider(LocalContentColor provides scheme.onBackground) {
            content()
        }
    }
}