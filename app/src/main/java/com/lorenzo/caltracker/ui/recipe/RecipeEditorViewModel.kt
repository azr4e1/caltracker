package com.lorenzo.caltracker.ui.recipe

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lorenzo.caltracker.data.db.entity.EntryUnit
import com.lorenzo.caltracker.data.db.entity.Food
import com.lorenzo.caltracker.data.db.entity.LogEntry
import com.lorenzo.caltracker.data.db.entity.Recipe
import com.lorenzo.caltracker.data.repository.FoodRepository
import com.lorenzo.caltracker.data.repository.LogRepository
import com.lorenzo.caltracker.data.repository.RecipeRepository
import com.lorenzo.caltracker.domain.RecipeNutrition
import com.lorenzo.caltracker.domain.baseAmount
import com.lorenzo.caltracker.domain.fractionOf
import com.lorenzo.caltracker.domain.scaledIngredients
import com.lorenzo.caltracker.ui.appViewModelFactory
import com.lorenzo.caltracker.ui.formatAmount
import com.lorenzo.caltracker.ui.toDecimalOrNull
import kotlinx.coroutines.launch

/** One ingredient in the form: [amount] is in the food's base unit (g or ml). */
data class IngredientRow(val food: Food, val amount: Double)

/** The ingredient being added ([index] null) or changed in the amount dialog. */
data class IngredientEdit(val food: Food, val amount: Double, val index: Int?)

class RecipeEditorViewModel(
    private val recipeId: Long,
    fromEntryIds: List<Long>,
    private val recipeRepository: RecipeRepository,
    private val foodRepository: FoodRepository,
    private val logRepository: LogRepository,
) : ViewModel() {
    val isEditing: Boolean get() = recipeId != 0L
    private var original: Recipe? = null

    var name by mutableStateOf("")
    var servings by mutableStateOf("1")
    var cookedWeight by mutableStateOf("")
    var ingredients by mutableStateOf(emptyList<IngredientRow>())
        private set
    var editing by mutableStateOf<IngredientEdit?>(null)

    /** One-off message to show in a snackbar. */
    var message by mutableStateOf<String?>(null)

    /** Diary entries that became ingredients; on save they are replaced by one entry of the new recipe. */
    private var sourceEntries = emptyList<LogEntry>()

    /** How many diary entries the saved recipe replaced (0 when not created from the diary). */
    var replacedCount = 0
        private set

    private var savedSnapshot by mutableStateOf<List<Any>?>(null)
    private fun snapshot(): List<Any> = listOf(name, servings, cookedWeight, ingredients)

    init {
        viewModelScope.launch {
            if (isEditing) {
                recipeRepository.get(recipeId)?.let { r ->
                    original = r.recipe
                    name = r.recipe.name
                    servings = r.recipe.servings.formatAmount()
                    cookedWeight = r.recipe.totalWeight?.formatAmount().orEmpty()
                    ingredients = r.ingredients.sortedBy { it.ingredient.position }.map { IngredientRow(it.food, it.ingredient.amount) }
                }
            }
            savedSnapshot = snapshot()
            if (fromEntryIds.isNotEmpty()) prefillFromEntries(fromEntryIds)
        }
    }

    /** Turns diary entries into ingredients: foods directly, recipes expanded, quick adds skipped. */
    private suspend fun prefillFromEntries(ids: List<Long>) {
        val rows = mutableListOf<IngredientRow>()
        val used = mutableListOf<LogEntry>()
        var skipped = 0
        for (entry in logRepository.getByIds(ids)) {
            val food = entry.foodId?.let { foodRepository.get(it) }
            val recipe = entry.recipeId?.let { recipeRepository.get(it) }
            val parts: List<IngredientRow>? = when {
                food != null -> entry.baseAmount(food)?.let { listOf(IngredientRow(food, it)) }
                recipe != null -> recipe.fractionOf(entry)?.let { f -> recipe.scaledIngredients(f).map { (food, amount) -> IngredientRow(food, amount) } }
                else -> null
            }
            if (parts == null) {
                skipped++
            } else {
                rows += parts
                used += entry
            }
        }
        sourceEntries = used
        // The same food logged twice becomes one ingredient.
        ingredients = rows.groupBy { it.food.id }.map { (_, same) -> IngredientRow(same.first().food, same.sumOf { it.amount }) }
        if (skipped > 0) {
            message = "$skipped ${if (skipped == 1) "entry was" else "entries were"} skipped and stays in your diary: " +
                "quick adds and deleted foods can't be ingredients."
        }
    }

    val hasUnsavedChanges: Boolean get() = savedSnapshot.let { it != null && it != snapshot() }

    private val servingsValue get() = servings.toDecimalOrNull()?.takeIf { it > 0 }
    private val cookedWeightValue get() = if (cookedWeight.isBlank()) null else cookedWeight.toDecimalOrNull()?.takeIf { it > 0 }

    val isCookedWeightInvalid: Boolean get() = cookedWeight.isNotBlank() && cookedWeightValue == null

    val ingredientsWeight: Double get() = ingredients.sumOf { it.amount }

    val nutrition: RecipeNutrition
        get() = RecipeNutrition.of(ingredients.map { it.food to it.amount }, servingsValue ?: 1.0, cookedWeightValue)

    val isValid: Boolean
        get() = name.isNotBlank() && servingsValue != null && ingredients.isNotEmpty() && !isCookedWeightInvalid

    /** A food came back from the picker: ask for its amount. */
    fun onFoodPicked(foodId: Long) {
        viewModelScope.launch {
            val food = foodRepository.get(foodId) ?: return@launch
            val amount = food.servingSize?.takeIf { it > 0 } ?: 100.0
            editing = IngredientEdit(food, amount, index = null)
        }
    }

    fun editIngredient(index: Int) {
        val row = ingredients.getOrNull(index) ?: return
        editing = IngredientEdit(row.food, row.amount, index)
    }

    fun confirmIngredient(amount: Double) {
        val edit = editing ?: return
        val row = IngredientRow(edit.food, amount)
        ingredients = if (edit.index == null) ingredients + row else ingredients.toMutableList().also { it[edit.index] = row }
        editing = null
    }

    fun removeIngredient() {
        val index = editing?.index ?: return
        ingredients = ingredients.filterIndexed { i, _ -> i != index }
        editing = null
    }

    fun save(onSaved: (Long) -> Unit) {
        if (!isValid) return
        val base = original ?: Recipe(name = "")
        val recipe = base.copy(name = name.trim(), servings = servingsValue!!, totalWeight = cookedWeightValue)
        viewModelScope.launch {
            val id = recipeRepository.save(recipe, ingredients.map { it.food.id to it.amount })
            if (sourceEntries.isNotEmpty()) replaceSourceEntries(recipe.copy(id = id))
            savedSnapshot = snapshot()
            onSaved(id)
        }
    }

    /** Swaps the diary entries the recipe was made from for one entry of the whole recipe, at the latest of their times. */
    private suspend fun replaceSourceEntries(recipe: Recipe) {
        val total = nutrition.total
        val latest = sourceEntries.maxBy { it.time }
        logRepository.replace(
            sourceEntries,
            listOf(
                LogEntry(
                    date = latest.date,
                    time = latest.time,
                    recipeId = recipe.id,
                    name = recipe.name,
                    amount = recipe.servings,
                    unit = EntryUnit.SERVING,
                    kcal = total.kcal,
                    protein = total.protein,
                    carbs = total.carbs,
                    fat = total.fat,
                ),
            ),
        )
        replacedCount = sourceEntries.size
    }

    companion object {
        fun factory(recipeId: Long, fromEntryIds: List<Long> = emptyList()) = appViewModelFactory {
            RecipeEditorViewModel(recipeId, fromEntryIds, it.recipeRepository, it.foodRepository, it.logRepository)
        }
    }
}
