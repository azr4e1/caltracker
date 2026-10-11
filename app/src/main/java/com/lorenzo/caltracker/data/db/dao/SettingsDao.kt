package com.lorenzo.caltracker.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Upsert
import com.lorenzo.caltracker.data.db.entity.Goals
import com.lorenzo.caltracker.data.db.entity.Profile
import kotlinx.coroutines.flow.Flow

@Dao
interface SettingsDao {
    @Query("SELECT * FROM profile WHERE id = 1")
    fun observeProfile(): Flow<Profile?>

    @Upsert
    suspend fun saveProfile(profile: Profile)

    @Query("SELECT * FROM goals WHERE id = 1")
    fun observeGoals(): Flow<Goals?>

    @Upsert
    suspend fun saveGoals(goals: Goals)
}
