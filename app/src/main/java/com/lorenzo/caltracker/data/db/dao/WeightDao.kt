package com.lorenzo.caltracker.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Upsert
import com.lorenzo.caltracker.data.db.entity.WeightEntry
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
interface WeightDao {
    @Query("SELECT * FROM weight_entries ORDER BY date")
    fun observeAll(): Flow<List<WeightEntry>>

    @Query("SELECT * FROM weight_entries ORDER BY date")
    suspend fun getAll(): List<WeightEntry>

    @Query("SELECT * FROM weight_entries WHERE date = :date")
    suspend fun getByDate(date: LocalDate): WeightEntry?

    @Query("SELECT * FROM weight_entries ORDER BY date DESC LIMIT 1")
    suspend fun latest(): WeightEntry?

    @Upsert
    suspend fun upsert(entry: WeightEntry)

    @Delete
    suspend fun delete(entry: WeightEntry)
}
