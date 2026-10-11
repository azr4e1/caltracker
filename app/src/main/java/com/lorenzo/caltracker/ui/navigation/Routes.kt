package com.lorenzo.caltracker.ui.navigation

import kotlinx.serialization.Serializable

// Screens and their arguments. Dates are passed as epoch days (LocalDate.toEpochDay()).

@Serializable
data object DiaryRoute

@Serializable
data object HistoryRoute

@Serializable
data object SettingsRoute

@Serializable
data class AddFoodRoute(val epochDay: Long)

@Serializable
data class LogFoodRoute(val foodId: Long, val epochDay: Long)

@Serializable
data class QuickAddRoute(val epochDay: Long)

/**
 * Creates a food, then opens it for logging on [epochDay].
 * [draftJson] optionally pre-fills the form with a JSON-encoded RemoteFood (scanned or searched).
 */
@Serializable
data class NewFoodRoute(val epochDay: Long, val draftJson: String? = null)

@Serializable
data class EditFoodRoute(val foodId: Long)

enum class EditorSection { PROFILE, GOALS, BOTH }

/**
 * Profile and goals editor showing [section]. [isSetup] shows both as the first-launch welcome screen.
 */
@Serializable
data class ProfileEditorRoute(val section: EditorSection = EditorSection.BOTH, val isSetup: Boolean = false)
