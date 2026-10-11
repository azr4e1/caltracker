package com.lorenzo.caltracker.ui.quickadd

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.lorenzo.caltracker.data.db.entity.EntryUnit
import com.lorenzo.caltracker.data.db.entity.LogEntry
import com.lorenzo.caltracker.data.repository.LogRepository
import com.lorenzo.caltracker.ui.appViewModelFactory
import com.lorenzo.caltracker.ui.navigation.QuickAddRoute
import com.lorenzo.caltracker.ui.toDecimalOrNull
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.coroutines.launch

class QuickAddViewModel(route: QuickAddRoute, private val logRepository: LogRepository) : ViewModel() {
    private val date = LocalDate.ofEpochDay(route.epochDay)

    var name by mutableStateOf("")
    var kcal by mutableStateOf("")
    var protein by mutableStateOf("")
    var carbs by mutableStateOf("")
    var fat by mutableStateOf("")
    var time by mutableStateOf(LocalTime.now().withSecond(0).withNano(0))

    private val macros: List<Double?>
        get() = listOf(protein, carbs, fat).map { if (it.isBlank()) 0.0 else it.toDecimalOrNull() }

    val isValid: Boolean
        get() = kcal.toDecimalOrNull()?.let { it >= 0 } == true && macros.all { it != null && it >= 0 }

    fun save(onSaved: () -> Unit) {
        if (!isValid) return
        val (p, c, f) = macros.map { it ?: 0.0 }
        viewModelScope.launch {
            logRepository.add(
                LogEntry(
                    date = date,
                    time = time,
                    name = name.trim().ifEmpty { "Quick add" },
                    amount = 1.0,
                    unit = EntryUnit.QUICK,
                    kcal = kcal.toDecimalOrNull()!!,
                    protein = p,
                    carbs = c,
                    fat = f,
                ),
            )
            onSaved()
        }
    }

    companion object {
        fun factory(route: QuickAddRoute) = appViewModelFactory { QuickAddViewModel(route, it.logRepository) }
    }
}
