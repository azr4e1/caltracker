package com.lorenzo.caltracker.ui.review

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.lorenzo.caltracker.data.repository.PendingItem
import com.lorenzo.caltracker.domain.UnitChoices
import com.lorenzo.caltracker.ui.components.MacroDonut
import com.lorenzo.caltracker.ui.components.StackedProgress
import com.lorenzo.caltracker.ui.diary.EditEntryDialog
import com.lorenzo.caltracker.ui.diary.dateLabel
import com.lorenzo.caltracker.ui.formatAmount
import com.lorenzo.caltracker.ui.formatHm
import com.lorenzo.caltracker.ui.formatWhole
import com.lorenzo.caltracker.ui.plural
import com.lorenzo.caltracker.ui.theme.macroColors
import com.lorenzo.caltracker.ui.unitLabel
import kotlinx.coroutines.launch

/** Goals: the day's progress with this selection added. Split: where this selection's calories come from. */
private enum class ChartStyle(val label: String) { GOALS("Goals"), SPLIT("Split") }

/** The foods picked in Add food, with their totals; Confirm logs them all. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReviewScreen(viewModel: ReviewViewModel, onBack: () -> Unit, onConfirmed: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    var chart by rememberSaveable { mutableStateOf(ChartStyle.GOALS) }
    var editing by remember { mutableStateOf<PendingItem?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Review") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
            )
        },
        bottomBar = {
            Surface(tonalElevation = 3.dp) {
                Button(
                    onClick = { viewModel.confirm(onConfirmed) },
                    enabled = state.items.isNotEmpty(),
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                ) { Text("Confirm · log ${plural(state.items.size, "item", "items")}") }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(bottom = 16.dp)) {
            item {
                Card(Modifier.fillMaxWidth().padding(16.dp)) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Text("Adding to ${dateLabel(viewModel.date).lowercase()}", style = MaterialTheme.typography.titleSmall)
                            SingleChoiceSegmentedButtonRow {
                                ChartStyle.entries.forEachIndexed { i, style ->
                                    SegmentedButton(
                                        selected = chart == style,
                                        onClick = { chart = style },
                                        shape = SegmentedButtonDefaults.itemShape(i, ChartStyle.entries.size),
                                        icon = {},
                                    ) { Text(style.label) }
                                }
                            }
                        }
                        when (chart) {
                            ChartStyle.GOALS -> Bars(state)
                            ChartStyle.SPLIT -> MacroDonut(state.selection, macroColors(), Modifier.fillMaxWidth())
                        }
                    }
                }
            }
            items(state.items, key = { it.key }) { item ->
                val e = item.entry
                ListItem(
                    leadingContent = { Text(e.time.formatHm(), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    headlineContent = { Text(e.name) },
                    supportingContent = {
                        val amount = if (e.isQuickAdd) "Quick add" else "${e.amount.formatAmount()} ${e.unitLabel()}"
                        Text("$amount · P ${e.protein.formatWhole()} · C ${e.carbs.formatWhole()} · F ${e.fat.formatWhole()}")
                    },
                    trailingContent = { Text("${e.kcal.formatWhole()} kcal") },
                    modifier = Modifier.clickable { editing = item },
                )
            }
            if (state.items.isEmpty()) {
                item {
                    Text(
                        "Nothing selected. Go back and add foods.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
    }

    editing?.let { item ->
        val unitChoices by produceState<UnitChoices?>(null, item.entry) { value = viewModel.unitChoices(item.entry) }
        EditEntryDialog(
            entry = item.entry,
            unitChoices = unitChoices,
            onDismiss = { editing = null },
            onSaveEntry = {
                viewModel.updateEntry(item, it)
                editing = null
            },
            onSaveQuickAdd = { name, n, time ->
                viewModel.updateQuickAdd(item, name, n, time)
                editing = null
            },
            onDuplicate = null,
            onCopyTo = null,
            onMoveTo = null,
            onSplit = null,
            onDelete = {
                editing = null
                viewModel.remove(item)
                scope.launch {
                    val result = snackbarHostState.showSnackbar("Removed ${item.entry.name}", actionLabel = "Undo", duration = SnackbarDuration.Short)
                    if (result == SnackbarResult.ActionPerformed) viewModel.restore(item)
                }
            },
            deleteLabel = "Remove",
        )
    }
}

/** The day's progress towards each goal: already logged (solid) plus this selection (lighter). */
@Composable
private fun Bars(state: ReviewUiState) {
    val goals = state.goals
    val colors = macroColors()
    val logged = state.logged
    val sel = state.selection
    if (goals == null) {
        Text("${sel.kcal.formatWhole()} kcal · P ${sel.protein.formatWhole()} · C ${sel.carbs.formatWhole()} · F ${sel.fat.formatWhole()}")
        return
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StackedProgress("Calories", logged.kcal, sel.kcal, goals.kcal, "kcal", MaterialTheme.colorScheme.primary, Modifier.fillMaxWidth())
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            StackedProgress("Protein", logged.protein, sel.protein, goals.protein, "g", colors.protein, Modifier.weight(1f))
            StackedProgress("Carbs", logged.carbs, sel.carbs, goals.carbs, "g", colors.carbs, Modifier.weight(1f))
            StackedProgress("Fat", logged.fat, sel.fat, goals.fat, "g", colors.fat, Modifier.weight(1f))
        }
    }
}
