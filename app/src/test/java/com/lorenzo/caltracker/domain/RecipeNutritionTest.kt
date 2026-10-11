package com.lorenzo.caltracker.domain

import com.lorenzo.caltracker.data.db.entity.Food
import org.junit.Assert.assertEquals
import org.junit.Test

class RecipeNutritionTest {
    private val pasta = Food(name = "Pasta", kcalPer100 = 360.0, proteinPer100 = 13.0, carbsPer100 = 71.0, fatPer100 = 2.0)
    private val sauce = Food(name = "Sauce", kcalPer100 = 50.0, proteinPer100 = 2.0, carbsPer100 = 8.0, fatPer100 = 1.0)

    @Test
    fun totalsAndPortions() {
        // 400 g pasta + 600 g sauce, 4 portions
        val n = RecipeNutrition.of(listOf(pasta to 400.0, sauce to 600.0), servings = 4.0, cookedWeight = null)
        assertEquals(1440.0 + 300.0, n.total.kcal, 0.001)
        assertEquals(1000.0, n.totalWeight, 0.001)
        assertEquals(435.0, n.perServing.kcal, 0.001)
        assertEquals(250.0, n.gramsPerServing, 0.001)
        assertEquals(870.0, n.forServings(2.0).kcal, 0.001)
    }

    @Test
    fun cookedWeightIsUsedForLoggingByGrams() {
        // Pasta absorbs water: 400 g dry becomes 1000 g cooked.
        val n = RecipeNutrition.of(listOf(pasta to 400.0), servings = 4.0, cookedWeight = 1000.0)
        assertEquals(1440.0, n.total.kcal, 0.001)
        assertEquals(144.0, n.forWeight(100.0).kcal, 0.001)
        assertEquals(250.0, n.gramsPerServing, 0.001)
    }

    @Test
    fun emptyRecipeIsZero() {
        val n = RecipeNutrition.of(emptyList(), servings = 1.0, cookedWeight = null)
        assertEquals(0.0, n.forWeight(100.0).kcal, 0.0)
        assertEquals(0.0, n.perServing.kcal, 0.0)
    }
}
