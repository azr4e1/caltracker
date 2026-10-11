package com.lorenzo.caltracker.ui.foodeditor

import android.database.sqlite.SQLiteConstraintException
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lorenzo.caltracker.data.db.entity.BaseUnit
import com.lorenzo.caltracker.data.db.entity.Food
import com.lorenzo.caltracker.data.remote.RemoteFood
import com.lorenzo.caltracker.data.repository.FoodRepository
import com.lorenzo.caltracker.domain.GoalCalculator
import com.lorenzo.caltracker.ui.appViewModelFactory
import com.lorenzo.caltracker.ui.formatAmount
import com.lorenzo.caltracker.ui.toDecimalOrNull
import kotlinx.coroutines.launch

/**
 * Form for creating a food, or editing one when [foodId] is set. A [draft] (from a scan or an
 * online search with missing values) pre-fills a new food. Nutrient values are per 100 g (or 100 ml).
 */
class FoodEditorViewModel(
    private val foodId: Long?,
    draft: RemoteFood?,
    private val foodRepository: FoodRepository,
) : ViewModel() {
    private var original: Food? = draft?.toFood()?.let { it.copy(externalId = it.externalId?.ifEmpty { null }) }

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
    var barcode by mutableStateOf("")

    /** Error to show in a snackbar. */
    var message by mutableStateOf<String?>(null)

    init {
        if (draft != null) {
            name = draft.name
            brand = draft.brand.orEmpty()
            barcode = draft.barcode.orEmpty()
            baseUnit = draft.baseUnit
            kcal = draft.kcal?.formatAmount().orEmpty()
            protein = draft.protein?.formatAmount().orEmpty()
            carbs = draft.carbs?.formatAmount().orEmpty()
            fat = draft.fat?.formatAmount().orEmpty()
            fiber = draft.fiber?.formatAmount().orEmpty()
            sugar = draft.sugar?.formatAmount().orEmpty()
            saturatedFat = draft.saturatedFat?.formatAmount().orEmpty()
            salt = draft.salt?.formatAmount().orEmpty()
            servingSize = draft.servingSize?.formatAmount().orEmpty()
            servingName = if (draft.servingSize != null) "serving" else ""
        }
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
                barcode = f.barcode.orEmpty()
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
        // Editing keeps the id, source and creation time of the original.
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
            barcode = barcode.trim().ifEmpty { null },
        )
        viewModelScope.launch {
            try {
                if (isEditing) {
                    foodRepository.update(food)
                    onSaved(food.id)
                } else {
                    onSaved(foodRepository.add(food))
                }
            } catch (e: SQLiteConstraintException) {
                message = "Another saved food already has barcode ${food.barcode}."
            }
        }
    }

    companion object {
        fun factory(foodId: Long?, draft: RemoteFood? = null) =
            appViewModelFactory { FoodEditorViewModel(foodId, draft, it.foodRepository) }
    }
}
