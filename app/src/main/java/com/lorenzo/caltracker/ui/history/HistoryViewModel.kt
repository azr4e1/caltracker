package com.lorenzo.caltracker.ui.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lorenzo.caltracker.data.db.dao.DailyTotal
import com.lorenzo.caltracker.data.db.entity.Goals
import com.lorenzo.caltracker.data.repository.DiaryDateHolder
import com.lorenzo.caltracker.data.repository.LogRepository
import com.lorenzo.caltracker.data.repository.SettingsRepository
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
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

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
    /** Every day in the range, oldest first, including days with nothing logged. */
    val days: List<DayColumn> = emptyList(),
    val goals: Goals? = null,
) {
    val loggedDays: List<DayColumn> get() = days.filter { it.isLogged }
    val averageKcal: Double? get() = loggedDays.takeIf { it.isNotEmpty() }?.map { it.kcal }?.average()
}

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModel(
    logRepository: LogRepository,
    settingsRepository: SettingsRepository,
    private val diaryDate: DiaryDateHolder,
) : ViewModel() {
    private val range = MutableStateFlow(HistoryRange.WEEK)

    val uiState: StateFlow<HistoryUiState> = combine(
        range,
        range.flatMapLatest { r ->
            val today = LocalDate.now()
            logRepository.observeDailyTotals(today.minusDays(r.days - 1L), today)
        },
        settingsRepository.goals,
    ) { r, totals, goals ->
        HistoryUiState(range = r, days = fillDays(r, totals), goals = goals)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), HistoryUiState())

    fun setRange(value: HistoryRange) {
        range.value = value
    }

    /** Selects [date] in the Diary; the screen then switches tabs. */
    fun openInDiary(date: LocalDate) = diaryDate.select(date)

    private fun fillDays(range: HistoryRange, totals: List<DailyTotal>): List<DayColumn> {
        val byDate = totals.associateBy { it.date }
        val today = LocalDate.now()
        return (range.days - 1 downTo 0).map { offset ->
            val date = today.minusDays(offset.toLong())
            val t = byDate[date]
            DayColumn(date, t?.kcal ?: 0.0, t?.protein ?: 0.0, t?.carbs ?: 0.0, t?.fat ?: 0.0)
        }
    }

    companion object {
        val Factory = appViewModelFactory { HistoryViewModel(it.logRepository, it.settingsRepository, it.diaryDate) }
    }
}
