package com.lorenzo.caltracker.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.lorenzo.caltracker.data.backup.Backup
import com.lorenzo.caltracker.data.db.entity.Food
import com.lorenzo.caltracker.data.db.entity.Goals
import com.lorenzo.caltracker.data.db.entity.LogEntry
import com.lorenzo.caltracker.data.db.entity.Profile
import com.lorenzo.caltracker.data.db.entity.Recipe
import com.lorenzo.caltracker.data.db.entity.RecipeIngredient

/** Bulk read and replace of every table, for backup and restore. */
@Dao
abstract class BackupDao {
    @Query("SELECT * FROM foods") abstract suspend fun foods(): List<Food>
    @Query("SELECT * FROM recipes") abstract suspend fun recipes(): List<Recipe>
    @Query("SELECT * FROM recipe_ingredients") abstract suspend fun recipeIngredients(): List<RecipeIngredient>
    @Query("SELECT * FROM log_entries") abstract suspend fun logEntries(): List<LogEntry>
    @Query("SELECT * FROM profile WHERE id = 1") abstract suspend fun profile(): Profile?
    @Query("SELECT * FROM goals WHERE id = 1") abstract suspend fun goals(): Goals?

    @Query("DELETE FROM log_entries") protected abstract suspend fun clearLogEntries()
    @Query("DELETE FROM recipe_ingredients") protected abstract suspend fun clearRecipeIngredients()
    @Query("DELETE FROM recipes") protected abstract suspend fun clearRecipes()
    @Query("DELETE FROM foods") protected abstract suspend fun clearFoods()
    @Query("DELETE FROM profile") protected abstract suspend fun clearProfile()
    @Query("DELETE FROM goals") protected abstract suspend fun clearGoals()

    @Insert protected abstract suspend fun insertFoods(items: List<Food>)
    @Insert protected abstract suspend fun insertRecipes(items: List<Recipe>)
    @Insert protected abstract suspend fun insertRecipeIngredients(items: List<RecipeIngredient>)
    @Insert protected abstract suspend fun insertLogEntries(items: List<LogEntry>)
    @Insert protected abstract suspend fun insertProfile(profile: Profile)
    @Insert protected abstract suspend fun insertGoals(goals: Goals)

    /** Deletes everything and inserts the backup's rows, keeping their ids. All or nothing. */
    @Transaction
    open suspend fun replaceAll(backup: Backup) {
        clearLogEntries()
        clearRecipeIngredients()
        clearRecipes()
        clearFoods()
        clearProfile()
        clearGoals()

        insertFoods(backup.foods)
        insertRecipes(backup.recipes)
        insertRecipeIngredients(backup.recipeIngredients)
        insertLogEntries(backup.logEntries)
        backup.profile?.let { insertProfile(it) }
        backup.goals?.let { insertGoals(it) }
    }
}
