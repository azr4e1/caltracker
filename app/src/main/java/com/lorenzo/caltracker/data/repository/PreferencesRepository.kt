package com.lorenzo.caltracker.data.repository

import android.content.Context
import androidx.core.content.edit
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Small app preferences that don't belong in the database. Not included in backups or exports
 * (the USDA key is a credential).
 */
class PreferencesRepository(context: Context) {
    private val prefs = context.getSharedPreferences("preferences", Context.MODE_PRIVATE)

    private val _usdaApiKey = MutableStateFlow(prefs.getString(KEY_USDA, null))

    /** The user's own USDA API key, or null to use the shared one built into the app. */
    val usdaApiKey: StateFlow<String?> = _usdaApiKey.asStateFlow()

    private val _groupMinutes = MutableStateFlow(prefs.getInt(KEY_GROUP_MINUTES, DEFAULT_GROUP_MINUTES))

    /** Diary entries logged within this many minutes of a group's first entry are shown together. 0 = off. */
    val groupMinutes: StateFlow<Int> = _groupMinutes.asStateFlow()

    fun setGroupMinutes(minutes: Int) {
        prefs.edit { putInt(KEY_GROUP_MINUTES, minutes) }
        _groupMinutes.value = minutes
    }

    private val _recentLimit = MutableStateFlow(prefs.getInt(KEY_RECENT_LIMIT, DEFAULT_RECENT_LIMIT))

    /** How many foods the Recent section in Add food shows. */
    val recentLimit: StateFlow<Int> = _recentLimit.asStateFlow()

    fun setRecentLimit(count: Int) {
        prefs.edit { putInt(KEY_RECENT_LIMIT, count) }
        _recentLimit.value = count
    }

    fun setUsdaApiKey(key: String?) {
        val value = key?.trim()?.ifEmpty { null }
        prefs.edit { if (value == null) remove(KEY_USDA) else putString(KEY_USDA, value) }
        _usdaApiKey.value = value
    }

    companion object {
        private const val KEY_USDA = "usdaApiKey"
        private const val KEY_GROUP_MINUTES = "groupMinutes"
        private const val KEY_RECENT_LIMIT = "recentLimit"
        const val DEFAULT_GROUP_MINUTES = 10
        const val DEFAULT_RECENT_LIMIT = 10
        val RECENT_LIMIT_OPTIONS = listOf(5, 10, 15, 20, 30)
        val GROUP_MINUTE_OPTIONS = listOf(0, 5, 10, 15, 30, 60)
    }
}
