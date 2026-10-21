package com.example.foecho.data

import android.content.Context

private const val PREFS_NAME = "foecho_favorites"
private const val KEY_FAVORITES = "keys"

fun loadFavorites(context: Context): Set<String> =
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getStringSet(KEY_FAVORITES, emptySet())
        ?.toSet()
        ?: emptySet()

fun saveFavorites(context: Context, keys: Set<String>) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putStringSet(KEY_FAVORITES, HashSet(keys))
        .apply()
}