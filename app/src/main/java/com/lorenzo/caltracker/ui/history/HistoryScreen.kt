package com.lorenzo.caltracker.ui.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Tab
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lorenzo.caltracker.data.db.entity.WeightEntry
import com.lorenzo.caltracker.ui.diary.dateLabel
import com.lorenzo.caltracker.ui.formatAmount
import com.lorenzo.caltracker.ui.formatWhole
import com.lorenzo.caltracker.ui.theme.MacroColors
import com.lorenzo.caltracker.ui.theme.macroColors
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.abs
import kotlinx.coroutines.launch

private val PERIOD_FORMAT = DateTimeFormatter.ofPattern("d MMM yyyy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    onOpenDay: () -> Unit,
    viewModel: HistoryViewModel = viewModel(factory = HistoryViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var showWeight by rememberSaveable { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    fun open(date: LocalDate) {
        viewModel.openInDiary(date)
        onOpenDay()
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = { TopAppBar(title = { Text("History") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            PrimaryTabRow(selectedTabIndex = if (showWeight) 1 else 0) {
                Tab(selected = !showWeight, onClick = { showWeight = false }, text = { Text("Calories") })
                Tab(selected = showWeight, onClick = { showWeight = true }, text = { Text("Weight") })
            }
            if (showWeight) {
                WeightHistory(
                    state = state,
                    onRangeChange = viewModel::setRange,
                    onSave = viewModel::saveWeight,
                    onDelete = { entry ->
                        viewModel.deleteWeight(entry)
                        scope.launch {
                            val result = snackbarHostState.showSnackbar("Weigh-in deleted", actionLabel = "Undo", duration = SnackbarDuration.Short)
                            if (result == SnackbarResult.ActionPerformed) viewModel.restoreWeight(entry)
                        }
                    },
                )
            } else {
                CalorieHistory(state, onRangeChange = viewModel::setRange, onOpenDay = ::open)
            }
        }
    }
}

@Composable
private fun RangeSelector(range: HistoryRange, onChange: (HistoryRange) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        HistoryRange.entries.forEachIndexed { i, r ->
            SegmentedButton(
                selected = range == r,
                onClick = { onChange(r) },
                shape = SegmentedButtonDefaults.itemShape(i, HistoryRange.entries.size),
            ) { Text(r.label) }
        }
    }
}

@Composable
private fun CalorieHistory(state: HistoryUiState, onRangeChange: (HistoryRange) -> Unit, onOpenDay: (LocalDate) -> Unit) {
    var selectedIndex by rememberSaveable(state.range) { mutableStateOf<Int?>(null) }
    var visibleRange by remember(state.range) { mutableStateOf<IntRange?>(null) }
    val visibleDays = state.days.window(visibleRange, state.range.days)
    val colors = macroColors()

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                RangeSelector(state.range, onRangeChange)
                CalorieChart(
                    days = state.days,
                    visibleCount = state.range.days,
                    goalKcal = state.goals?.kcal,
                    colors = colors,
                    selectedIndex = selectedIndex,
                    onSelect = { selectedIndex = it },
                    onVisibleRangeChange = { range ->
                        visibleRange = range
                        // A selected day that scrolls off screen is deselected.
                        if (selectedIndex?.let { it !in range } == true) selectedIndex = null
                    },
                )
                Legend(colors, showOther = visibleDays.any { it.otherKcal >= 1 })
                val selected = selectedIndex?.let { state.days.getOrNull(it) }
                if (selected != null) {
                    SelectedDayCard(selected, colors, onOpen = { onOpenDay(selected.date) })
                } else {
                    SummaryCard(visibleDays, state.goals?.kcal, canScroll = state.days.size > state.range.days)
                }
                SectionTitle("Logged days")
            }
        }
        val logged = visibleDays.filter { it.isLogged }
        if (logged.isEmpty()) item { EmptyText("Nothing logged in this period.") }
        items(logged.reversed(), key = { it.date.toEpochDay() }) { day ->
            ListItem(
                headlineContent = { Text(dateLabel(day.date)) },
                supportingContent = {
                    Text("P ${day.protein.formatWhole()} g · C ${day.carbs.formatWhole()} g · F ${day.fat.formatWhole()} g")
                },
                trailingContent = { Text("${day.kcal.formatWhole()} kcal", style = MaterialTheme.typography.bodyLarge) },
                modifier = Modifier.clickable { onOpenDay(day.date) },
            )
        }
    }
}

@Composable
private fun WeightHistory(
    state: HistoryUiState,
    onRangeChange: (HistoryRange) -> Unit,
    onSave: (WeightEntry?, LocalDate, Double) -> Unit,
    onDelete: (WeightEntry) -> Unit,
) {
    var visibleRange by remember(state.range) { mutableStateOf<IntRange?>(null) }
    var dialog by remember { mutableStateOf<WeightDialogState?>(null) }
    val dates = state.days.map { it.date }
    val visibleDates = dates.window(visibleRange, state.range.days)
    val inView = state.weights.filter { visibleDates.isNotEmpty() && it.date in visibleDates.first()..visibleDates.last() }

    LazyColumn(Modifier.fillMaxSize()) {
        item {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                RangeSelector(state.range, onRangeChange)
                if (state.weights.isEmpty()) {
                    Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                        Text("Log your weight to see your trend here.", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    WeightChart(
                        dates = dates,
                        weights = state.weights.associate { it.date to it.weightKg },
                        visibleCount = state.range.days,
                        onVisibleRangeChange = { visibleRange = it },
                    )
                }
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (visibleDates.isNotEmpty()) {
                            Text(
                                "${visibleDates.first().format(PERIOD_FORMAT)} – ${visibleDates.last().format(PERIOD_FORMAT)}",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        val latest = state.weights.lastOrNull()
                        Text(
                            latest?.let { "Latest ${it.weightKg.formatAmount()} kg · ${dateLabel(it.date)}" } ?: "No weigh-ins yet",
                            style = MaterialTheme.typography.titleMedium,
                        )
                        if (inView.size >= 2) {
                            val change = inView.last().weightKg - inView.first().weightKg
                            val sign = if (change > 0) "+" else if (change < 0) "−" else "±"
                            Text("$sign${abs(change).formatAmount()} kg in this period", style = MaterialTheme.typography.bodyMedium)
                        }
                        FilledTonalButton(
                            onClick = { dialog = WeightDialogState(null) },
                            modifier = Modifier.padding(top = 8.dp),
                        ) { Text("Log weight") }
                    }
                }
                SectionTitle("Weigh-ins")
            }
        }
        if (inView.isEmpty()) item { EmptyText("No weigh-ins in this period.") }
        items(inView.reversed(), key = { it.id }) { entry ->
            ListItem(
                headlineContent = { Text(dateLabel(entry.date)) },
                trailingContent = { Text("${entry.weightKg.formatAmount()} kg", style = MaterialTheme.typography.bodyLarge) },
                modifier = Modifier.clickable { dialog = WeightDialogState(entry) },
            )
        }
    }

    dialog?.let { d ->
        WeightDialog(
            existing = d.entry,
            suggestedKg = state.weights.lastOrNull()?.weightKg ?: state.profileWeightKg,
            onDismiss = { dialog = null },
            onSave = { date, kg ->
                onSave(d.entry, date, kg)
                dialog = null
            },
            onDelete = {
                d.entry?.let(onDelete)
                dialog = null
            },
        )
    }
}

private data class WeightDialogState(val entry: WeightEntry?)

/** The items in [range], or the last [count] while the chart hasn't reported its range yet. */
private fun <T> List<T>.window(range: IntRange?, count: Int): List<T> =
    range?.let { r -> slice(r.first.coerceAtLeast(0)..r.last.coerceAtMost(lastIndex)) } ?: takeLast(count)

@Composable
private fun SectionTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(top = 8.dp),
    )
}

@Composable
private fun EmptyText(text: String) {
    Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(16.dp))
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

/** Averages over the days currently on screen. */
@Composable
private fun SummaryCard(days: List<DayColumn>, goalKcal: Double?, canScroll: Boolean) {
    val logged = days.filter { it.isLogged }
    val average = logged.takeIf { it.isNotEmpty() }?.map { it.kcal }?.average()
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            if (days.isNotEmpty()) {
                Text(
                    "${days.first().date.format(PERIOD_FORMAT)} – ${days.last().date.format(PERIOD_FORMAT)}",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                if (average != null) "Average ${average.formatWhole()} kcal/day" else "Nothing logged",
                style = MaterialTheme.typography.titleMedium,
            )
            if (average != null && goalKcal != null) {
                val diff = average - goalKcal
                Text(
                    if (diff <= 0) "${(-diff).formatWhole()} kcal below goal on average" else "${diff.formatWhole()} kcal above goal on average",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Text(
                "${logged.size} of ${days.size} days logged · tap a column for details" + if (canScroll) " · drag for earlier days" else "",
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
