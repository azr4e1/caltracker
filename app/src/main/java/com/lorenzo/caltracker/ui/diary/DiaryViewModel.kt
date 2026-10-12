package com.lorenzo.caltracker.ui.diary

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lorenzo.caltracker.data.db.entity.Goals
import com.lorenzo.caltracker.data.db.entity.LogEntry
import com.lorenzo.caltracker.data.db.entity.WeightEntry
import com.lorenzo.caltracker.data.repository.DiaryDateHolder
import com.lorenzo.caltracker.data.repository.LogRepository
import com.lorenzo.caltracker.data.repository.PreferencesRepository
import com.lorenzo.caltracker.data.repository.RecipeRepository
import com.lorenzo.caltracker.data.repository.SettingsRepository
import com.lorenzo.caltracker.data.repository.UnitChoicesRepository
import com.lorenzo.caltracker.data.repository.WeightRepository
import com.lorenzo.caltracker.domain.Nutrients
import com.lorenzo.caltracker.domain.splitRecipeEntry
import com.lorenzo.caltracker.domain.UnitChoices
import com.lorenzo.caltracker.domain.withValues
import com.lorenzo.caltracker.ui.appViewModelFactory
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class DiaryViewModel(
    private val logRepository: LogRepository,
    private val recipeRepository: RecipeRepository,
    private val weightRepository: WeightRepository,
    private val unitChoicesRepository: UnitChoicesRepository,
    private val dateHolder: DiaryDateHolder,
    settingsRepository: SettingsRepository,
    preferences: PreferencesRepository,
) : ViewModel() {
    val groupMinutes: StateFlow<Int> = preferences.groupMinutes

    val selectedDate: StateFlow<LocalDate> = dateHolder.date

    val goals: StateFlow<Goals?> = settingsRepository.goals
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val weights: StateFlow<List<WeightEntry>> = weightRepository.observeAll()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val profileWeightKg: StateFlow<Double?> = settingsRepository.profile.map { it?.weightKg }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    fun logWeight(date: LocalDate, weightKg: Double) {
        viewModelScope.launch { weightRepository.log(date, weightKg) }
    }

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

    /** Selects a whole group, or deselects it if it's already fully selected. */
    fun toggleGroup(group: List<LogEntry>) {
        val ids = group.map { it.id }.toSet()
        selectedIds = if (selectedIds.containsAll(ids)) selectedIds - ids else selectedIds + ids
    }

    fun selectAll(entries: List<LogEntry>) {
        selectedIds = entries.map { it.id }.toSet()
    }

    /**
     * Replaces a logged recipe with its ingredients. Calls [onDone] with the new entries (for undo),
     * or with an empty list if the recipe no longer exists.
     */
    fun splitRecipe(entry: LogEntry, onDone: (List<LogEntry>) -> Unit) {
        val recipeId = entry.recipeId ?: return
        viewModelScope.launch {
            val recipe = recipeRepository.get(recipeId)
            val parts = recipe?.let { splitRecipeEntry(entry, it) }.orEmpty()
            onDone(if (parts.isEmpty()) emptyList() else logRepository.replace(listOf(entry), parts))
        }
    }

    fun undoSplit(original: LogEntry, parts: List<LogEntry>) {
        viewModelScope.launch { logRepository.replace(parts, listOf(original)) }
    }

    suspend fun unitChoices(entry: LogEntry): UnitChoices? = unitChoicesRepository.forEntry(entry)

    /** Saves an edited food or recipe entry (amount, unit, time). */
    fun updateEntry(entry: LogEntry) = update(entry)

    /** Replaces the values of a quick-add entry. */
    fun updateQuickAdd(entry: LogEntry, name: String, nutrients: Nutrients, time: LocalTime) =
        update(entry.withValues(name, nutrients).copy(time = time))

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
        val Factory = appViewModelFactory {
            DiaryViewModel(it.logRepository, it.recipeRepository, it.weightRepository, it.unitChoicesRepository, it.diaryDate, it.settingsRepository, it.preferencesRepository)
        }
    }
}
