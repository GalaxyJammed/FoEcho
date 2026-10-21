package com.example.foecho.data

import android.content.Context

enum class SortOption {
    DateCreatedNewest,
    DateCreatedOldest,
    DateEditedNewest,
    DateEditedOldest,
    NameAZ,
    NameZA,
    LengthLongest,
    LengthShortest
}

private const val PREFS_NAME = "foecho_sort"
private const val KEY_SORT = "option"

fun loadSort(context: Context): SortOption {
    val saved = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getString(KEY_SORT, null)
    return SortOption.entries.firstOrNull { it.name == saved } ?: SortOption.DateCreatedNewest
}

fun saveSort(context: Context, option: SortOption) {
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_SORT, option.name)
        .apply()
}