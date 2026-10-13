package com.lorenzo.caltracker.ui.review

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lorenzo.caltracker.data.db.entity.Goals
import com.lorenzo.caltracker.data.db.entity.LogEntry
import com.lorenzo.caltracker.data.repository.LogRepository
import com.lorenzo.caltracker.data.repository.PendingItem
import com.lorenzo.caltracker.data.repository.PendingLog
import com.lorenzo.caltracker.data.repository.SettingsRepository
import com.lorenzo.caltracker.data.repository.UnitChoicesRepository
import com.lorenzo.caltracker.domain.Nutrients
import com.lorenzo.caltracker.domain.totalNutrients
import com.lorenzo.caltracker.domain.UnitChoices
import com.lorenzo.caltracker.domain.withValues
import com.lorenzo.caltracker.ui.appViewModelFactory
import com.lorenzo.caltracker.ui.navigation.ReviewRoute
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ReviewUiState(
    val items: List<PendingItem> = emptyList(),
    /** What's already logged that day. */
    val logged: Nutrients = Nutrients(),
    val goals: Goals? = null,
) {
    val selection: Nutrients get() = items.map { it.entry }.totalNutrients()
}

class ReviewViewModel(
    route: ReviewRoute,
    private val pendingLog: PendingLog,
    private val logRepository: LogRepository,
    private val unitChoicesRepository: UnitChoicesRepository,
    settingsRepository: SettingsRepository,
) : ViewModel() {
    val date: LocalDate = LocalDate.ofEpochDay(route.epochDay)

    val uiState: StateFlow<ReviewUiState> = combine(
        pendingLog.items.map { items -> items.sortedWith(compareBy({ it.entry.time }, { it.key })) },
        logRepository.observeDay(date).map { it.totalNutrients() },
        settingsRepository.goalsOn(date),
        ::ReviewUiState,
    ).stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ReviewUiState())

    suspend fun unitChoices(entry: LogEntry): UnitChoices? = unitChoicesRepository.forEntry(entry)

    fun updateEntry(item: PendingItem, entry: LogEntry) = pendingLog.update(item.key, entry)

    fun updateQuickAdd(item: PendingItem, name: String, n: Nutrients, time: LocalTime) =
        pendingLog.update(item.key, item.entry.withValues(name, n).copy(time = time))

    fun remove(item: PendingItem) = pendingLog.remove(item.key)

    fun restore(item: PendingItem) = pendingLog.restore(item)

    /** Logs everything in the selection and empties it. */
    fun confirm(onDone: () -> Unit) {
        val entries: List<LogEntry> = pendingLog.items.value.map { it.entry }
        if (entries.isEmpty()) return
        viewModelScope.launch {
            logRepository.addAll(entries)
            pendingLog.clear()
            onDone()
        }
    }

    companion object {
        fun factory(route: ReviewRoute) = appViewModelFactory {
            ReviewViewModel(route, it.pendingLog, it.logRepository, it.unitChoicesRepository, it.settingsRepository)
        }
    }
}
