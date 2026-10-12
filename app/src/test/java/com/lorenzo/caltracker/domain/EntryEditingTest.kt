package com.lorenzo.caltracker.domain

import com.lorenzo.caltracker.data.db.entity.EntryUnit
import com.lorenzo.caltracker.data.db.entity.LogEntry
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Test

class EntryEditingTest {
    private fun entry(time: String, kcal: Double = 100.0) = LogEntry(
        date = LocalDate.of(2026, 10, 12), time = LocalTime.parse(time), name = time,
        amount = 100.0, unit = EntryUnit.G, kcal = kcal, protein = 10.0, carbs = 20.0, fat = 5.0,
    )

    @Test
    fun changingTheAmountScalesNutrients() {
        val e = entry("08:00").withAmount(150.0)
        assertEquals(150.0, e.amount, 0.0)
        assertEquals(150.0, e.kcal, 1e-9)
        assertEquals(30.0, e.carbs, 1e-9)
    }

    @Test
    fun groupsByWindowFromTheFirstEntry() {
        val entries = listOf("08:00", "08:10", "08:15", "08:16", "13:00", "13:20").map { entry(it) }
        val groups = entries.groupByTime(15).map { g -> g.map { it.name } }
        assertEquals(listOf(listOf("08:00", "08:10", "08:15"), listOf("08:16"), listOf("13:00"), listOf("13:20")), groups)
    }

    @Test
    fun zeroWindowOnlyGroupsTheSameMinute() {
        val groups = listOf("08:00", "08:00", "08:01").map { entry(it) }.groupByTime(0)
        assertEquals(listOf(2, 1), groups.map { it.size })
    }

    @Test
    fun switchingToServingsKeepsNutrients() {
        val choices = UnitChoices(EntryUnit.G, 40.0, "slice")
        val e = entry("08:00").inUnit(EntryUnit.SERVING, choices)
        assertEquals(EntryUnit.SERVING, e.unit)
        assertEquals(2.5, e.amount, 1e-9)
        assertEquals(100.0, e.kcal, 1e-9)
        assertEquals(entry("08:00").amount, e.inUnit(EntryUnit.G, choices).amount, 1e-9)
    }

    @Test
    fun cannotSwitchWithoutAServingSize() {
        val e = entry("08:00").inUnit(EntryUnit.SERVING, UnitChoices(EntryUnit.G, null, "serving"))
        assertEquals(EntryUnit.G, e.unit)
        assertEquals(listOf(EntryUnit.G), UnitChoices(EntryUnit.G, null, "serving").units)
    }
}
