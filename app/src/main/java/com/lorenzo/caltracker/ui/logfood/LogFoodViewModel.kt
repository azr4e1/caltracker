package com.lorenzo.caltracker.ui.logfood

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

    var food by mutableStateOf<Food?>(null)
        private set
    var amount by mutableStateOf("")
    var unit by mutableStateOf(EntryUnit.G)
    var time by mutableStateOf(LocalTime.now().withSecond(0).withNano(0))

    init {
        viewModelScope.launch {
            val loaded = foodRepository.get(route.foodId) ?: return@launch
            val last = logRepository.lastForFood(loaded.id)
            val baseUnit = if (loaded.baseUnit == BaseUnit.ML) EntryUnit.ML else EntryUnit.G
            // Pre-fill with the amount used last time, else one serving, else 100 g/ml.
            when {
                last != null && (last.unit != EntryUnit.SERVING || loaded.hasServing) -> {
                    unit = last.unit
                    amount = last.amount.formatAmount()
                }
                loaded.hasServing -> {
                    unit = EntryUnit.SERVING
                    amount = "1"
                }
                else -> {
                    unit = baseUnit
                    amount = "100"
                }
            }
            food = loaded
        }
    }

    val unitOptions: List<EntryUnit>
        get() {
            val f = food ?: return emptyList()
            val base = if (f.baseUnit == BaseUnit.ML) EntryUnit.ML else EntryUnit.G
            return if (f.hasServing) listOf(base, EntryUnit.SERVING) else listOf(base)
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

    companion object {
        fun factory(route: LogFoodRoute) = appViewModelFactory {
            LogFoodViewModel(route, it.foodRepository, it.logRepository)
        }
    }
}

private val Food.hasServing: Boolean get() = servingSize != null && servingSize > 0
