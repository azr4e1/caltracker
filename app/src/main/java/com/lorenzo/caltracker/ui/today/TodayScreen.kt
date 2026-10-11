package com.lorenzo.caltracker.ui.today

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lorenzo.caltracker.data.db.entity.LogEntry
import com.lorenzo.caltracker.data.db.entity.Meal
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

@Composable
fun TodayScreen(viewModel: TodayViewModel = viewModel(factory = TodayViewModel.Factory)) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        DateBar(
            date = state.date,
            onPrevious = viewModel::previousDay,
            onNext = viewModel::nextDay,
            onDateClick = { showDatePicker = true },
        )
        TotalsCard(state.totals)
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Meal.entries.forEach { meal ->
                val entries = state.entriesByMeal[meal].orEmpty()
                item(key = meal) { MealHeader(meal, entries) }
                items(entries, key = { it.id }) { EntryRow(it) }
            }
        }
    }

    if (showDatePicker) {
        DateSelectDialog(
            initial = state.date,
            onDismiss = { showDatePicker = false },
            onConfirm = {
                viewModel.selectDate(it)
                showDatePicker = false
            },
        )
    }
}

@Composable
private fun DateBar(date: LocalDate, onPrevious: () -> Unit, onNext: () -> Unit, onDateClick: () -> Unit) {
    val today = LocalDate.now()
    val label = when (date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        today.plusDays(1) -> "Tomorrow"
        else -> date.format(DateTimeFormatter.ofPattern("EEE d MMM yyyy"))
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onPrevious) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous day")
        }
        TextButton(onClick = onDateClick) {
            Text(label, style = MaterialTheme.typography.titleMedium)
        }
        IconButton(onClick = onNext) {
            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next day")
        }
    }
}

@Composable
private fun TotalsCard(totals: Totals) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Stat("kcal", totals.kcal)
            Stat("Protein", totals.protein, "g")
            Stat("Carbs", totals.carbs, "g")
            Stat("Fat", totals.fat, "g")
        }
    }
}

@Composable
private fun Stat(label: String, value: Double, unit: String = "") {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("${value.roundToInt()}$unit", style = MaterialTheme.typography.titleLarge)
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun MealHeader(meal: Meal, entries: List<LogEntry>) {
    Column(Modifier.padding(horizontal = 16.dp).padding(top = 8.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(meal.name.lowercase().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.titleSmall)
            Text("${entries.totals().kcal.roundToInt()} kcal", style = MaterialTheme.typography.titleSmall)
        }
        HorizontalDivider(Modifier.padding(top = 4.dp))
        if (entries.isEmpty()) {
            Text(
                "Nothing logged",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
        }
    }
}

@Composable
private fun EntryRow(entry: LogEntry) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(Modifier.weight(1f)) {
            Text(entry.name, style = MaterialTheme.typography.bodyLarge)
            Text(
                "${entry.amount.roundToInt()} ${entry.unit.name.lowercase()} · " +
                    "P ${entry.protein.roundToInt()} · C ${entry.carbs.roundToInt()} · F ${entry.fat.roundToInt()}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text("${entry.kcal.roundToInt()} kcal", style = MaterialTheme.typography.bodyMedium)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DateSelectDialog(initial: LocalDate, onDismiss: () -> Unit, onConfirm: (LocalDate) -> Unit) {
    // The Material date picker works in UTC milliseconds.
    val pickerState = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli(),
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                pickerState.selectedDateMillis?.let {
                    onConfirm(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
                }
            }) { Text("OK") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    ) {
        DatePicker(state = pickerState)
    }
}
