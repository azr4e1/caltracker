package com.lorenzo.caltracker.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Embedded
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import com.lorenzo.caltracker.data.db.entity.Food
import com.lorenzo.caltracker.data.db.entity.LogEntry
import com.lorenzo.caltracker.data.db.entity.Recipe
import com.lorenzo.caltracker.data.db.entity.RecipeIngredient
import kotlinx.coroutines.flow.Flow

data class IngredientWithFood(
    @Embedded val ingredient: RecipeIngredient,
    @Relation(parentColumn = "foodId", entityColumn = "id")
    val food: Food,
)

data class RecipeWithIngredients(
    @Embedded val recipe: Recipe,
    @Relation(entity = RecipeIngredient::class, parentColumn = "id", entityColumn = "recipeId")
    val ingredients: List<IngredientWithFood>,
)

@Dao
interface RecipeDao {
    @Transaction
    @Query("SELECT * FROM recipes ORDER BY name COLLATE NOCASE")
    fun observeAll(): Flow<List<RecipeWithIngredients>>

    @Transaction
    @Query("SELECT * FROM recipes WHERE name LIKE '%' || :query || '%' ORDER BY name COLLATE NOCASE")
    fun search(query: String): Flow<List<RecipeWithIngredients>>

    @Transaction
    @Query("SELECT * FROM recipes WHERE id = :id")
    suspend fun getById(id: Long): RecipeWithIngredients?

    @Transaction
    @Query("SELECT * FROM recipes WHERE id = :id")
    fun observeById(id: Long): Flow<RecipeWithIngredients?>

    @Transaction
    @Query("SELECT * FROM recipes ORDER BY name COLLATE NOCASE")
    suspend fun getAll(): List<RecipeWithIngredients>

    @Insert
    suspend fun insert(recipe: Recipe): Long

    @Update
    suspend fun update(recipe: Recipe)

    @Delete
    suspend fun delete(recipe: Recipe)

    @Insert
    suspend fun insertIngredients(ingredients: List<RecipeIngredient>)

    @Query("SELECT * FROM log_entries WHERE recipeId = :recipeId ORDER BY createdAt DESC LIMIT 1")
    suspend fun lastEntry(recipeId: Long): LogEntry?

    @Query("DELETE FROM recipe_ingredients WHERE recipeId = :recipeId")
    suspend fun deleteIngredients(recipeId: Long)
}
