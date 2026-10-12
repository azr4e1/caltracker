package com.lorenzo.caltracker.domain

import com.lorenzo.caltracker.data.db.entity.LogEntry
import java.time.Duration
import java.time.LocalTime

/** The entry with a new amount; its nutrient snapshot scales with it. Unchanged if either amount isn't positive. */
fun LogEntry.withAmount(newAmount: Double): LogEntry {
    if (newAmount <= 0 || amount <= 0) return this
    val factor = newAmount / amount
    return copy(amount = newAmount, kcal = kcal * factor, protein = protein * factor, carbs = carbs * factor, fat = fat * factor)
}

/** A quick add with new name and values. */
fun LogEntry.withValues(newName: String, n: Nutrients): LogEntry =
    copy(name = newName, kcal = n.kcal, protein = n.protein, carbs = n.carbs, fat = n.fat)

/**
 * Splits entries (sorted by time) into groups: an entry joins the current group if it was logged
 * at most [windowMinutes] after the group's first entry. With 0, only entries at the same minute group.
 */
fun List<LogEntry>.groupByTime(windowMinutes: Int): List<List<LogEntry>> {
    val groups = mutableListOf<MutableList<LogEntry>>()
    for (entry in this) {
        val group = groups.lastOrNull()
        val start: LocalTime? = group?.first()?.time
        val minutes = start?.let { Duration.between(it, entry.time).toMinutes() }
        if (group != null && minutes != null && minutes in 0..windowMinutes.toLong()) group += entry else groups += mutableListOf(entry)
    }
    return groups
}
