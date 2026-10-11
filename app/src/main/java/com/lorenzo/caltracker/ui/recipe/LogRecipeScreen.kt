package com.lorenzo.caltracker.ui.recipe

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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lorenzo.caltracker.data.db.entity.EntryUnit
import com.lorenzo.caltracker.domain.nutrition
import com.lorenzo.caltracker.ui.components.DecimalField
import com.lorenzo.caltracker.ui.components.TimeField
import com.lorenzo.caltracker.ui.formatAmount
import com.lorenzo.caltracker.ui.formatWhole
import com.lorenzo.caltracker.ui.toDecimalOrNull

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogRecipeScreen(viewModel: LogRecipeViewModel, onBack: () -> Unit, onSaved: () -> Unit, onEdit: (Long) -> Unit) {
    val vm = viewModel
    val recipe = vm.recipe
    var confirmDelete by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(recipe?.recipe?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    if (recipe != null) {
                        IconButton(onClick = { onEdit(recipe.recipe.id) }) { Icon(Icons.Filled.Edit, contentDescription = "Edit recipe") }
                        IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, contentDescription = "Delete recipe") }
                    }
                },
            )
        },
    ) { padding ->
        if (recipe == null) return@Scaffold
        val n = recipe.nutrition
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                "${n.servings.formatAmount()} ${if (n.servings == 1.0) "portion" else "portions"} of ${n.gramsPerServing.formatWhole()} g · " +
                    "${n.perServing.kcal.formatWhole()} kcal per portion",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                recipe.ingredients.sortedBy { it.ingredient.position }.joinToString(", ") { it.food.name },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = vm.unit == EntryUnit.SERVING, onClick = { vm.unit = EntryUnit.SERVING }, label = { Text("portions") })
                FilterChip(selected = vm.unit == EntryUnit.G, onClick = { vm.unit = EntryUnit.G }, label = { Text("grams") })
            }
            DecimalField(
                value = vm.amount,
                onValueChange = { vm.amount = it },
                label = "Amount",
                suffix = if (vm.unit == EntryUnit.G) "g" else if (vm.amount.toDecimalOrNull() == 1.0) "portion" else "portions",
                isError = vm.amount.toDecimalOrNull()?.let { it <= 0 } ?: true,
                modifier = Modifier.fillMaxWidth(),
            )
            TimeField(vm.time, { vm.time = it }, Modifier.fillMaxWidth())
            vm.preview?.let { p ->
                Card(Modifier.fillMaxWidth()) {
                    Text(
                        "${p.kcal.formatWhole()} kcal · P ${p.protein.formatAmount()} g · C ${p.carbs.formatAmount()} g · F ${p.fat.formatAmount()} g",
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
            Button(onClick = { vm.save(onSaved) }, enabled = vm.preview != null, modifier = Modifier.fillMaxWidth()) {
                Text("Add to diary")
            }
        }
    }

    if (confirmDelete && recipe != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete ${recipe.recipe.name}?") },
            text = { Text("The recipe will be removed. Its foods and the entries already in your diary are kept.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    vm.delete(onBack)
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}
