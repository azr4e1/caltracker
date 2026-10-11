package com.lorenzo.caltracker.ui.foodeditor

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lorenzo.caltracker.data.db.entity.BaseUnit
import com.lorenzo.caltracker.data.db.entity.Food
import com.lorenzo.caltracker.data.repository.FoodRepository
import com.lorenzo.caltracker.domain.GoalCalculator
import com.lorenzo.caltracker.ui.appViewModelFactory
import com.lorenzo.caltracker.ui.formatAmount
import com.lorenzo.caltracker.ui.toDecimalOrNull
import kotlinx.coroutines.launch

/**
 * Form for creating a food, or editing one when [foodId] is set.
 * Nutrient values are per 100 g (or 100 ml).
 */
class FoodEditorViewModel(private val foodId: Long?, private val foodRepository: FoodRepository) : ViewModel() {
    private var original: Food? = null

    val isEditing: Boolean get() = foodId != null

    var name by mutableStateOf("")
    var brand by mutableStateOf("")
    var baseUnit by mutableStateOf(BaseUnit.G)
    var kcal by mutableStateOf("")
    var protein by mutableStateOf("")
    var carbs by mutableStateOf("")
    var fat by mutableStateOf("")
    var fiber by mutableStateOf("")
    var sugar by mutableStateOf("")
    var saturatedFat by mutableStateOf("")
    var salt by mutableStateOf("")
    var servingName by mutableStateOf("")
    var servingSize by mutableStateOf("")

    init {
        if (foodId != null) {
            viewModelScope.launch {
                val f = foodRepository.get(foodId) ?: return@launch
                original = f
                name = f.name
                brand = f.brand.orEmpty()
                baseUnit = f.baseUnit
                kcal = f.kcalPer100.formatAmount()
                protein = f.proteinPer100.formatAmount()
                carbs = f.carbsPer100.formatAmount()
                fat = f.fatPer100.formatAmount()
                fiber = f.fiberPer100?.formatAmount().orEmpty()
                sugar = f.sugarPer100?.formatAmount().orEmpty()
                saturatedFat = f.saturatedFatPer100?.formatAmount().orEmpty()
                salt = f.saltPer100?.formatAmount().orEmpty()
                servingName = f.servingName.orEmpty()
                servingSize = f.servingSize?.formatAmount().orEmpty()
            }
        }
    }

    private fun required(value: String) = value.toDecimalOrNull()?.takeIf { it >= 0 }
    private fun optional(value: String) = if (value.isBlank()) null else value.toDecimalOrNull()

    fun isInvalidRequired(value: String) = value.isNotBlank() && required(value) == null
    fun isInvalidOptional(value: String) = value.isNotBlank() && value.toDecimalOrNull() == null

    val isValid: Boolean
        get() = name.isNotBlank() &&
            listOf(kcal, protein, carbs, fat).all { required(it) != null } &&
            listOf(fiber, sugar, saturatedFat, salt).none(::isInvalidOptional) &&
            (servingSize.isBlank() || (servingSize.toDecimalOrNull() ?: 0.0) > 0)

    /** Calories implied by the macros, to help spot typos. Null until all three are filled in. */
    val kcalFromMacros: Double?
        get() {
            val p = required(protein) ?: return null
            val c = required(carbs) ?: return null
            val f = required(fat) ?: return null
            return GoalCalculator.kcalFromMacros(p, c, f)
        }

    fun save(onSaved: (Long) -> Unit) {
        if (!isValid) return
        if (isEditing && original == null) return
        // Editing keeps the id, barcode, source and creation time of the original.
        val base = original ?: Food(name = "", kcalPer100 = 0.0, proteinPer100 = 0.0, carbsPer100 = 0.0, fatPer100 = 0.0)
        val food = base.copy(
            name = name.trim(),
            brand = brand.trim().ifEmpty { null },
            baseUnit = baseUnit,
            kcalPer100 = required(kcal)!!,
            proteinPer100 = required(protein)!!,
            carbsPer100 = required(carbs)!!,
            fatPer100 = required(fat)!!,
            fiberPer100 = optional(fiber),
            sugarPer100 = optional(sugar),
            saturatedFatPer100 = optional(saturatedFat),
            saltPer100 = optional(salt),
            servingName = servingName.trim().ifEmpty { null }?.takeIf { servingSize.isNotBlank() },
            servingSize = optional(servingSize),
        )
        viewModelScope.launch {
            if (isEditing) {
                foodRepository.update(food)
                onSaved(food.id)
            } else {
                onSaved(foodRepository.add(food))
            }
        }
    }

    companion object {
        fun factory(foodId: Long?) = appViewModelFactory { FoodEditorViewModel(foodId, it.foodRepository) }
    }
}
