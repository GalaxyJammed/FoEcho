package com.example.foecho.data

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

enum class AppTheme(val label: String) {
    SYSTEM("System"),
    WHITE("White"),
    DARK("Dark"),
    PLUM("Plum")
}

private const val APPEARANCE_PREFS = "foecho_appearance"
private const val KEY_THEME = "theme"
private const val KEY_SCATTER = "scatter"

object Appearance {
    var theme by mutableStateOf(AppTheme.SYSTEM)
        private set

    var scatter by mutableStateOf(false)
        private set

    private fun prefs(context: Context) =
        context.getSharedPreferences(APPEARANCE_PREFS, Context.MODE_PRIVATE)

    fun load(context: Context) {
        val p = prefs(context)
        theme = AppTheme.entries.firstOrNull { it.name == p.getString(KEY_THEME, null) }
            ?: AppTheme.SYSTEM
        scatter = p.getBoolean(KEY_SCATTER, false)
    }

    fun setTheme(context: Context, value: AppTheme) {
        theme = value
        prefs(context).edit().putString(KEY_THEME, value.name).apply()
    }

    fun setScatter(context: Context, on: Boolean) {
        scatter = on
        prefs(context).edit().putBoolean(KEY_SCATTER, on).apply()
    }
}