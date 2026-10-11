package com.lorenzo.caltracker.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.lorenzo.caltracker.data.db.entity.LogEntry
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

data class DailyTotal(
    val date: LocalDate,
    val kcal: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
)

@Dao
interface LogEntryDao {
    @Query("SELECT * FROM log_entries WHERE date = :date ORDER BY position, id")
    fun observeDay(date: LocalDate): Flow<List<LogEntry>>

    @Query(
        """
        SELECT date, SUM(kcal) AS kcal, SUM(protein) AS protein, SUM(carbs) AS carbs, SUM(fat) AS fat
        FROM log_entries
        WHERE date BETWEEN :from AND :to
        GROUP BY date
        ORDER BY date
        """,
    )
    fun observeDailyTotals(from: LocalDate, to: LocalDate): Flow<List<DailyTotal>>

    @Insert
    suspend fun insert(entry: LogEntry): Long

    @Insert
    suspend fun insertAll(entries: List<LogEntry>)

    @Update
    suspend fun update(entry: LogEntry)

    @Delete
    suspend fun delete(entry: LogEntry)
}
