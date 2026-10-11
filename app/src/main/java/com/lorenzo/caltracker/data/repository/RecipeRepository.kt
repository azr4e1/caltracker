package com.lorenzo.caltracker.data.repository

import androidx.room.withTransaction
import com.lorenzo.caltracker.data.db.AppDatabase
import com.lorenzo.caltracker.data.db.dao.RecipeWithIngredients
import com.lorenzo.caltracker.data.db.entity.LogEntry
import com.lorenzo.caltracker.data.db.entity.Recipe
import com.lorenzo.caltracker.data.db.entity.RecipeIngredient
import kotlinx.coroutines.flow.Flow

class RecipeRepository(private val database: AppDatabase) {
    private val dao = database.recipeDao()

    fun observeAll(): Flow<List<RecipeWithIngredients>> = dao.observeAll()

    fun search(query: String): Flow<List<RecipeWithIngredients>> = dao.search(query.trim())

    fun observe(id: Long): Flow<RecipeWithIngredients?> = dao.observeById(id)

    suspend fun get(id: Long): RecipeWithIngredients? = dao.getById(id)

    suspend fun getAll(): List<RecipeWithIngredients> = dao.getAll()

    suspend fun lastEntry(recipeId: Long): LogEntry? = dao.lastEntry(recipeId)

    /**
     * Creates or updates [recipe] (id 0 = new) and replaces its ingredients, all or nothing.
     * [ingredients] are (foodId, amount in the food's base unit). Returns the recipe id.
     */
    suspend fun save(recipe: Recipe, ingredients: List<Pair<Long, Double>>): Long = database.withTransaction {
        val id = if (recipe.id == 0L) {
            dao.insert(recipe)
        } else {
            dao.update(recipe.copy(updatedAt = System.currentTimeMillis()))
            dao.deleteIngredients(recipe.id)
            recipe.id
        }
        dao.insertIngredients(
            ingredients.mapIndexed { i, (foodId, amount) -> RecipeIngredient(recipeId = id, foodId = foodId, amount = amount, position = i) },
        )
        id
    }

    /** Deletes the recipe and its ingredient list. Logged entries keep their values. */
    suspend fun delete(recipe: Recipe) = dao.delete(recipe)
}
