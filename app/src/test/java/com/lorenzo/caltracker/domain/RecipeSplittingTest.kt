package com.lorenzo.caltracker.domain

import com.lorenzo.caltracker.data.db.dao.IngredientWithFood
import com.lorenzo.caltracker.data.db.dao.RecipeWithIngredients
import com.lorenzo.caltracker.data.db.entity.EntryUnit
import com.lorenzo.caltracker.data.db.entity.Food
import com.lorenzo.caltracker.data.db.entity.LogEntry
import com.lorenzo.caltracker.data.db.entity.Recipe
import com.lorenzo.caltracker.data.db.entity.RecipeIngredient
import java.time.LocalDate
import java.time.LocalTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RecipeSplittingTest {
    private val oats = Food(id = 1, name = "Oats", kcalPer100 = 380.0, proteinPer100 = 13.0, carbsPer100 = 60.0, fatPer100 = 7.0)
    private val milk = Food(id = 2, name = "Milk", kcalPer100 = 50.0, proteinPer100 = 3.4, carbsPer100 = 5.0, fatPer100 = 1.5)

    // 2 portions: 160 g oats + 400 g milk, cooked down to 500 g
    private val porridge = RecipeWithIngredients(
        Recipe(id = 7, name = "Porridge", servings = 2.0, totalWeight = 500.0),
        listOf(
            IngredientWithFood(RecipeIngredient(recipeId = 7, foodId = 1, amount = 160.0, position = 0), oats),
            IngredientWithFood(RecipeIngredient(recipeId = 7, foodId = 2, amount = 400.0, position = 1), milk),
        ),
    )

    private fun entry(amount: Double, unit: EntryUnit) = LogEntry(
        date = LocalDate.of(2026, 10, 11), time = LocalTime.of(8, 0), recipeId = 7, name = "Porridge",
        amount = amount, unit = unit, kcal = 0.0, protein = 0.0, carbs = 0.0, fat = 0.0,
    )

    @Test
    fun oneOfTwoPortionsSplitsIntoHalfOfEachIngredient() {
        val parts = splitRecipeEntry(entry(1.0, EntryUnit.SERVING), porridge)
        assertEquals(listOf("Oats", "Milk"), parts.map { it.name })
        assertEquals(80.0, parts[0].amount, 0.001)
        assertEquals(200.0, parts[1].amount, 0.001)
        assertEquals(304.0, parts[0].kcal, 0.001)
        assertTrue(parts.all { it.recipeId == null && it.unit == EntryUnit.G && it.time == LocalTime.of(8, 0) })
    }

    @Test
    fun gramsUseTheCookedWeight() {
        // 250 g of a 500 g dish = half of everything
        val parts = splitRecipeEntry(entry(250.0, EntryUnit.G), porridge)
        assertEquals(80.0, parts[0].amount, 0.001)
        assertEquals(200.0, parts[1].amount, 0.001)
    }

    @Test
    fun servingEntriesConvertToBaseAmount() {
        val bread = Food(name = "Bread", servingSize = 30.0, kcalPer100 = 250.0, proteinPer100 = 9.0, carbsPer100 = 49.0, fatPer100 = 3.0)
        val e = entry(2.0, EntryUnit.SERVING).copy(recipeId = null, foodId = 3)
        assertEquals(60.0, e.baseAmount(bread)!!, 0.001)
    }
}
