package com.lorenzo.caltracker.domain

import com.lorenzo.caltracker.data.db.entity.GoalPeriod
import com.lorenzo.caltracker.data.db.entity.Goals
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GoalHistoryTest {
    private fun day(d: Int) = LocalDate.of(2026, 10, d)
    private val periods = listOf(
        GoalPeriod(day(10), kcal = 2600.0, protein = 186.0, carbs = 300.0, fat = 72.0),
        GoalPeriod(day(14), kcal = 2400.0, protein = 190.0, carbs = 250.0, fat = 67.0),
    )
    private val current = Goals(kcal = 2400.0, protein = 190.0, carbs = 250.0, fat = 67.0, proteinPerKg = 2.1)

    @Test
    fun eachDayUsesTheLatestPeriodStartingOnOrBeforeIt() {
        val history = GoalHistory(current, periods)
        assertEquals(2600.0, history.on(day(13))!!.kcal, 0.0)
        assertEquals(2400.0, history.on(day(14))!!.kcal, 0.0)
        assertEquals(2400.0, history.on(day(20))!!.kcal, 0.0)
        assertEquals(186.0, history.on(day(12))!!.protein, 0.0)
    }

    @Test
    fun daysBeforeTheFirstPeriodUseTheFirstPeriod() {
        assertEquals(2600.0, GoalHistory(current, periods).on(day(1))!!.kcal, 0.0)
    }

    @Test
    fun withoutHistoryTheCurrentGoalsApply() {
        assertEquals(current, GoalHistory(current, emptyList()).on(day(1)))
        assertNull(GoalHistory(null, periods).on(day(12)))
    }

    @Test
    fun settingsOtherThanTargetsComeFromTheCurrentGoals() {
        assertEquals(2.1, GoalHistory(current, periods).on(day(12))!!.proteinPerKg, 0.0)
    }
}
