package com.lorenzo.caltracker.domain

import com.lorenzo.caltracker.data.db.entity.GoalPeriod
import com.lorenzo.caltracker.data.db.entity.Goals
import java.time.LocalDate

/**
 * The period in force on [date]: the latest one starting on or before it. Days before the first
 * period use the first one. Expects the list sorted by date; null if it's empty.
 */
fun List<GoalPeriod>.periodFor(date: LocalDate): GoalPeriod? = lastOrNull { !it.date.isAfter(date) } ?: firstOrNull()

/** These goals with the targets of [period], or unchanged if there's none. */
fun Goals.withTargetsOf(period: GoalPeriod?): Goals =
    period?.let { copy(kcal = it.kcal, protein = it.protein, carbs = it.carbs, fat = it.fat) } ?: this

/** Looks up the goals of any day. [current] is null before the goals are first set. */
data class GoalHistory(val current: Goals?, val periods: List<GoalPeriod>) {
    fun on(date: LocalDate): Goals? = current?.withTargetsOf(periods.periodFor(date))
}
