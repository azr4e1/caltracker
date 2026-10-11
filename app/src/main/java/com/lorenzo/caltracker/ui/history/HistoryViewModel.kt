package com.lorenzo.caltracker.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lorenzo.caltracker.data.db.dao.DailyTotal
import com.lorenzo.caltracker.data.db.entity.Goals
import com.lorenzo.caltracker.data.db.entity.WeightEntry
import com.lorenzo.caltracker.data.repository.DiaryDateHolder
import com.lorenzo.caltracker.data.repository.LogRepository
import com.lorenzo.caltracker.data.repository.SettingsRepository
import com.lorenzo.caltracker.data.repository.WeightRepository
import com.lorenzo.caltracker.domain.GoalCalculator
import com.lorenzo.caltracker.domain.Nutrients.Companion.KCAL_PER_G_CARBS
import com.lorenzo.caltracker.domain.Nutrients.Companion.KCAL_PER_G_FAT
import com.lorenzo.caltracker.domain.Nutrients.Companion.KCAL_PER_G_PROTEIN
import com.lorenzo.caltracker.ui.appViewModelFactory
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** How many days the chart shows at once. Older days are reached by scrolling. */
enum class HistoryRange(val days: Int, val label: String) {
    WEEK(7, "7 days"),
    MONTH(30, "30 days"),
    QUARTER(90, "90 days"),
}

/**
 * One column of the chart. Calories are split by where they come from: protein, carbs and fat
 * (grams × 4/4/9), plus "other" for calories not explained by macros, such as a quick add
 * with only calories, fiber or alcohol.
 */
data class DayColumn(
    val date: LocalDate,
    val kcal: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
) {
    val proteinKcal get() = protein * KCAL_PER_G_PROTEIN
    val carbsKcal get() = carbs * KCAL_PER_G_CARBS
    val fatKcal get() = fat * KCAL_PER_G_FAT
    val otherKcal get() = (kcal - GoalCalculator.kcalFromMacros(protein, carbs, fat)).coerceAtLeast(0.0)

    /** Height of the stacked column. */
    val stackKcal get() = proteinKcal + carbsKcal + fatKcal + otherKcal

    val isLogged get() = kcal > 0 || stackKcal > 0
}

data class HistoryUiState(
    val range: HistoryRange = HistoryRange.WEEK,
    /** Every day from the first logged day or weigh-in (or at least 90 days back) to today, oldest first. */
    val days: List<DayColumn> = emptyList(),
    val goals: Goals? = null,
    /** Weigh-ins, oldest first. */
    val weights: List<WeightEntry> = emptyList(),
    val profileWeightKg: Double? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel(
    logRepository: LogRepository,
    settingsRepository: SettingsRepository,
    private val weightRepository: WeightRepository,
    private val diaryDate: DiaryDateHolder,
) : ViewModel() {
    private val range = MutableStateFlow(HistoryRange.WEEK)
    private val weights = weightRepository.observeAll()

    private val start = combine(logRepository.observeFirstDate(), weights) { firstLog, weighIns ->
        val today = LocalDate.now()
        listOfNotNull(firstLog, weighIns.firstOrNull()?.date, today.minusDays(HistoryRange.entries.maxOf { it.days } - 1L)).min()
    }.distinctUntilChanged()

    private val allDays = start.flatMapLatest { from ->
        val today = LocalDate.now()
        logRepository.observeDailyTotals(from, today).map { fillDays(from, today, it) }
    }

    val uiState: StateFlow<HistoryUiState> = combine(range, allDays, settingsRepository.goals, weights, settingsRepository.profile) {
            r, days, goals, weighIns, profile ->
        HistoryUiState(r, days, goals, weighIns, profile?.weightKg)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    /** Saves a weigh-in; when editing one and its date changed, the old one is replaced. */
    fun saveWeight(original: WeightEntry?, date: LocalDate, weightKg: Double) {
        viewModelScope.launch {
            if (original != null && original.date != date) weightRepository.delete(original)
            weightRepository.log(date, weightKg)
        }
    }

    fun deleteWeight(entry: WeightEntry) {
        viewModelScope.launch { weightRepository.delete(entry) }
    }

    fun restoreWeight(entry: WeightEntry) {
        viewModelScope.launch { weightRepository.restore(entry) }
    }

    fun setRange(value: HistoryRange) {
        range.value = value
    }

    /** Selects [date] in the Diary; the screen then switches tabs. */
    fun openInDiary(date: LocalDate) = diaryDate.select(date)

    private fun fillDays(start: LocalDate, end: LocalDate, totals: List<DailyTotal>): List<DayColumn> {
        val byDate = totals.associateBy { it.date }
        return generateSequence(start) { it.plusDays(1) }.takeWhile { !it.isAfter(end) }.map { date ->
            val t = byDate[date]
            DayColumn(date, t?.kcal ?: 0.0, t?.protein ?: 0.0, t?.carbs ?: 0.0, t?.fat ?: 0.0)
        }.toList()
    }

    companion object {
        val Factory = appViewModelFactory { HistoryViewModel(it.logRepository, it.settingsRepository, it.weightRepository, it.diaryDate) }
    }
}
