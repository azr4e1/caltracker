package com.lorenzo.caltracker.ui.diary

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
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
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lorenzo.caltracker.data.db.entity.EntryUnit
import com.lorenzo.caltracker.data.db.entity.Goals
import com.lorenzo.caltracker.data.db.entity.LogEntry
import com.lorenzo.caltracker.domain.Nutrients
import com.lorenzo.caltracker.domain.totalNutrients
import com.lorenzo.caltracker.ui.components.DateSelectDialog
import com.lorenzo.caltracker.ui.formatAmount
import com.lorenzo.caltracker.ui.formatHm
import com.lorenzo.caltracker.ui.formatWhole
import com.lorenzo.caltracker.ui.label
import com.lorenzo.caltracker.ui.theme.macroColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.abs
import kotlinx.coroutines.launch

// The pager has one page per day; the middle page is the day the screen was opened.
private const val PAGE_COUNT = 20_000
private const val CENTER_PAGE = PAGE_COUNT / 2

private enum class DateAction { COPY, MOVE }

private data class PendingDateAction(val action: DateAction, val entries: List<LogEntry>)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiaryScreen(
    onAddFood: (LocalDate) -> Unit,
    viewModel: DiaryViewModel = viewModel(factory = DiaryViewModel.Factory),
) {
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val goals by viewModel.goals.collectAsStateWithLifecycle()
    val currentEntries by remember(selectedDate) { viewModel.entriesFor(selectedDate) }
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val selectedEntries = currentEntries.filter { it.id in viewModel.selectedIds }
    val selecting = viewModel.selectedIds.isNotEmpty()

    var showDatePicker by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf<LogEntry?>(null) }
    var pendingDateAction by remember { mutableStateOf<PendingDateAction?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val anchor = remember { LocalDate.now() }
    fun dateFor(page: Int): LocalDate = anchor.plusDays((page - CENTER_PAGE).toLong())
    fun pageFor(date: LocalDate): Int = CENTER_PAGE + ChronoUnit.DAYS.between(anchor, date).toInt()
    val pagerState = rememberPagerState(initialPage = pageFor(selectedDate)) { PAGE_COUNT }

    // Swiping changes the selected day...
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { viewModel.selectDate(dateFor(it)) }
    }
    // ...and arrows, the date picker or History move the pager.
    LaunchedEffect(selectedDate) {
        val target = pageFor(selectedDate)
        if (pagerState.currentPage != target) {
            if (abs(target - pagerState.currentPage) == 1) pagerState.animateScrollToPage(target) else pagerState.scrollToPage(target)
        }
    }

    BackHandler(enabled = selecting) { viewModel.clearSelection() }

    fun deleteWithUndo(entries: List<LogEntry>) {
        viewModel.delete(entries)
        scope.launch {
            val message = if (entries.size == 1) "Deleted ${entries[0].name}" else "Deleted ${entries.size} entries"
            val result = snackbarHostState.showSnackbar(message, actionLabel = "Undo", duration = SnackbarDuration.Short)
            if (result == SnackbarResult.ActionPerformed) viewModel.restore(entries)
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            if (selecting) {
                TopAppBar(
                    title = { Text("${viewModel.selectedIds.size} selected") },
                    navigationIcon = {
                        IconButton(onClick = viewModel::clearSelection) { Icon(Icons.Filled.Close, contentDescription = "Cancel selection") }
                    },
                    actions = {
                        TextButton(onClick = { pendingDateAction = PendingDateAction(DateAction.COPY, selectedEntries) }) { Text("Copy") }
                        TextButton(onClick = { pendingDateAction = PendingDateAction(DateAction.MOVE, selectedEntries) }) { Text("Move") }
                        IconButton(onClick = { deleteWithUndo(selectedEntries) }) { Icon(Icons.Filled.Delete, contentDescription = "Delete") }
                    },
                )
            } else {
                CenterAlignedTopAppBar(
                    title = {
                        TextButton(onClick = { showDatePicker = true }) {
                            Text(dateLabel(selectedDate), style = MaterialTheme.typography.titleLarge)
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
            }
        },
        floatingActionButton = {
            if (!selecting) {
                FloatingActionButton(onClick = { onAddFood(selectedDate) }) {
                    Icon(Icons.Filled.Add, contentDescription = "Add food")
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = !selecting,
            beyondViewportPageCount = 1,
            modifier = Modifier.fillMaxSize().padding(padding),
        ) { page ->
            val date = dateFor(page)
            val entries by remember(date) { viewModel.entriesFor(date) }.collectAsStateWithLifecycle(initialValue = null)
            DayPage(
                entries = entries,
                goals = goals,
                selectedIds = viewModel.selectedIds,
                onClick = { entry -> if (selecting) viewModel.toggleSelection(entry) else editing = entry },
                onLongClick = viewModel::toggleSelection,
            )
        }
    }

    if (showDatePicker) {
        DateSelectDialog(
            initial = selectedDate,
            onDismiss = { showDatePicker = false },
            onConfirm = {
                viewModel.selectDate(it)
                showDatePicker = false
            },
        )
    }

    pendingDateAction?.let { pending ->
        DateSelectDialog(
            initial = selectedDate,
            onDismiss = { pendingDateAction = null },
            onConfirm = { target ->
                pendingDateAction = null
                val what = if (pending.entries.size == 1) pending.entries[0].name else "${pending.entries.size} entries"
                val verb = when (pending.action) {
                    DateAction.COPY -> {
                        viewModel.copyTo(pending.entries, target)
                        "Copied"
                    }
                    DateAction.MOVE -> {
                        viewModel.moveTo(pending.entries, target)
                        "Moved"
                    }
                }
                scope.launch {
                    val result = snackbarHostState.showSnackbar(
                        "$verb $what to ${dateLabel(target)}",
                        actionLabel = "View",
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) viewModel.selectDate(target)
                }
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
            onDuplicate = {
                editing = null
                viewModel.copyTo(listOf(entry), entry.date)
                scope.launch { snackbarHostState.showSnackbar("Duplicated ${entry.name}") }
            },
            onCopyTo = {
                editing = null
                pendingDateAction = PendingDateAction(DateAction.COPY, listOf(entry))
            },
            onMoveTo = {
                editing = null
                pendingDateAction = PendingDateAction(DateAction.MOVE, listOf(entry))
            },
            onDelete = {
                editing = null
                deleteWithUndo(listOf(entry))
            },
        )
    }
}

fun dateLabel(date: LocalDate): String {
    val today = LocalDate.now()
    return when (date) {
        today -> "Today"
        today.minusDays(1) -> "Yesterday"
        today.plusDays(1) -> "Tomorrow"
        else -> date.format(DateTimeFormatter.ofPattern("EEE d MMM yyyy"))
    }
}

@Composable
private fun DayPage(
    entries: List<LogEntry>?,
    goals: Goals?,
    selectedIds: Set<Long>,
    onClick: (LogEntry) -> Unit,
    onLongClick: (LogEntry) -> Unit,
) {
    if (entries == null) return
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 88.dp)) {
        item { GoalsCard(entries.totalNutrients(), goals) }
        if (entries.isEmpty()) {
            item {
                Text(
                    "Nothing logged. Tap + to add food.\nSwipe left or right to change day.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                )
            }
        }
        itemsIndexed(entries, key = { _, entry -> entry.id }) { index, entry ->
            // Show the time only on the first of several entries logged at the same minute.
            val showTime = index == 0 || entries[index - 1].time != entry.time
            TimelineRow(
                entry = entry,
                showTime = showTime,
                isFirst = index == 0,
                isLast = index == entries.lastIndex,
                selected = entry.id in selectedIds,
                onClick = { onClick(entry) },
                onLongClick = { onLongClick(entry) },
            )
        }
    }
}

@Composable
private fun GoalsCard(totals: Nutrients, goals: Goals?) {
    val colors = macroColors()
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
                Progress(totals.kcal, goals.kcal, MaterialTheme.colorScheme.primary)
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    MacroProgress("Protein", totals.protein, goals.protein, colors.protein, Modifier.weight(1f))
                    MacroProgress("Carbs", totals.carbs, goals.carbs, colors.carbs, Modifier.weight(1f))
                    MacroProgress("Fat", totals.fat, goals.fat, colors.fat, Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun MacroProgress(label: String, value: Double, goal: Double, color: Color, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Progress(value, goal, color)
        Text("${value.formatWhole()} / ${goal.formatWhole()} g", style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun Progress(value: Double, goal: Double, color: Color) {
    val over = goal > 0 && value > goal
    LinearProgressIndicator(
        progress = { if (goal > 0) (value / goal).toFloat().coerceIn(0f, 1f) else 0f },
        color = if (over) MaterialTheme.colorScheme.error else color,
        modifier = Modifier.fillMaxWidth(),
    )
}

/** One entry: time on the left, a vertical timeline in the middle, the food on the right. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TimelineRow(
    entry: LogEntry,
    showTime: Boolean,
    isFirst: Boolean,
    isLast: Boolean,
    selected: Boolean,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
) {
    val lineColor = MaterialTheme.colorScheme.outlineVariant
    val dotColor = MaterialTheme.colorScheme.primary
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
            .background(if (selected) MaterialTheme.colorScheme.secondaryContainer else Color.Transparent)
            .combinedClickable(onClick = onClick, onLongClick = onLongClick)
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
    val unit = when {
        entry.recipeId != null && entry.unit == EntryUnit.SERVING -> if (entry.amount == 1.0) "portion" else "portions"
        else -> entry.unit.label(entry.amount)
    }
    return if (entry.isQuickAdd) "Quick add · $macros" else "${entry.amount.formatAmount()} $unit · $macros"
}
