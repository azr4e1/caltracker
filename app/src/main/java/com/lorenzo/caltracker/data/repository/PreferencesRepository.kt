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

    fun setUsdaApiKey(key: String?) {
        val value = key?.trim()?.ifEmpty { null }
        prefs.edit { if (value == null) remove(KEY_USDA) else putString(KEY_USDA, value) }
        _usdaApiKey.value = value
    }

    private companion object {
        const val KEY_USDA = "usdaApiKey"
    }
}
