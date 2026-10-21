package com.example.foecho.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class AddedAudio(
    val uri: String,
    val name: String,
    val durationMs: Long,
    val dateCreated: Long = 0L,
    val dateModified: Long = 0L,
    val sizeBytes: Long = 0L
)

data class AudioCategory(val name: String, val audios: List<AddedAudio>)

private const val PREFS_NAME = "foecho_categories"
private const val KEY_CATEGORIES = "categories"

fun loadCategories(context: Context): List<AudioCategory> {
    val raw = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .getString(KEY_CATEGORIES, null) ?: return emptyList()
    val categories = JSONArray(raw)
    return List(categories.length()) { i ->
        val category = categories.getJSONObject(i)
        val audios = category.getJSONArray("audios")
        AudioCategory(
            name = category.getString("name"),
            audios = List(audios.length()) { j ->
                val audio = audios.getJSONObject(j)
                AddedAudio(
                    uri = audio.getString("uri"),
                    name = audio.getString("name"),
                    durationMs = audio.getLong("durationMs"),
                    dateCreated = audio.optLong("dateCreated", 0L),
                    dateModified = audio.optLong("dateModified", 0L),
                    sizeBytes = audio.optLong("sizeBytes", 0L)
                )
            }
        )
    }
}

fun saveCategories(context: Context, categories: List<AudioCategory>) {
    val array = JSONArray()
    categories.forEach { category ->
        val audios = JSONArray()
        category.audios.forEach { audio ->
            audios.put(
                JSONObject()
                    .put("uri", audio.uri)
                    .put("name", audio.name)
                    .put("durationMs", audio.durationMs)
                    .put("dateCreated", audio.dateCreated)
                    .put("dateModified", audio.dateModified)
                    .put("sizeBytes", audio.sizeBytes)
            )
        }
        array.put(JSONObject().put("name", category.name).put("audios", audios))
    }
    context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        .edit()
        .putString(KEY_CATEGORIES, array.toString())
        .apply()
}