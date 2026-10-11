package com.lorenzo.caltracker.data.repository

import com.lorenzo.caltracker.data.db.dao.WeightDao
import com.lorenzo.caltracker.data.db.entity.WeightEntry
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

class WeightRepository(private val dao: WeightDao, private val settingsRepository: SettingsRepository) {
    fun observeAll(): Flow<List<WeightEntry>> = dao.observeAll()

    suspend fun getAll(): List<WeightEntry> = dao.getAll()

    /**
     * Records the weight for [date], replacing any weigh-in that day. If it's the most recent
     * weigh-in, it also becomes the profile weight (and recalculates calculated macros).
     */
    suspend fun log(date: LocalDate, weightKg: Double) {
        val existing = dao.getByDate(date)
        dao.upsert(existing?.copy(weightKg = weightKg) ?: WeightEntry(date = date, weightKg = weightKg))
        syncProfile()
    }

    suspend fun delete(entry: WeightEntry) {
        dao.delete(entry)
        syncProfile()
    }

    suspend fun restore(entry: WeightEntry) {
        dao.upsert(entry)
        syncProfile()
    }

    private suspend fun syncProfile() {
        dao.latest()?.let { settingsRepository.updateWeight(it.weightKg) }
    }
}
