package com.lorenzo.caltracker.data.remote

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.doubleOrNull

/** Open Food Facts asks every app to identify itself. */
private const val USER_AGENT = "CalTracker/0.3 (Android; personal use)"

/** Fetches [url] and parses the body as JSON. 404 bodies are returned too, since some APIs use them for "not found". */
internal suspend fun getJson(url: String): JsonElement = withContext(Dispatchers.IO) {
    val connection = URL(url).openConnection() as HttpURLConnection
    try {
        connection.connectTimeout = 10_000
        connection.readTimeout = 15_000
        connection.setRequestProperty("User-Agent", USER_AGENT)
        connection.setRequestProperty("Accept", "application/json")
        val code = connection.responseCode
        val stream = if (code in 200..299) connection.inputStream else connection.errorStream
        if (code !in 200..299 && code != 404) throw IOException("HTTP $code")
        val body = stream?.bufferedReader()?.use { it.readText() } ?: throw IOException("Empty response (HTTP $code)")
        Json.parseToJsonElement(body)
    } finally {
        connection.disconnect()
    }
}

internal fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")

// Lenient accessors: user-contributed data mixes numbers, numeric strings, lists and nulls.

internal fun JsonElement?.obj(): JsonObject? = this as? JsonObject

internal fun JsonElement?.arr(): JsonArray? = this as? JsonArray

internal fun JsonElement?.str(): String? =
    (this as? JsonPrimitive)?.takeUnless { it is JsonNull }?.content?.trim()?.takeIf { it.isNotEmpty() }

internal fun JsonElement?.num(): Double? {
    val p = this as? JsonPrimitive ?: return null
    return p.doubleOrNull ?: p.content.trim().replace(',', '.').toDoubleOrNull()
}
