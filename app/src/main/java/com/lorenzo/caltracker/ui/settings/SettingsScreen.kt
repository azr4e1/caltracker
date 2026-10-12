package com.lorenzo.caltracker.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lorenzo.caltracker.data.db.entity.GoalMode
import com.lorenzo.caltracker.data.db.entity.Goals
import com.lorenzo.caltracker.data.db.entity.Profile
import com.lorenzo.caltracker.data.repository.PreferencesRepository
import com.lorenzo.caltracker.domain.GoalCalculator
import com.lorenzo.caltracker.ui.formatAmount
import com.lorenzo.caltracker.ui.formatWhole
import com.lorenzo.caltracker.ui.profile.label
import com.lorenzo.caltracker.ui.profile.title
import java.time.LocalDate

private const val XLSX_MIME = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onEditProfile: () -> Unit,
    onEditGoals: () -> Unit,
    onDataDeleted: () -> Unit,
    viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel.message) {
        viewModel.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.message = null
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = { TopAppBar(title = { Text("Settings") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ProfileCard(state.profile, onClick = onEditProfile)
            GoalsCard(state.goals, state.profile, onClick = onEditGoals)
            val groupMinutes by viewModel.groupMinutes.collectAsStateWithLifecycle()
            var pickingGroup by remember { mutableStateOf(false) }

            if (pickingGroup) {
                GroupMinutesDialog(
                    current = groupMinutes,
                    onDismiss = { pickingGroup = false },
                    onSelect = {
                        viewModel.setGroupMinutes(it)
                        pickingGroup = false
                    },
                )
            }
            val usdaKey by viewModel.usdaApiKey.collectAsStateWithLifecycle()
            GeneralCard(
                listOf(
                    SettingRow("Group diary entries within", groupLabel(groupMinutes)) { pickingGroup = true },
                    SettingRow("USDA API key", usdaKey?.let { "Your key ••••${it.takeLast(4)}" } ?: "Shared (limited)", viewModel::openUsdaKey),
                ),
            )
            BackupSection(viewModel)
            DeleteDataSection(onDelete = { viewModel.deleteAllData(onDataDeleted) })
        }
    }

    if (viewModel.editingUsdaKey) {
        UsdaKeyDialog(
            currentKey = viewModel.usdaApiKey.collectAsStateWithLifecycle().value,
            checking = viewModel.checkingUsdaKey,
            error = viewModel.usdaKeyError,
            onDismiss = { viewModel.editingUsdaKey = false },
            onSave = viewModel::saveUsdaKey,
            onUseShared = viewModel::useSharedUsdaKey,
        )
    }
}

private data class SettingRow(val label: String, val value: String, val onClick: () -> Unit)

/** App settings; each row opens its own editor. */
@Composable
private fun GeneralCard(rows: List<SettingRow>) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(vertical = 8.dp)) {
            Text(
                "General",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
            rows.forEach { row ->
                Row(
                    Modifier
                        .fillMaxWidth()
                        .clickable(onClick = row.onClick)
                        .padding(start = 16.dp, end = 8.dp, top = 10.dp, bottom = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(row.label, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.weight(1f))
                    Text(row.value)
                    Icon(
                        Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = "Change ${row.label}",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

/** A tappable card of label/value rows; tapping opens its editor. */
@Composable
private fun SummaryCard(title: String, rows: List<Pair<String, String>>, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
                Icon(Icons.Filled.Edit, contentDescription = "Edit $title", tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            rows.forEach { (label, value) ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(value)
                }
            }
        }
    }
}

@Composable
private fun ProfileCard(profile: Profile?, onClick: () -> Unit) {
    if (profile == null) {
        SummaryCard("Profile", listOf("Not set" to ""), onClick)
        return
    }
    SummaryCard(
        "Profile",
        listOf(
            "Sex" to profile.sex.label,
            "Age" to "${GoalCalculator.age(profile.birthDate)}",
            "Height" to "${profile.heightCm.formatAmount()} cm",
            "Weight" to "${profile.weightKg.formatAmount()} kg",
            "Activity" to profile.activityLevel.title,
            "BMR" to "${GoalCalculator.bmr(profile).formatWhole()} kcal",
            "Maintenance" to "${GoalCalculator.tdee(profile).formatWhole()} kcal",
        ),
        onClick,
    )
}

@Composable
private fun GoalsCard(goals: Goals?, profile: Profile?, onClick: () -> Unit) {
    if (goals == null) {
        SummaryCard("Daily goals", listOf("Not set" to ""), onClick)
        return
    }
    val method = when (goals.mode) {
        GoalMode.AUTO -> "Calculated: protein ${goals.proteinPerKg.formatAmount()} g/kg, fat ${(goals.fatShare * 100).formatAmount()}%"
        GoalMode.MANUAL -> "Set manually"
    }
    val vsMaintenance = profile?.let {
        val diff = goals.kcal - GoalCalculator.tdee(it)
        if (diff < 0) "${(-diff).formatWhole()} below maintenance" else "${diff.formatWhole()} above maintenance"
    }
    SummaryCard(
        "Daily goals",
        listOfNotNull(
            "Calories" to "${goals.kcal.formatWhole()} kcal",
            vsMaintenance?.let { "" to it },
            "Protein" to "${goals.protein.formatWhole()} g",
            "Carbs" to "${goals.carbs.formatWhole()} g",
            "Fat" to "${goals.fat.formatWhole()} g",
            "Method" to method,
        ),
        onClick,
    )
}

@Composable
private fun BackupSection(vm: SettingsViewModel) {
    var pendingRestore by remember { mutableStateOf<Uri?>(null) }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        uri?.let(vm::exportBackup)
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        pendingRestore = uri
    }
    val excelLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument(XLSX_MIME)) { uri ->
        uri?.let(vm::exportExcel)
    }

    Text("Your data", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
    Text(
        "Save everything to a file (e.g. on Google Drive) to move it to a new phone.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { exportLauncher.launch("caltracker-backup-${LocalDate.now()}.json") }, modifier = Modifier.weight(1f)) {
            Text("Export backup")
        }
        OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json", "application/octet-stream")) }, modifier = Modifier.weight(1f)) {
            Text("Restore")
        }
    }
    Text(
        "Export your diary as a spreadsheet: every entry, daily totals, foods, recipes and weigh-ins.",
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    OutlinedButton(onClick = { excelLauncher.launch("caltracker-${LocalDate.now()}.xlsx") }, modifier = Modifier.fillMaxWidth()) {
        Text("Export to Excel")
    }

    pendingRestore?.let { uri ->
        AlertDialog(
            onDismissRequest = { pendingRestore = null },
            title = { Text("Restore backup?") },
            text = { Text("This replaces ALL data in the app (diary, foods, goals) with the contents of the backup.") },
            confirmButton = {
                TextButton(onClick = {
                    vm.importBackup(uri)
                    pendingRestore = null
                }) { Text("Replace everything") }
            },
            dismissButton = { TextButton(onClick = { pendingRestore = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun DeleteDataSection(onDelete: () -> Unit) {
    var confirming by remember { mutableStateOf(false) }
    Spacer(Modifier.height(16.dp))
    OutlinedButton(
        onClick = { confirming = true },
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error),
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Delete all data") }

    if (confirming) {
        AlertDialog(
            onDismissRequest = { confirming = false },
            title = { Text("Delete all data?") },
            text = {
                Text(
                    "This permanently deletes your diary, foods, recipes, weigh-ins, profile and goals. " +
                        "It can't be undone.\n\nTo keep a copy, cancel and use Export backup first.",
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirming = false
                        onDelete()
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                ) { Text("Delete everything") }
            },
            dismissButton = { TextButton(onClick = { confirming = false }) { Text("Cancel") } },
        )
    }
}

private fun groupLabel(minutes: Int) = if (minutes == 0) "Off (same minute only)" else "$minutes min"

@Composable
private fun GroupMinutesDialog(current: Int, onDismiss: () -> Unit, onSelect: (Int) -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Group diary entries") },
        text = {
            Column {
                Text(
                    "Entries logged within this time of a group's first entry are shown together with their total.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 8.dp),
                )
                PreferencesRepository.GROUP_MINUTE_OPTIONS.forEach { minutes ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .selectable(selected = minutes == current, onClick = { onSelect(minutes) }, role = Role.RadioButton)
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = minutes == current, onClick = null)
                        Text(groupLabel(minutes), modifier = Modifier.padding(start = 12.dp))
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } },
    )
}
