package com.lorenzo.caltracker.domain

import com.lorenzo.caltracker.data.db.dao.RecipeWithIngredients
import com.lorenzo.caltracker.data.db.entity.BaseUnit
import com.lorenzo.caltracker.data.db.entity.EntryUnit
import com.lorenzo.caltracker.data.db.entity.Food
import com.lorenzo.caltracker.data.db.entity.LogEntry

val Food.baseEntryUnit: EntryUnit get() = if (baseUnit == BaseUnit.ML) EntryUnit.ML else EntryUnit.G

/** Share of the whole recipe that a logged [entry] represents, or null if it can't be worked out. */
fun RecipeWithIngredients.fractionOf(entry: LogEntry): Double? = when (entry.unit) {
    EntryUnit.SERVING -> recipe.servings.takeIf { it > 0 }?.let { entry.amount / it }
    EntryUnit.G, EntryUnit.ML -> nutrition.totalWeight.takeIf { it > 0 }?.let { entry.amount / it }
    EntryUnit.QUICK -> null
}

/** The ingredients of a recipe scaled by [fraction], as (food, amount in base unit), in recipe order. */
fun RecipeWithIngredients.scaledIngredients(fraction: Double): List<Pair<Food, Double>> =
    ingredients.sortedBy { it.ingredient.position }.map { it.food to it.ingredient.amount * fraction }

/**
 * Replaces a logged recipe with one entry per ingredient, at the same date and time, scaled to the
 * amount that was logged. Uses the recipe's current ingredients and food values.
 */
fun splitRecipeEntry(entry: LogEntry, recipe: RecipeWithIngredients): List<LogEntry> {
    val fraction = recipe.fractionOf(entry) ?: return emptyList()
    val now = System.currentTimeMillis()
    return recipe.scaledIngredients(fraction).map { (food, amount) ->
        val n = food.nutrientsFor(amount)
        LogEntry(
            date = entry.date,
            time = entry.time,
            foodId = food.id,
            name = food.name,
            amount = amount,
            unit = food.baseEntryUnit,
            kcal = n.kcal,
            protein = n.protein,
            carbs = n.carbs,
            fat = n.fat,
            createdAt = now,
        )
    }
}

/** Amount of a logged food entry in the food's base unit (g/ml), or null for quick adds and recipes. */
fun LogEntry.baseAmount(food: Food): Double? = when (unit) {
    EntryUnit.G, EntryUnit.ML -> amount
    EntryUnit.SERVING -> food.servingSize?.let { amount * it }
    EntryUnit.QUICK -> null
}
