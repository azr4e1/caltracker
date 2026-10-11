package com.lorenzo.caltracker.ui.navigation

import kotlinx.serialization.Serializable

// Screens and their arguments. Dates are passed as epoch days (LocalDate.toEpochDay()).

@Serializable
data object DiaryRoute

@Serializable
data object HistoryRoute

@Serializable
data object SettingsRoute

/** Food picker. With [forRecipe] the chosen food is returned to the recipe editor instead of being logged. */
@Serializable
data class AddFoodRoute(val epochDay: Long, val forRecipe: Boolean = false)

@Serializable
data class LogFoodRoute(val foodId: Long, val epochDay: Long)

@Serializable
data class QuickAddRoute(val epochDay: Long)

/**
 * Creates a food, then opens it for logging on [epochDay].
 * [draftJson] optionally pre-fills the form with a JSON-encoded RemoteFood (scanned or searched).
 */
@Serializable
data class NewFoodRoute(val epochDay: Long, val draftJson: String? = null, val forRecipe: Boolean = false)

@Serializable
data class EditFoodRoute(val foodId: Long)

enum class EditorSection { PROFILE, GOALS, BOTH }

/**
 * Profile and goals editor showing [section]. [isSetup] shows both as the first-launch welcome screen.
 */
@Serializable
data class ProfileEditorRoute(val section: EditorSection = EditorSection.BOTH, val isSetup: Boolean = false)

/**
 * Creates a recipe ([recipeId] 0) or edits one. [fromEntryIds] (comma-separated diary entry ids)
 * pre-fills a new recipe with those entries' foods. With [logAfterSave], a new recipe is then
 * opened for logging on [epochDay].
 */
@Serializable
data class RecipeEditorRoute(
    val recipeId: Long = 0,
    val epochDay: Long,
    val fromEntryIds: String? = null,
    val logAfterSave: Boolean = true,
)

@Serializable
data class LogRecipeRoute(val recipeId: Long, val epochDay: Long)
