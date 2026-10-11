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
    @Query("SELECT * FROM log_entries WHERE date = :date ORDER BY time, id")
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

    /** Date of the first entry ever logged, or null if the diary is empty. */
    @Query("SELECT MIN(date) FROM log_entries")
    fun observeFirstDate(): Flow<LocalDate?>

    /** The most recent time this food was logged; used to pre-fill the amount. */
    @Query("SELECT * FROM log_entries WHERE foodId = :foodId ORDER BY createdAt DESC LIMIT 1")
    suspend fun lastForFood(foodId: Long): LogEntry?

    @Query("SELECT * FROM log_entries WHERE id IN (:ids) ORDER BY time, id")
    suspend fun getByIds(ids: List<Long>): List<LogEntry>

    /** Every entry, oldest first; used for the Excel export. */
    @Query("SELECT * FROM log_entries ORDER BY date, time, id")
    suspend fun getAll(): List<LogEntry>

    @Insert
    suspend fun insert(entry: LogEntry): Long

    @Insert
    suspend fun insertAll(entries: List<LogEntry>): List<Long>

    @Update
    suspend fun update(entry: LogEntry)

    @Update
    suspend fun updateAll(entries: List<LogEntry>)

    @Delete
    suspend fun deleteAll(entries: List<LogEntry>)
}
