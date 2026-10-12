package com.lorenzo.caltracker.data.repository

import com.lorenzo.caltracker.data.db.entity.EntryUnit
import com.lorenzo.caltracker.data.db.entity.LogEntry
import com.lorenzo.caltracker.domain.UnitChoices
import com.lorenzo.caltracker.domain.baseEntryUnit
import com.lorenzo.caltracker.domain.nutrition

/** Looks up which units a logged entry can be switched between, from its food or recipe. */
class UnitChoicesRepository(private val foodRepository: FoodRepository, private val recipeRepository: RecipeRepository) {
    /** Null for quick adds and for entries whose food or recipe was deleted. */
    suspend fun forEntry(entry: LogEntry): UnitChoices? {
        if (entry.isQuickAdd) return null
        entry.recipeId?.let { id ->
            val recipe = recipeRepository.get(id) ?: return null
            return UnitChoices(EntryUnit.G, recipe.nutrition.gramsPerServing, "portion")
        }
        val food = entry.foodId?.let { foodRepository.get(it) } ?: return null
        return UnitChoices(food.baseEntryUnit, food.servingSize, food.servingName ?: "serving")
    }
}
