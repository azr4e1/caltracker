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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lorenzo.caltracker.data.db.entity.BaseUnit
import com.lorenzo.caltracker.data.db.entity.EntryUnit
import com.lorenzo.caltracker.domain.Nutrients
import com.lorenzo.caltracker.ui.components.DecimalField
import com.lorenzo.caltracker.ui.components.TimeField
import com.lorenzo.caltracker.ui.formatAmount
import com.lorenzo.caltracker.ui.formatWhole
import com.lorenzo.caltracker.ui.toDecimalOrNull

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LogFoodScreen(viewModel: LogFoodViewModel, onBack: () -> Unit, onSaved: () -> Unit) {
    val food = viewModel.food
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(food?.name ?: "") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        if (food == null) return@Scaffold
        val preview = viewModel.preview
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            food.brand?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
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
