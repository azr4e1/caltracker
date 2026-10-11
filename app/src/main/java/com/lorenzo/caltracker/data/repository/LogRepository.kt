package com.lorenzo.caltracker.data.repository

import com.lorenzo.caltracker.data.db.dao.LogEntryDao
import com.lorenzo.caltracker.data.db.entity.LogEntry
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

class LogRepository(private val dao: LogEntryDao) {
    fun observeDay(date: LocalDate): Flow<List<LogEntry>> = dao.observeDay(date)

    suspend fun lastForFood(foodId: Long): LogEntry? = dao.lastForFood(foodId)

    suspend fun add(entry: LogEntry): Long = dao.insert(entry)

    suspend fun update(entry: LogEntry) = dao.update(entry)

    suspend fun delete(entry: LogEntry) = dao.delete(entry)
}
