package com.lorenzo.caltracker.ui.logfood

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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
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
import com.lorenzo.caltracker.data.db.entity.EntryUnit
import com.lorenzo.caltracker.data.db.entity.FoodSource
import com.lorenzo.caltracker.domain.Nutrients
import com.lorenzo.caltracker.ui.components.DecimalField
import com.lorenzo.caltracker.ui.components.TimeField
import com.lorenzo.caltracker.ui.formatAmount
import com.lorenzo.caltracker.ui.formatWhole
import com.lorenzo.caltracker.ui.toDecimalOrNull

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogFoodScreen(viewModel: LogFoodViewModel, onBack: () -> Unit, onSaved: () -> Unit, onEdit: (Long) -> Unit) {
    val food = viewModel.food
    var confirmDelete by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(viewModel.message) {
        viewModel.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.message = null
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(food?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    if (food != null) {
                        IconButton(onClick = { onEdit(food.id) }) { Icon(Icons.Filled.Edit, contentDescription = "Edit food") }
                        IconButton(onClick = { confirmDelete = true }) { Icon(Icons.Filled.Delete, contentDescription = "Delete food") }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (food == null) return@Scaffold
        val preview = viewModel.preview
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            val origin = listOfNotNull(
                food.brand,
                when (food.source) {
                    FoodSource.OPEN_FOOD_FACTS -> "data: Open Food Facts"
                    FoodSource.USDA -> "data: USDA FoodData Central"
                    FoodSource.CUSTOM -> null
                },
            )
            if (origin.isNotEmpty()) Text(origin.joinToString(" · "), color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                viewModel.unitOptions.forEach { option ->
                    FilterChip(
                        selected = viewModel.unit == option,
                        onClick = { viewModel.unit = option },
                        label = {
                            Text(
                                when (option) {
                                    EntryUnit.G -> "grams"
                                    EntryUnit.ML -> "ml"
                                    EntryUnit.SERVING -> "${food.servingName ?: "serving"} (${food.servingSize?.formatAmount()} ${if (food.baseUnit == BaseUnit.ML) "ml" else "g"})"
                                    EntryUnit.QUICK -> ""
                                },
                            )
                        },
                    )
                }
            }
            DecimalField(
                value = viewModel.amount,
                onValueChange = { viewModel.amount = it },
                label = "Amount",
                suffix = when (viewModel.unit) {
                    EntryUnit.G -> "g"
                    EntryUnit.ML -> "ml"
                    EntryUnit.SERVING -> "× ${food.servingName ?: "serving"}"
                    EntryUnit.QUICK -> null
                },
                isError = viewModel.amount.toDecimalOrNull()?.let { it <= 0 } ?: true,
                modifier = Modifier.fillMaxWidth(),
            )
            TimeField(viewModel.time, { viewModel.time = it }, Modifier.fillMaxWidth())
            if (preview != null) NutrientsCard(preview)
            Button(
                onClick = { viewModel.save(onSaved) },
                enabled = preview != null,
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Add to diary") }
        }
    }

    if (confirmDelete && food != null) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("Delete ${food.name}?") },
            text = { Text("It will be removed from your saved foods. Entries already in your diary are kept.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.delete(onBack)
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun NutrientsCard(n: Nutrients) {
    Card(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Stat("kcal", n.kcal.formatWhole())
            Stat("Protein", "${n.protein.formatAmount()} g")
            Stat("Carbs", "${n.carbs.formatAmount()} g")
            Stat("Fat", "${n.fat.formatAmount()} g")
        }
    }
}

@Composable
private fun Stat(label: String, value: String) {
    Column {
        Text(value, style = MaterialTheme.typography.titleMedium)
        Text(label, style = MaterialTheme.typography.labelMedium)
    }
}
