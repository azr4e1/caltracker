package com.lorenzo.caltracker.ui.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lorenzo.caltracker.data.db.entity.GoalMode
import com.lorenzo.caltracker.data.db.entity.Goals
import com.lorenzo.caltracker.data.db.entity.Profile
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
            ProfileCard(state.profile)
            GoalsCard(state.goals, state.profile)
            FilledTonalButton(onClick = onEditProfile, modifier = Modifier.fillMaxWidth()) { Text("Edit profile & goals") }
            BackupSection(viewModel)
        }
    }
}

@Composable
private fun SummaryCard(title: String, rows: List<Pair<String, String>>) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
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
private fun ProfileCard(profile: Profile?) {
    if (profile == null) {
        SummaryCard("Profile", listOf("Not set" to ""))
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
    )
}

@Composable
private fun GoalsCard(goals: Goals?, profile: Profile?) {
    if (goals == null) {
        SummaryCard("Daily goals", listOf("Not set" to ""))
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
        "Export your diary as a spreadsheet: every entry, daily totals and your foods.",
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
