package com.lorenzo.caltracker.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.lorenzo.caltracker.data.db.entity.Food
import kotlinx.coroutines.flow.Flow

@Dao
interface FoodDao {
    @Query("SELECT * FROM foods WHERE id = :id")
    suspend fun getById(id: Long): Food?

    @Query("SELECT * FROM foods WHERE barcode = :barcode")
    suspend fun getByBarcode(barcode: String): Food?

    @Query("SELECT * FROM foods WHERE name LIKE '%' || :query || '%' OR brand LIKE '%' || :query || '%' ORDER BY name")
    fun search(query: String): Flow<List<Food>>

    /** Foods ordered by when they were last logged, most recent first. */
    @Query(
        """
        SELECT foods.* FROM foods
        JOIN log_entries ON log_entries.foodId = foods.id
        GROUP BY foods.id
        ORDER BY MAX(log_entries.createdAt) DESC
        LIMIT :limit
        """,
    )
    fun observeRecent(limit: Int): Flow<List<Food>>

    @Insert
    suspend fun insert(food: Food): Long

    @Update
    suspend fun update(food: Food)

    @Delete
    suspend fun delete(food: Food)
}
