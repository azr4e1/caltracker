package com.lorenzo.caltracker.ui.addfood

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.lorenzo.caltracker.data.db.entity.BaseUnit
import com.lorenzo.caltracker.data.db.entity.Food
import com.lorenzo.caltracker.ui.formatWhole

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddFoodScreen(
    onBack: () -> Unit,
    onFoodSelected: (Long) -> Unit,
    onQuickAdd: () -> Unit,
    onNewFood: () -> Unit,
    viewModel: AddFoodViewModel = viewModel(factory = AddFoodViewModel.Factory),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add food") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding).imePadding()) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                placeholder = { Text("Search your foods") },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
            )
            Row(
                Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilledTonalButton(onClick = onQuickAdd, modifier = Modifier.weight(1f)) { Text("Quick add") }
                FilledTonalButton(onClick = onNewFood, modifier = Modifier.weight(1f)) { Text("New food") }
            }
            LazyColumn(Modifier.fillMaxSize()) {
                if (state.query.isNotBlank()) {
                    foodSection("Results", state.results, onFoodSelected, empty = "No saved food matches \"${state.query}\".")
                } else {
                    if (state.recent.isNotEmpty()) foodSection("Recent", state.recent, onFoodSelected)
                    foodSection("All foods", state.all, onFoodSelected, empty = "No saved foods yet. Create one with \"New food\".")
                }
            }
        }
    }
}

private fun LazyListScope.foodSection(title: String, foods: List<Food>, onClick: (Long) -> Unit, empty: String? = null) {
    item(key = "header-$title") {
        Text(
            title,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
    }
    if (foods.isEmpty() && empty != null) {
        item(key = "empty-$title") {
            Text(empty, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(horizontal = 16.dp))
        }
    }
    items(foods, key = { "$title-${it.id}" }) { food ->
        ListItem(
            headlineContent = { Text(food.name) },
            supportingContent = food.brand?.let { { Text(it) } },
            trailingContent = {
                Text("${food.kcalPer100.formatWhole()} kcal / 100 ${if (food.baseUnit == BaseUnit.ML) "ml" else "g"}")
            },
            modifier = Modifier.clickable { onClick(food.id) },
        )
    }
}
