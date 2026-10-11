package com.lorenzo.caltracker.data.backup

import com.lorenzo.caltracker.data.db.entity.Food
import com.lorenzo.caltracker.data.db.entity.Goals
import com.lorenzo.caltracker.data.db.entity.LogEntry
import com.lorenzo.caltracker.data.db.entity.Profile
import com.lorenzo.caltracker.data.db.entity.Recipe
import com.lorenzo.caltracker.data.db.entity.RecipeIngredient
import com.lorenzo.caltracker.data.db.entity.WeightEntry
import kotlinx.serialization.Serializable

/** Everything in the database, as written to a backup JSON file. */
@Serializable
data class Backup(
    /** Database schema version the backup was taken from. */
    val schemaVersion: Int,
    val exportedAt: String,
    val foods: List<Food> = emptyList(),
    val recipes: List<Recipe> = emptyList(),
    val recipeIngredients: List<RecipeIngredient> = emptyList(),
    val logEntries: List<LogEntry> = emptyList(),
    val profile: Profile? = null,
    val goals: Goals? = null,
    val weightEntries: List<WeightEntry> = emptyList(),
)
