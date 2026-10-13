package com.lorenzo.caltracker.data.repository

import com.lorenzo.caltracker.data.db.dao.SettingsDao
import com.lorenzo.caltracker.data.db.entity.GoalMode
import com.lorenzo.caltracker.data.db.entity.Goals
import com.lorenzo.caltracker.data.db.entity.Profile
import com.lorenzo.caltracker.domain.GoalCalculator
import com.lorenzo.caltracker.domain.GoalHistory
import com.lorenzo.caltracker.domain.periodFor
import com.lorenzo.caltracker.domain.withTargetsOf
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first

class SettingsRepository(private val dao: SettingsDao) {
    val profile: Flow<Profile?> = dao.observeProfile()
    /** The current goals, as set in Settings. */
    val goals: Flow<Goals?> = dao.observeGoals()

    /** The goals in force on [date]: past days keep the targets they had then. */
    fun goalsOn(date: LocalDate): Flow<Goals?> = combine(goals, dao.observeGoalHistory()) { current, history ->
        current?.withTargetsOf(history.periodFor(date))
    }

    /** The current goals with each day's targets looked up from [history]. */
    val goalHistory: Flow<GoalHistory> = combine(goals, dao.observeGoalHistory()) { current, history -> GoalHistory(current, history) }

    suspend fun saveProfile(profile: Profile) = dao.saveProfile(profile)

    suspend fun saveGoals(goals: Goals) = dao.saveGoals(goals, LocalDate.now())

    /**
     * Sets the profile weight. Goals calculated from the profile (protein is per kg) are
     * recalculated with the same calories; manual goals are left alone.
     */
    suspend fun updateWeight(weightKg: Double) {
        val profile = dao.observeProfile().first() ?: return
        dao.saveProfile(profile.copy(weightKg = weightKg))
        val goals = dao.observeGoals().first() ?: return
        if (goals.mode == GoalMode.AUTO) {
            val macros = GoalCalculator.autoMacros(goals.kcal, weightKg, goals.proteinPerKg, goals.fatShare)
            dao.saveGoals(goals.copy(protein = macros.protein, carbs = macros.carbs, fat = macros.fat), LocalDate.now())
        }
    }
}
