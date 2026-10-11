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
import com.lorenzo.caltracker.ui.toDecimalOrNull
import kotlinx.coroutines.launch

/** Form for creating a food. Nutrient values are per 100 g (or 100 ml). */
class FoodEditorViewModel(private val foodRepository: FoodRepository) : ViewModel() {
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
        val food = Food(
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
        viewModelScope.launch { onSaved(foodRepository.add(food)) }
    }

    companion object {
        val Factory = appViewModelFactory { FoodEditorViewModel(it.foodRepository) }
    }
}
