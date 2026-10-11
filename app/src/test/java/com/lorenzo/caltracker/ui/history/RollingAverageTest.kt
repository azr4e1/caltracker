package com.lorenzo.caltracker.ui.history

import org.junit.Assert.assertEquals
import org.junit.Test

class RollingAverageTest {
    @Test
    fun averagesTheLastSevenDays() {
        // Weigh-ins on days 0, 3 and 10
        val avg = rollingAverage(listOf(0 to 90.0, 3 to 88.0, 10 to 86.0)).toMap()
        assertEquals(90.0, avg.getValue(0), 1e-9)
        assertEquals(89.0, avg.getValue(3), 1e-9) // days 0 and 3
        assertEquals(89.0, avg.getValue(6), 1e-9) // window 0..6
        assertEquals(88.0, avg.getValue(7), 1e-9) // window 1..7: only day 3
        assertEquals(88.0, avg.getValue(9), 1e-9) // window 3..9: only day 3
        assertEquals(86.0, avg.getValue(10), 1e-9) // window 4..10: only day 10
    }

    @Test
    fun skipsDaysWithNoWeighInInTheWindow() {
        val avg = rollingAverage(listOf(0 to 90.0, 20 to 85.0)).toMap()
        assertEquals(false, 10 in avg)
        assertEquals(90.0, avg.getValue(6), 1e-9)
        assertEquals(85.0, avg.getValue(20), 1e-9)
    }
}
