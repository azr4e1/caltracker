package com.lorenzo.caltracker.ui.today

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.lorenzo.caltracker.CalTrackerApp
import com.lorenzo.caltracker.data.db.entity.LogEntry
import com.lorenzo.caltracker.data.db.entity.Meal
import com.lorenzo.caltracker.data.repository.LogRepository
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

data class Totals(val kcal: Double = 0.0, val protein: Double = 0.0, val carbs: Double = 0.0, val fat: Double = 0.0)

fun List<LogEntry>.totals() = Totals(
    kcal = sumOf { it.kcal },
    protein = sumOf { it.protein },
    carbs = sumOf { it.carbs },
    fat = sumOf { it.fat },
)

data class TodayUiState(
    val date: LocalDate = LocalDate.now(),
    val entriesByMeal: Map<Meal, List<LogEntry>> = emptyMap(),
    val totals: Totals = Totals(),
)

@OptIn(ExperimentalCoroutinesApi::class)
class TodayViewModel(private val logRepository: LogRepository) : ViewModel() {
    private val selectedDate = MutableStateFlow(LocalDate.now())

    val uiState: StateFlow<TodayUiState> = combine(
        selectedDate,
        selectedDate.flatMapLatest { logRepository.observeDay(it) },
    ) { date, entries ->
        TodayUiState(
            date = date,
            entriesByMeal = entries.groupBy { it.meal },
            totals = entries.totals(),
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), TodayUiState())

    fun selectDate(date: LocalDate) {
        selectedDate.value = date
    }

    fun previousDay() = selectDate(selectedDate.value.minusDays(1))

    fun nextDay() = selectDate(selectedDate.value.plusDays(1))

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY] as CalTrackerApp
                TodayViewModel(app.container.logRepository)
            }
        }
    }
}
