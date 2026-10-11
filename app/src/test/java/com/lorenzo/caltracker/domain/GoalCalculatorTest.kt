package com.lorenzo.caltracker.domain

import com.lorenzo.caltracker.data.db.entity.ActivityLevel
import com.lorenzo.caltracker.data.db.entity.Food
import com.lorenzo.caltracker.data.db.entity.Sex
import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class GoalCalculatorTest {
    @Test
    fun bmrMatchesMifflinStJeor() {
        // 10*80 + 6.25*180 - 5*30 + 5 = 1780
        assertEquals(1780.0, GoalCalculator.bmr(Sex.MALE, 30, 180.0, 80.0), 0.001)
        // 10*60 + 6.25*165 - 5*25 - 161 = 1345.25
        assertEquals(1345.25, GoalCalculator.bmr(Sex.FEMALE, 25, 165.0, 60.0), 0.001)
    }

    @Test
    fun tdeeAppliesActivityMultiplier() {
        assertEquals(1780.0 * 1.55, GoalCalculator.tdee(Sex.MALE, 30, 180.0, 80.0, ActivityLevel.MODERATE), 0.001)
    }

    @Test
    fun ageCountsCompletedYears() {
        assertEquals(29, GoalCalculator.age(LocalDate.of(1996, 10, 12), today = LocalDate.of(2026, 10, 11)))
        assertEquals(30, GoalCalculator.age(LocalDate.of(1996, 10, 11), today = LocalDate.of(2026, 10, 11)))
    }

    @Test
    fun autoMacrosFillCarbsWithRemainingCalories() {
        val macros = GoalCalculator.autoMacros(kcal = 2400.0, weightKg = 80.0, proteinPerKg = 2.0, fatShare = 0.25)
        assertEquals(160.0, macros.protein, 0.001)
        assertEquals(2400.0 * 0.25 / 9, macros.fat, 0.001)
        // 2400 - 160*4 - 600 = 1160 kcal left -> 290 g carbs
        assertEquals(290.0, macros.carbs, 0.001)
    }

    @Test
    fun autoMacrosNeverGoNegative() {
        val macros = GoalCalculator.autoMacros(kcal = 800.0, weightKg = 120.0, proteinPerKg = 2.2, fatShare = 0.3)
        assertEquals(0.0, macros.carbs, 0.0)
    }

    @Test
    fun foodNutrientsScaleFromPer100() {
        val oats = Food(name = "Oats", kcalPer100 = 380.0, proteinPer100 = 13.0, carbsPer100 = 60.0, fatPer100 = 7.0)
        val n = oats.nutrientsFor(50.0)
        assertEquals(190.0, n.kcal, 0.001)
        assertEquals(6.5, n.protein, 0.001)
        assertEquals(30.0, n.carbs, 0.001)
        assertEquals(3.5, n.fat, 0.001)
    }
}
