package com.lorenzo.caltracker.ui.foodeditor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.lorenzo.caltracker.data.db.entity.BaseUnit
import com.lorenzo.caltracker.ui.components.DecimalField
import com.lorenzo.caltracker.ui.formatWhole
import com.lorenzo.caltracker.ui.toDecimalOrNull
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodEditorScreen(viewModel: FoodEditorViewModel, onBack: () -> Unit, onSaved: (Long) -> Unit) {
    val vm = viewModel
    val unit = if (vm.baseUnit == BaseUnit.ML) "ml" else "g"
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
                title = { Text(if (vm.isEditing) "Edit food" else "New food") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
                actions = {
                    TextButton(onClick = { vm.save(onSaved) }, enabled = vm.isValid) { Text("Save") }
                },
            )
        },
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).imePadding().verticalScroll(rememberScrollState()).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (vm.isEditing) {
                Text(
                    "Changes apply to future entries. Entries already in your diary keep their values.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            OutlinedTextField(vm.name, { vm.name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            OutlinedTextField(vm.brand, { vm.brand = it }, label = { Text("Brand (optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())

            Text("Nutrition values are per", style = MaterialTheme.typography.titleSmall)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                BaseUnit.entries.forEachIndexed { index, option ->
                    SegmentedButton(
                        selected = vm.baseUnit == option,
                        onClick = { vm.baseUnit = option },
                        shape = SegmentedButtonDefaults.itemShape(index, BaseUnit.entries.size),
                    ) { Text(if (option == BaseUnit.ML) "100 ml" else "100 g") }
                }
            }

            val implied = vm.kcalFromMacros
            val kcalValue = vm.kcal.toDecimalOrNull()
            // Labels round values, so only flag clearly inconsistent numbers.
            val mismatch = implied != null && kcalValue != null && abs(implied - kcalValue) > maxOf(20.0, kcalValue * 0.15)
            DecimalField(
                vm.kcal, { vm.kcal = it }, "Calories", suffix = "kcal",
                isError = vm.isInvalidRequired(vm.kcal),
                supportingText = if (mismatch) "Macros add up to ${implied.formatWhole()} kcal. Check for typos." else null,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DecimalField(vm.protein, { vm.protein = it }, "Protein", suffix = "g", isError = vm.isInvalidRequired(vm.protein), modifier = Modifier.weight(1f))
                DecimalField(vm.carbs, { vm.carbs = it }, "Carbs", suffix = "g", isError = vm.isInvalidRequired(vm.carbs), modifier = Modifier.weight(1f))
                DecimalField(vm.fat, { vm.fat = it }, "Fat", suffix = "g", isError = vm.isInvalidRequired(vm.fat), modifier = Modifier.weight(1f))
            }

            Text("Optional", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DecimalField(vm.fiber, { vm.fiber = it }, "Fiber", suffix = "g", modifier = Modifier.weight(1f))
                DecimalField(vm.sugar, { vm.sugar = it }, "Sugar", suffix = "g", modifier = Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                DecimalField(vm.saturatedFat, { vm.saturatedFat = it }, "Sat. fat", suffix = "g", modifier = Modifier.weight(1f))
                DecimalField(vm.salt, { vm.salt = it }, "Salt", suffix = "g", modifier = Modifier.weight(1f))
            }

            Text("Serving (optional)", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    vm.servingName, { vm.servingName = it },
                    label = { Text("Name, e.g. slice") },
                    singleLine = true,
                    modifier = Modifier.weight(1f),
                )
                DecimalField(vm.servingSize, { vm.servingSize = it }, "Size", suffix = unit, modifier = Modifier.weight(1f))
            }

            OutlinedTextField(
                vm.barcode,
                { new -> if (new.all(Char::isDigit)) vm.barcode = new },
                label = { Text("Barcode (optional)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )

            Button(onClick = { vm.save(onSaved) }, enabled = vm.isValid, modifier = Modifier.fillMaxWidth()) {
                Text(if (vm.isEditing) "Save" else "Save and log")
            }
        }
    }
}
