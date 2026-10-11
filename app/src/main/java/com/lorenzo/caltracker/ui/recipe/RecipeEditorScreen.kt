package com.lorenzo.caltracker.ui.recipe

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import com.lorenzo.caltracker.data.db.entity.BaseUnit
import com.lorenzo.caltracker.domain.Nutrients
import com.lorenzo.caltracker.domain.nutrientsFor
import com.lorenzo.caltracker.ui.components.DecimalField
import com.lorenzo.caltracker.ui.formatAmount
import com.lorenzo.caltracker.ui.formatWhole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeEditorScreen(
    viewModel: RecipeEditorViewModel,
    logAfterSave: Boolean,
    onAddIngredient: () -> Unit,
    onBack: () -> Unit,
    onSaved: (Long) -> Unit,
) {
    val vm = viewModel
    var confirmDiscard by remember { mutableStateOf(false) }
    val onBackPressed = { if (vm.hasUnsavedChanges) confirmDiscard = true else onBack() }
    BackHandler(enabled = vm.hasUnsavedChanges) { confirmDiscard = true }
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(vm.message) {
        vm.message?.let {
            snackbarHostState.showSnackbar(it)
            vm.message = null
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text(if (vm.isEditing) "Edit recipe" else "New recipe") },
                navigationIcon = {
                    IconButton(onClick = onBackPressed) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = { TextButton(onClick = { vm.save(onSaved) }, enabled = vm.isValid) { Text("Save") } },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!logAfterSave && !vm.isEditing) {
                Text(
                    "When you save, the selected diary entries are replaced by this recipe.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (vm.isEditing) {
                Text(
                    "Changes apply to future entries. Entries already in your diary keep their values.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedTextField(vm.name, { vm.name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DecimalField(vm.servings, { vm.servings = it }, "Portions", modifier = Modifier.weight(1f))
                DecimalField(
                    vm.cookedWeight, { vm.cookedWeight = it }, "Cooked weight", suffix = "g",
                    isError = vm.isCookedWeightInvalid,
                    modifier = Modifier.weight(1f),
                )
            }
            Text(
                "Optional: weigh the finished dish so you can log it by grams. " +
                    "Without it, the ingredients' total (${vm.ingredientsWeight.formatWhole()} g) is used.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text("Ingredients", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary)
            if (vm.ingredients.isEmpty()) {
                Text("No ingredients yet.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Column {
                vm.ingredients.forEachIndexed { index, row ->
                    val unit = if (row.food.baseUnit == BaseUnit.ML) "ml" else "g"
                    ListItem(
                        headlineContent = { Text(row.food.name) },
                        supportingContent = { Text("${row.amount.formatAmount()} $unit") },
                        trailingContent = { Text("${row.food.nutrientsFor(row.amount).kcal.formatWhole()} kcal") },
                        modifier = Modifier.clickable { vm.editIngredient(index) },
                    )
                }
            }
            OutlinedButton(onClick = onAddIngredient, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Filled.Add, contentDescription = null)
                Text("  Add ingredient")
            }

            if (vm.ingredients.isNotEmpty()) {
                val n = vm.nutrition
                NutritionCard("Whole recipe · ${n.totalWeight.formatWhole()} g", n.total)
                NutritionCard("Per portion · ${n.gramsPerServing.formatWhole()} g", n.perServing)
            }

            Button(onClick = { vm.save(onSaved) }, enabled = vm.isValid, modifier = Modifier.fillMaxWidth()) {
                Text(if (vm.isEditing || !logAfterSave) "Save" else "Save and log")
            }
        }
    }

    vm.editing?.let { edit ->
        IngredientDialog(
            edit = edit,
            onDismiss = { vm.editing = null },
            onConfirm = vm::confirmIngredient,
            onRemove = vm::removeIngredient,
        )
    }

    if (confirmDiscard) {
        AlertDialog(
            onDismissRequest = { confirmDiscard = false },
            title = { Text("Discard changes?") },
            text = { Text("Your changes to this recipe haven't been saved.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDiscard = false
                    onBack()
                }) { Text("Discard") }
            },
            dismissButton = { TextButton(onClick = { confirmDiscard = false }) { Text("Keep editing") } },
        )
    }
}

@Composable
private fun NutritionCard(title: String, n: Nutrients) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                "${n.kcal.formatWhole()} kcal · P ${n.protein.formatWhole()} g · C ${n.carbs.formatWhole()} g · F ${n.fat.formatWhole()} g",
                style = MaterialTheme.typography.titleSmall,
            )
        }
    }
}
