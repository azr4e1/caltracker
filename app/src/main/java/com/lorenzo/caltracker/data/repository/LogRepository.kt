package com.lorenzo.caltracker.data.repository

import com.lorenzo.caltracker.data.db.dao.DailyTotal
import com.lorenzo.caltracker.data.db.dao.LogEntryDao
import com.lorenzo.caltracker.data.db.entity.LogEntry
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

class LogRepository(private val dao: LogEntryDao) {
    fun observeDay(date: LocalDate): Flow<List<LogEntry>> = dao.observeDay(date)

    fun observeDailyTotals(from: LocalDate, to: LocalDate): Flow<List<DailyTotal>> = dao.observeDailyTotals(from, to)

    fun observeFirstDate(): Flow<LocalDate?> = dao.observeFirstDate()

    suspend fun getAll(): List<LogEntry> = dao.getAll()

    suspend fun lastForFood(foodId: Long): LogEntry? = dao.lastForFood(foodId)

    suspend fun add(entry: LogEntry): Long = dao.insert(entry)

    suspend fun update(entry: LogEntry) = dao.update(entry)

    /** Re-inserts entries with their original ids, e.g. to undo a delete. */
    suspend fun restore(entries: List<LogEntry>) = dao.insertAll(entries)

    suspend fun delete(entries: List<LogEntry>) = dao.deleteAll(entries)

    /** Adds copies of [entries] on [date] (same time of day). */
    suspend fun copyTo(entries: List<LogEntry>, date: LocalDate) {
        val now = System.currentTimeMillis()
        dao.insertAll(entries.map { it.copy(id = 0, date = date, createdAt = now) })
    }

    suspend fun moveTo(entries: List<LogEntry>, date: LocalDate) = dao.updateAll(entries.map { it.copy(date = date) })
}
