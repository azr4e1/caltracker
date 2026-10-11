package com.lorenzo.caltracker.domain

import com.lorenzo.caltracker.data.db.dao.RecipeWithIngredients
import com.lorenzo.caltracker.data.db.entity.Food

/** Nutrients of a whole recipe, and how to scale them to a portion or a weight. */
data class RecipeNutrition(
    val total: Nutrients,
    /** Weight of the finished dish in grams (cooked weight if known, otherwise the sum of ingredients). */
    val totalWeight: Double,
    val servings: Double,
) {
    val perServing: Nutrients get() = if (servings > 0) total * (1 / servings) else Nutrients()

    fun forServings(count: Double): Nutrients = perServing * count

    fun forWeight(grams: Double): Nutrients = if (totalWeight > 0) total * (grams / totalWeight) else Nutrients()

    val gramsPerServing: Double get() = if (servings > 0) totalWeight / servings else 0.0

    companion object {
        /** [ingredients] are (food, amount in the food's base unit). Millilitres count as grams for the weight. */
        fun of(ingredients: List<Pair<Food, Double>>, servings: Double, cookedWeight: Double?): RecipeNutrition =
            RecipeNutrition(
                total = ingredients.fold(Nutrients()) { acc, (food, amount) -> acc + food.nutrientsFor(amount) },
                totalWeight = cookedWeight ?: ingredients.sumOf { it.second },
                servings = servings,
            )
    }
}

val RecipeWithIngredients.nutrition: RecipeNutrition
    get() = RecipeNutrition.of(ingredients.map { it.food to it.ingredient.amount }, recipe.servings, recipe.totalWeight)
