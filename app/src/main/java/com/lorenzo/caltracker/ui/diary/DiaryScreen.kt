package com.lorenzo.caltracker.ui.diary

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lorenzo.caltracker.data.db.entity.Goals
import com.lorenzo.caltracker.data.db.entity.LogEntry
import com.lorenzo.caltracker.domain.Nutrients
import com.lorenzo.caltracker.ui.components.DateSelectDialog
import com.lorenzo.caltracker.ui.formatAmount
import com.lorenzo.caltracker.ui.formatHm
import com.lorenzo.caltracker.ui.formatWhole
import com.lorenzo.caltracker.ui.label
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryScreen(
    onAddFood: (LocalDate) -> Unit,
    viewModel: DiaryViewModel = viewModel(factory = DiaryViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showDatePicker by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<LogEntry?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    TextButton(onClick = { showDatePicker = true }) {
                        Text(dateLabel(state.date), style = MaterialTheme.typography.titleLarge)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = viewModel::previousDay) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowLeft, contentDescription = "Previous day")
                    }
                },
                actions = {
                    IconButton(onClick = viewModel::nextDay) {
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = "Next day")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { onAddFood(state.date) }) {
                Icon(Icons.Filled.Add, contentDescription = "Add food")
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(bottom = 88.dp),
        ) {
            item { GoalsCard(state.totals, state.goals) }
            if (state.entries.isEmpty()) {
                item {
                    Text(
                        "Nothing logged. Tap + to add food.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth().padding(32.dp),
                    )
                }
            }
            itemsIndexed(state.entries, key = { _, entry -> entry.id }) { index, entry ->
                // Show the time only on the first of several entries logged at the same minute.
                val showTime = index == 0 || state.entries[index - 1].time != entry.time
                TimelineRow(
                    entry = entry,
                    showTime = showTime,
                    isFirst = index == 0,
                    isLast = index == state.entries.lastIndex,
                    onClick = { editing = entry },
                )
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

    editing?.let { entry ->
        EditEntryDialog(
            entry = entry,
            onDismiss = { editing = null },
            onSaveAmount = { amount, time ->
                viewModel.updateAmount(entry, amount, time)
                editing = null
            },
            onSaveQuickAdd = { name, nutrients, time ->
                viewModel.updateQuickAdd(entry, name, nutrients, time)
                editing = null
            },
            onDelete = {
                viewModel.delete(entry)
                editing = null
                scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        message = "Deleted ${entry.name}",
                        actionLabel = "Undo",
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) viewModel.restore(entry)
                }
            },
        )
    }
}

private fun dateLabel(date: LocalDate): String {
    val today = LocalDate.now()
    return when (date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        today.plusDays(1) -> "Tomorrow"
        else -> date.format(DateTimeFormatter.ofPattern("EEE d MMM yyyy"))
    }
}

@Composable
private fun GoalsCard(totals: Nutrients, goals: Goals?) {
    Card(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (goals == null) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("${totals.kcal.formatWhole()} kcal", style = MaterialTheme.typography.titleLarge)
                    Text(
                        "P ${totals.protein.formatWhole()} · C ${totals.carbs.formatWhole()} · F ${totals.fat.formatWhole()}",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                Text(
                    "Set your goals in Settings to track progress.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                val left = goals.kcal - totals.kcal
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    Text(
                        "${totals.kcal.formatWhole()} / ${goals.kcal.formatWhole()} kcal",
                        style = MaterialTheme.typography.titleLarge,
                    )
                    Text(
                        if (left >= 0) "${left.formatWhole()} left" else "${(-left).formatWhole()} over",
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (left >= 0) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error,
                    )
                }
                Progress(totals.kcal, goals.kcal)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MacroProgress("Protein", totals.protein, goals.protein, Modifier.weight(1f))
                    MacroProgress("Carbs", totals.carbs, goals.carbs, Modifier.weight(1f))
                    MacroProgress("Fat", totals.fat, goals.fat, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MacroProgress(label: String, value: Double, goal: Double, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Progress(value, goal)
        Text("${value.formatWhole()} / ${goal.formatWhole()} g", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun Progress(value: Double, goal: Double) {
    val over = goal > 0 && value > goal
    LinearProgressIndicator(
        progress = { if (goal > 0) (value / goal).toFloat().coerceIn(0f, 1f) else 0f },
        color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth(),
    )
}

/** One entry: time on the left, a vertical timeline in the middle, the food on the right. */
@Composable
private fun TimelineRow(entry: LogEntry, showTime: Boolean, isFirst: Boolean, isLast: Boolean, onClick: () -> Unit) {
    val lineColor = MaterialTheme.colorScheme.outlineVariant
    val dotColor = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
    ) {
        Text(
            text = if (showTime) entry.time.formatHm() else "",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(48.dp).padding(top = 10.dp),
        )
        Box(
            Modifier
                .width(20.dp)
                .fillMaxHeight()
                .drawBehind {
                    val x = size.width / 2
                    val dotY = 18.dp.toPx()
                    if (!(isFirst && isLast)) {
                        val top = if (isFirst) dotY else 0f
                        val bottom = if (isLast) dotY else size.height
                        drawLine(lineColor, Offset(x, top), Offset(x, bottom), strokeWidth = 2.dp.toPx())
                    }
                    if (showTime) drawCircle(dotColor, radius = 5.dp.toPx(), center = Offset(x, dotY))
                },
        )
        Spacer(Modifier.width(8.dp))
        Row(Modifier.weight(1f).padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(entry.name, style = MaterialTheme.typography.bodyLarge)
                Text(entryDetails(entry), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("${entry.kcal.formatWhole()} kcal", style = MaterialTheme.typography.bodyMedium)
        }
    }
}

private fun entryDetails(entry: LogEntry): String {
    val macros = "P ${entry.protein.formatWhole()} · C ${entry.carbs.formatWhole()} · F ${entry.fat.formatWhole()}"
    return if (entry.isQuickAdd) "Quick add · $macros" else "${entry.amount.formatAmount()} ${entry.unit.label(entry.amount)} · $macros"
}
