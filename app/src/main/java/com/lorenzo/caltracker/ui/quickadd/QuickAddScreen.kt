package com.lorenzo.caltracker.ui.quickadd

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lorenzo.caltracker.ui.components.DecimalField
import com.lorenzo.caltracker.ui.components.TimeField

/** Logs calories (and optionally macros) without creating a food. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddScreen(viewModel: QuickAddViewModel, onBack: () -> Unit, onSaved: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Quick add") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = viewModel.name,
                onValueChange = { viewModel.name = it },
                label = { Text("Name (optional)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            DecimalField(viewModel.kcal, { viewModel.kcal = it }, "Calories", suffix = "kcal", modifier = Modifier.fillMaxWidth())
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DecimalField(viewModel.protein, { viewModel.protein = it }, "Protein", suffix = "g", modifier = Modifier.weight(1f))
                DecimalField(viewModel.carbs, { viewModel.carbs = it }, "Carbs", suffix = "g", modifier = Modifier.weight(1f))
                DecimalField(viewModel.fat, { viewModel.fat = it }, "Fat", suffix = "g", modifier = Modifier.weight(1f))
            }
            TimeField(viewModel.time, { viewModel.time = it }, Modifier.fillMaxWidth())
            Button(
                enabled = viewModel.isValid,
                onClick = { viewModel.save(onSaved) },
                modifier = Modifier.fillMaxWidth(),
            ) { Text("Add to diary") }
        }
    }
}
