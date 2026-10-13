package com.lorenzo.caltracker.data.db.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Upsert
import com.lorenzo.caltracker.data.db.entity.GoalPeriod
import com.lorenzo.caltracker.data.db.entity.Goals
import com.lorenzo.caltracker.data.db.entity.Profile
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

@Dao
abstract class SettingsDao {
    @Query("SELECT * FROM profile WHERE id = 1")
    abstract fun observeProfile(): Flow<Profile?>

    @Upsert
    abstract suspend fun saveProfile(profile: Profile)

    @Query("SELECT * FROM goals WHERE id = 1")
    abstract fun observeGoals(): Flow<Goals?>

    @Query("SELECT * FROM goal_history ORDER BY date")
    abstract fun observeGoalHistory(): Flow<List<GoalPeriod>>

    @Upsert
    protected abstract suspend fun upsertGoals(goals: Goals)

    @Upsert
    protected abstract suspend fun upsertGoalPeriod(period: GoalPeriod)

    /** Saves the goals and records their targets as in force from [today]. */
    @Transaction
    open suspend fun saveGoals(goals: Goals, today: LocalDate) {
        upsertGoals(goals)
        upsertGoalPeriod(GoalPeriod(today, goals.kcal, goals.protein, goals.carbs, goals.fat))
    }
}
