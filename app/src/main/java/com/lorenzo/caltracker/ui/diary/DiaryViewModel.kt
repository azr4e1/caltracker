package com.lorenzo.caltracker.ui.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lorenzo.caltracker.data.db.entity.Goals
import com.lorenzo.caltracker.data.db.entity.LogEntry
import com.lorenzo.caltracker.data.repository.LogRepository
import com.lorenzo.caltracker.data.repository.SettingsRepository
import com.lorenzo.caltracker.domain.Nutrients
import com.lorenzo.caltracker.domain.totalNutrients
import com.lorenzo.caltracker.ui.appViewModelFactory
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class DiaryUiState(
    val date: LocalDate = LocalDate.now(),
    val entries: List<LogEntry> = emptyList(),
    val totals: Nutrients = Nutrients(),
    val goals: Goals? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class DiaryViewModel(
    private val logRepository: LogRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {
    private val selectedDate = MutableStateFlow(LocalDate.now())

    private val day = selectedDate.flatMapLatest { date ->
        logRepository.observeDay(date).map { date to it }
    }

    val uiState: StateFlow<DiaryUiState> = combine(day, settingsRepository.goals) { (date, entries), goals ->
        DiaryUiState(date = date, entries = entries, totals = entries.totalNutrients(), goals = goals)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), DiaryUiState())

    fun selectDate(date: LocalDate) {
        selectedDate.value = date
    }

    fun previousDay() = selectDate(selectedDate.value.minusDays(1))

    fun nextDay() = selectDate(selectedDate.value.plusDays(1))

    /** Changes the quantity of a food/recipe entry, scaling its nutrient snapshot. */
    fun updateAmount(entry: LogEntry, amount: Double, time: LocalTime) {
        if (amount <= 0 || entry.amount <= 0) return
        val factor = amount / entry.amount
        save(
            entry.copy(
                amount = amount,
                time = time,
                kcal = entry.kcal * factor,
                protein = entry.protein * factor,
                carbs = entry.carbs * factor,
                fat = entry.fat * factor,
            ),
        )
    }

    /** Replaces the values of a quick-add entry. */
    fun updateQuickAdd(entry: LogEntry, name: String, nutrients: Nutrients, time: LocalTime) {
        save(
            entry.copy(
                name = name,
                time = time,
                kcal = nutrients.kcal,
                protein = nutrients.protein,
                carbs = nutrients.carbs,
                fat = nutrients.fat,
            ),
        )
    }

    private fun save(entry: LogEntry) {
        viewModelScope.launch { logRepository.update(entry) }
    }

    fun delete(entry: LogEntry) {
        viewModelScope.launch { logRepository.delete(entry) }
    }

    /** Puts a deleted entry back with its original id. */
    fun restore(entry: LogEntry) {
        viewModelScope.launch { logRepository.add(entry) }
    }

    companion object {
        val Factory = appViewModelFactory { DiaryViewModel(it.logRepository, it.settingsRepository) }
    }
}
