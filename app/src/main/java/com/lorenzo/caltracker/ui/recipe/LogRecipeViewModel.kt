package com.lorenzo.caltracker.ui.recipe

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lorenzo.caltracker.data.db.dao.RecipeWithIngredients
import com.lorenzo.caltracker.data.db.entity.EntryUnit
import com.lorenzo.caltracker.data.db.entity.LogEntry
import com.lorenzo.caltracker.data.repository.LogRepository
import com.lorenzo.caltracker.data.repository.RecipeRepository
import com.lorenzo.caltracker.domain.Nutrients
import com.lorenzo.caltracker.domain.nutrition
import com.lorenzo.caltracker.ui.appViewModelFactory
import com.lorenzo.caltracker.ui.formatAmount
import com.lorenzo.caltracker.ui.navigation.LogRecipeRoute
import com.lorenzo.caltracker.ui.toDecimalOrNull
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.launch

class LogRecipeViewModel(
    route: LogRecipeRoute,
    private val recipeRepository: RecipeRepository,
    private val logRepository: LogRepository,
) : ViewModel() {
    private val date = LocalDate.ofEpochDay(route.epochDay)

    /** Kept up to date, so edits made in the recipe editor show up when returning here. */
    var recipe by mutableStateOf<RecipeWithIngredients?>(null)
        private set

    /** [EntryUnit.SERVING] (portions) or [EntryUnit.G]. */
    var unit by mutableStateOf(EntryUnit.SERVING)
    var amount by mutableStateOf("1")
    var time by mutableStateOf(LocalTime.now().withSecond(0).withNano(0))

    init {
        viewModelScope.launch {
            // Pre-fill with the amount used last time.
            recipeRepository.lastEntry(route.recipeId)?.let { last ->
                unit = last.unit
                amount = last.amount.formatAmount()
            }
            recipeRepository.observe(route.recipeId).collect { recipe = it }
        }
    }

    val preview: Nutrients?
        get() {
            val r = recipe ?: return null
            val value = amount.toDecimalOrNull()?.takeIf { it > 0 } ?: return null
            return if (unit == EntryUnit.G) r.nutrition.forWeight(value) else r.nutrition.forServings(value)
        }

    fun save(onSaved: () -> Unit) {
        val r = recipe ?: return
        val n = preview ?: return
        viewModelScope.launch {
            logRepository.add(
                LogEntry(
                    date = date,
                    time = time,
                    recipeId = r.recipe.id,
                    name = r.recipe.name,
                    amount = amount.toDecimalOrNull()!!,
                    unit = unit,
                    kcal = n.kcal,
                    protein = n.protein,
                    carbs = n.carbs,
                    fat = n.fat,
                ),
            )
            onSaved()
        }
    }

    fun delete(onDeleted: () -> Unit) {
        val r = recipe ?: return
        viewModelScope.launch {
            recipeRepository.delete(r.recipe)
            onDeleted()
        }
    }

    companion object {
        fun factory(route: LogRecipeRoute) = appViewModelFactory { LogRecipeViewModel(route, it.recipeRepository, it.logRepository) }
    }
}
