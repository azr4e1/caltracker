package com.lorenzo.caltracker.ui.diary

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lorenzo.caltracker.data.db.entity.Goals
import com.lorenzo.caltracker.data.db.entity.LogEntry
import com.lorenzo.caltracker.data.repository.DiaryDateHolder
import com.lorenzo.caltracker.data.repository.LogRepository
import com.lorenzo.caltracker.data.repository.SettingsRepository
import com.lorenzo.caltracker.domain.Nutrients
import com.lorenzo.caltracker.ui.appViewModelFactory
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DiaryViewModel(
    private val logRepository: LogRepository,
    private val dateHolder: DiaryDateHolder,
    settingsRepository: SettingsRepository,
) : ViewModel() {
    val selectedDate: StateFlow<LocalDate> = dateHolder.date

    val goals: StateFlow<Goals?> = settingsRepository.goals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** Ids of entries selected with a long press. Empty means not in selection mode. */
    var selectedIds by mutableStateOf(emptySet<Long>())
        private set

    fun entriesFor(date: LocalDate): Flow<List<LogEntry>> = logRepository.observeDay(date)

    fun selectDate(date: LocalDate) {
        if (date != selectedDate.value) clearSelection()
        dateHolder.select(date)
    }

    fun previousDay() = selectDate(selectedDate.value.minusDays(1))

    fun nextDay() = selectDate(selectedDate.value.plusDays(1))

    fun toggleSelection(entry: LogEntry) {
        selectedIds = if (entry.id in selectedIds) selectedIds - entry.id else selectedIds + entry.id
    }

    fun clearSelection() {
        selectedIds = emptySet()
    }

    /** Changes the quantity of a food entry, scaling its nutrient snapshot. */
    fun updateAmount(entry: LogEntry, amount: Double, time: LocalTime) {
        if (amount <= 0 || entry.amount <= 0) return
        val factor = amount / entry.amount
        update(
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
        update(
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

    private fun update(entry: LogEntry) {
        viewModelScope.launch { logRepository.update(entry) }
    }

    fun delete(entries: List<LogEntry>) {
        clearSelection()
        viewModelScope.launch { logRepository.delete(entries) }
    }

    /** Puts deleted entries back with their original ids. */
    fun restore(entries: List<LogEntry>) {
        viewModelScope.launch { logRepository.restore(entries) }
    }

    fun copyTo(entries: List<LogEntry>, date: LocalDate) {
        clearSelection()
        viewModelScope.launch { logRepository.copyTo(entries, date) }
    }

    fun moveTo(entries: List<LogEntry>, date: LocalDate) {
        clearSelection()
        viewModelScope.launch { logRepository.moveTo(entries, date) }
    }

    companion object {
        val Factory = appViewModelFactory { DiaryViewModel(it.logRepository, it.diaryDate, it.settingsRepository) }
    }
}
