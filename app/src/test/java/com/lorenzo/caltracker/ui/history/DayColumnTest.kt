package com.lorenzo.caltracker.ui.history

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class DayColumnTest {
    private val date = LocalDate.of(2026, 10, 12)

    @Test
    fun columnIsAsTallAsLoggedCaloriesWhenMacrosAddUpToMore() {
        // 155×4 + 286×4 + 112×9 = 2772 kcal of macros, but 2601 logged.
        val day = DayColumn(date, kcal = 2601.0, protein = 155.0, carbs = 286.0, fat = 112.0)
        assertEquals(2601.0, day.stackKcal, 1e-9)
        assertEquals(0.0, day.otherKcal, 0.0)
        assertEquals(620.0 / 2772.0, day.proteinKcal / day.stackKcal, 1e-9)
    }

    @Test
    fun caloriesNotExplainedByMacrosShowAsOther() {
        val day = DayColumn(date, kcal = 1756.0, protein = 71.0, carbs = 225.0, fat = 57.0)
        assertEquals(1756.0, day.stackKcal, 1e-9)
        assertEquals(59.0, day.otherKcal, 1e-9)
        assertEquals(284.0, day.proteinKcal, 1e-9)
    }
}
