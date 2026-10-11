package com.lorenzo.caltracker.data.repository

import com.lorenzo.caltracker.data.db.dao.SettingsDao
import com.lorenzo.caltracker.data.db.entity.Goals
import com.lorenzo.caltracker.data.db.entity.Profile
import kotlinx.coroutines.flow.Flow

class SettingsRepository(private val dao: SettingsDao) {
    val profile: Flow<Profile?> = dao.observeProfile()
    val goals: Flow<Goals?> = dao.observeGoals()

    suspend fun saveProfile(profile: Profile) = dao.saveProfile(profile)

    suspend fun saveGoals(goals: Goals) = dao.saveGoals(goals)
}
