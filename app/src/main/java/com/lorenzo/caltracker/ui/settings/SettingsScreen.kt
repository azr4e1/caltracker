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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lorenzo.caltracker.data.db.entity.ActivityLevel
import com.lorenzo.caltracker.data.db.entity.GoalMode
import com.lorenzo.caltracker.data.db.entity.Sex
import com.lorenzo.caltracker.domain.GoalCalculator
import com.lorenzo.caltracker.ui.components.DateSelectDialog
import com.lorenzo.caltracker.ui.components.DecimalField
import com.lorenzo.caltracker.ui.formatWhole
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel = viewModel(factory = SettingsViewModel.Factory)) {
    val vm = viewModel
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(vm.message) {
        vm.message?.let {
            snackbarHostState.showSnackbar(it)
            vm.message = null
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = { TopAppBar(title = { Text("Settings") }) },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            ProfileSection(vm)
            HorizontalDivider()
            GoalsSection(vm)
            HorizontalDivider()
            BackupSection(vm)
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(text, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
}

@Composable
private fun <T> Segmented(options: List<T>, selected: T, label: (T) -> String, onSelect: (T) -> Unit) {
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, option ->
            SegmentedButton(
                selected = option == selected,
                onClick = { onSelect(option) },
                shape = SegmentedButtonDefaults.itemShape(index, options.size),
            ) { Text(label(option)) }
        }
    }
}

@Composable
private fun ProfileSection(vm: SettingsViewModel) {
    var pickBirthDate by remember { mutableStateOf(false) }

    SectionTitle("Profile")
    Segmented(Sex.entries, vm.sex, { if (it == Sex.MALE) "Male" else "Female" }) { vm.sex = it }
    OutlinedButton(onClick = { pickBirthDate = true }, modifier = Modifier.fillMaxWidth()) {
        val birth = vm.birthDate
        Text(
            if (birth == null) {
                "Set birth date"
            } else {
                "Born ${birth.format(DateTimeFormatter.ofPattern("d MMM yyyy"))} (age ${GoalCalculator.age(birth)})"
            },
        )
    }
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        DecimalField(vm.height, { vm.height = it }, "Height", suffix = "cm", modifier = Modifier.weight(1f))
        DecimalField(vm.weight, { vm.weight = it }, "Weight", suffix = "kg", modifier = Modifier.weight(1f))
    }
    Text("Activity level", style = MaterialTheme.typography.titleSmall)
    Column {
        ActivityLevel.entries.forEach { level ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .selectable(selected = vm.activity == level, onClick = { vm.activity = level }, role = Role.RadioButton)
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = vm.activity == level, onClick = null)
                Column(Modifier.padding(start = 12.dp)) {
                    Text(level.title)
                    Text(level.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
    val bmr = vm.bmr
    val maintenance = vm.maintenanceKcal
    if (bmr != null && maintenance != null) {
        Text(
            "BMR ${bmr.formatWhole()} kcal · Maintenance ${maintenance.formatWhole()} kcal/day",
            style = MaterialTheme.typography.bodyMedium,
        )
    }

    if (pickBirthDate) {
        DateSelectDialog(
            initial = vm.birthDate ?: LocalDate.now().minusYears(30),
            onDismiss = { pickBirthDate = false },
            onConfirm = {
                vm.birthDate = it
                pickBirthDate = false
            },
        )
    }
}

private val ActivityLevel.title: String
    get() = when (this) {
        ActivityLevel.SEDENTARY -> "Sedentary"
        ActivityLevel.LIGHT -> "Lightly active"
        ActivityLevel.MODERATE -> "Moderately active"
        ActivityLevel.ACTIVE -> "Active"
        ActivityLevel.VERY_ACTIVE -> "Very active"
    }

private val ActivityLevel.description: String
    get() = when (this) {
        ActivityLevel.SEDENTARY -> "Desk job, little exercise"
        ActivityLevel.LIGHT -> "Exercise 1–3 days a week"
        ActivityLevel.MODERATE -> "Exercise 3–5 days a week"
        ActivityLevel.ACTIVE -> "Hard exercise 6–7 days a week"
        ActivityLevel.VERY_ACTIVE -> "Physical job plus training"
    }

@Composable
private fun GoalsSection(vm: SettingsViewModel) {
    SectionTitle("Daily goals")
    Segmented(GoalMode.entries, vm.mode, { if (it == GoalMode.AUTO) "Calculate macros" else "Set manually" }, vm::setGoalMode)

    DecimalField(vm.kcal, { vm.kcal = it }, "Calories", suffix = "kcal", modifier = Modifier.fillMaxWidth())
    vm.maintenanceKcal?.let { maintenance ->
        TextButton(onClick = vm::useMaintenance) { Text("Use maintenance (${maintenance.formatWhole()} kcal)") }
    }

    when (vm.mode) {
        GoalMode.AUTO -> {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DecimalField(vm.proteinPerKg, { vm.proteinPerKg = it }, "Protein", suffix = "g/kg", modifier = Modifier.weight(1f))
                DecimalField(vm.fatPercent, { vm.fatPercent = it }, "Fat", suffix = "% kcal", modifier = Modifier.weight(1f))
            }
            if (vm.profile == null) {
                Text(
                    "Fill in your profile above to calculate macros.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        GoalMode.MANUAL -> {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DecimalField(vm.protein, { vm.protein = it }, "Protein", suffix = "g", modifier = Modifier.weight(1f))
                DecimalField(vm.carbs, { vm.carbs = it }, "Carbs", suffix = "g", modifier = Modifier.weight(1f))
                DecimalField(vm.fat, { vm.fat = it }, "Fat", suffix = "g", modifier = Modifier.weight(1f))
            }
        }
    }

    vm.targets?.let { t ->
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    "${t.kcal.formatWhole()} kcal · P ${t.protein.formatWhole()} g · C ${t.carbs.formatWhole()} g · F ${t.fat.formatWhole()} g",
                    style = MaterialTheme.typography.titleSmall,
                )
                val fromMacros = GoalCalculator.kcalFromMacros(t.protein, t.carbs, t.fat)
                if (vm.mode == GoalMode.MANUAL) {
                    Text("Macros add up to ${fromMacros.formatWhole()} kcal", style = MaterialTheme.typography.bodySmall)
                } else if (fromMacros > t.kcal + 1) {
                    Text(
                        "Protein and fat alone exceed your calories, so carbs are 0. Lower protein or fat.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }

    Button(onClick = vm::save, enabled = vm.targets != null, modifier = Modifier.fillMaxWidth()) { Text("Save profile & goals") }
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

    SectionTitle("Backup")
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
