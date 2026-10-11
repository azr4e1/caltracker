package com.lorenzo.caltracker.ui.logfood

import android.database.sqlite.SQLiteConstraintException
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lorenzo.caltracker.data.db.entity.BaseUnit
import com.lorenzo.caltracker.data.db.entity.EntryUnit
import com.lorenzo.caltracker.data.db.entity.Food
import com.lorenzo.caltracker.data.db.entity.LogEntry
import com.lorenzo.caltracker.data.repository.FoodRepository
import com.lorenzo.caltracker.data.repository.LogRepository
import com.lorenzo.caltracker.domain.Nutrients
import com.lorenzo.caltracker.domain.nutrientsFor
import com.lorenzo.caltracker.ui.appViewModelFactory
import com.lorenzo.caltracker.ui.formatAmount
import com.lorenzo.caltracker.ui.navigation.LogFoodRoute
import com.lorenzo.caltracker.ui.toDecimalOrNull
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.launch

class LogFoodViewModel(
    private val route: LogFoodRoute,
    private val foodRepository: FoodRepository,
    private val logRepository: LogRepository,
) : ViewModel() {
    private val date: LocalDate = LocalDate.ofEpochDay(route.epochDay)

    /** Kept up to date, so edits made in the food editor show up when returning here. */
    var food by mutableStateOf<Food?>(null)
        private set
    var amount by mutableStateOf("")
    var unit by mutableStateOf(EntryUnit.G)
    var time by mutableStateOf(LocalTime.now().withSecond(0).withNano(0))

    /** Error from a failed delete, shown in a snackbar. */
    var message by mutableStateOf<String?>(null)

    init {
        viewModelScope.launch {
            foodRepository.observe(route.foodId).collect { loaded ->
                if (loaded != null && food == null) prefill(loaded)
                food = loaded
                if (loaded != null && unit !in unitOptions) unit = unitOptions.first()
            }
        }
    }

    /** Pre-fills with the amount used last time, else one serving, else 100 g/ml. */
    private suspend fun prefill(food: Food) {
        val last = logRepository.lastForFood(food.id)
        when {
            last != null && (last.unit != EntryUnit.SERVING || food.hasServing) -> {
                unit = last.unit
                amount = last.amount.formatAmount()
            }
            food.hasServing -> {
                unit = EntryUnit.SERVING
                amount = "1"
            }
            else -> {
                unit = food.baseEntryUnit
                amount = "100"
            }
        }
    }

    val unitOptions: List<EntryUnit>
        get() {
            val f = food ?: return emptyList()
            return if (f.hasServing) listOf(f.baseEntryUnit, EntryUnit.SERVING) else listOf(f.baseEntryUnit)
        }

    /** Nutrients for the current amount, or null if the amount is invalid. */
    val preview: Nutrients?
        get() {
            val f = food ?: return null
            val value = amount.toDecimalOrNull()?.takeIf { it > 0 } ?: return null
            val base = if (unit == EntryUnit.SERVING) value * (f.servingSize ?: return null) else value
            return f.nutrientsFor(base)
        }

    fun save(onSaved: () -> Unit) {
        val f = food ?: return
        val nutrients = preview ?: return
        viewModelScope.launch {
            logRepository.add(
                LogEntry(
                    date = date,
                    time = time,
                    foodId = f.id,
                    name = f.name,
                    amount = amount.toDecimalOrNull()!!,
                    unit = unit,
                    kcal = nutrients.kcal,
                    protein = nutrients.protein,
                    carbs = nutrients.carbs,
                    fat = nutrients.fat,
                ),
            )
            onSaved()
        }
    }

    fun delete(onDeleted: () -> Unit) {
        val f = food ?: return
        viewModelScope.launch {
            try {
                foodRepository.delete(f)
                onDeleted()
            } catch (e: SQLiteConstraintException) {
                message = "${f.name} is used in a recipe. Remove it from the recipe first."
            }
        }
    }

    companion object {
        fun factory(route: LogFoodRoute) = appViewModelFactory {
            LogFoodViewModel(route, it.foodRepository, it.logRepository)
        }
    }
}

private val Food.hasServing: Boolean get() = servingSize != null && servingSize > 0

private val Food.baseEntryUnit: EntryUnit get() = if (baseUnit == BaseUnit.ML) EntryUnit.ML else EntryUnit.G
