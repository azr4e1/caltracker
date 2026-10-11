package com.lorenzo.caltracker.ui.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lorenzo.caltracker.ui.diary.dateLabel
import com.lorenzo.caltracker.ui.formatWhole
import com.lorenzo.caltracker.ui.theme.MacroColors
import com.lorenzo.caltracker.ui.theme.macroColors
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onOpenDay: () -> Unit,
    viewModel: HistoryViewModel = viewModel(factory = HistoryViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedIndex by rememberSaveable(state.range) { mutableStateOf<Int?>(null) }
    val colors = macroColors()
    fun open(date: LocalDate) {
        viewModel.openInDiary(date)
        onOpenDay()
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = { TopAppBar(title = { Text("History") }) },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            item {
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                        HistoryRange.entries.forEachIndexed { i, range ->
                            SegmentedButton(
                                selected = state.range == range,
                                onClick = { viewModel.setRange(range) },
                                shape = SegmentedButtonDefaults.itemShape(i, HistoryRange.entries.size),
                            ) { Text(range.label) }
                        }
                    }
                    CalorieChart(
                        days = state.days,
                        goalKcal = state.goals?.kcal,
                        colors = colors,
                        selectedIndex = selectedIndex,
                        onSelect = { selectedIndex = it },
                    )
                    Legend(colors, showOther = state.days.any { it.otherKcal >= 1 })
                    val selected = selectedIndex?.let { state.days.getOrNull(it) }
                    if (selected != null) {
                        SelectedDayCard(selected, colors, onOpen = { open(selected.date) })
                    } else {
                        SummaryCard(state)
                    }
                    Text(
                        "Logged days",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(top = 8.dp),
                    )
                }
            }
            if (state.loggedDays.isEmpty()) {
                item {
                    Text(
                        "Nothing logged in the last ${state.range.days} days.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
            items(state.loggedDays.reversed(), key = { it.date.toEpochDay() }) { day ->
                ListItem(
                    headlineContent = { Text(dateLabel(day.date)) },
                    supportingContent = {
                        Text("P ${day.protein.formatWhole()} g · C ${day.carbs.formatWhole()} g · F ${day.fat.formatWhole()} g")
                    },
                    trailingContent = { Text("${day.kcal.formatWhole()} kcal", style = MaterialTheme.typography.bodyLarge) },
                    modifier = Modifier.clickable { open(day.date) },
                )
            }
        }
    }
}

@Composable
private fun Legend(colors: MacroColors, showOther: Boolean) {
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        LegendItem(colors.protein, "Protein")
        LegendItem(colors.carbs, "Carbs")
        LegendItem(colors.fat, "Fat")
        if (showOther) LegendItem(colors.other, "Other")
    }
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Canvas(Modifier.size(10.dp)) { drawRoundRect(color, cornerRadius = CornerRadius(2.dp.toPx())) }
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun SummaryCard(state: HistoryUiState) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            val average = state.averageKcal
            Text(
                if (average != null) "Average ${average.formatWhole()} kcal/day" else "No data yet",
                style = MaterialTheme.typography.titleMedium,
            )
            val goal = state.goals?.kcal
            if (average != null && goal != null) {
                val diff = average - goal
                Text(
                    if (diff <= 0) "${(-diff).formatWhole()} kcal below goal on average" else "${diff.formatWhole()} kcal above goal on average",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Text(
                "${state.loggedDays.size} of ${state.days.size} days logged · tap a column for details",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun SelectedDayCard(day: DayColumn, colors: MacroColors, onOpen: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(dateLabel(day.date), style = MaterialTheme.typography.titleMedium)
                Text("${day.kcal.formatWhole()} kcal", style = MaterialTheme.typography.titleMedium)
            }
            if (day.isLogged) {
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    LegendItem(colors.protein, "${day.protein.formatWhole()} g")
                    LegendItem(colors.carbs, "${day.carbs.formatWhole()} g")
                    LegendItem(colors.fat, "${day.fat.formatWhole()} g")
                    if (day.otherKcal >= 1) LegendItem(colors.other, "${day.otherKcal.formatWhole()} kcal other")
                }
            } else {
                Text("Nothing logged", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            FilledTonalButton(onClick = onOpen) { Text("Open in diary") }
        }
    }
}
