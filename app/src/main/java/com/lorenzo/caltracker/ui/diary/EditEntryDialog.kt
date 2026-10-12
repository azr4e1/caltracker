package com.lorenzo.caltracker.ui.diary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lorenzo.caltracker.data.db.entity.EntryUnit
import com.lorenzo.caltracker.data.db.entity.LogEntry
import com.lorenzo.caltracker.domain.Nutrients
import com.lorenzo.caltracker.domain.UnitChoices
import com.lorenzo.caltracker.domain.inUnit
import com.lorenzo.caltracker.domain.withAmount
import com.lorenzo.caltracker.ui.components.DecimalField
import com.lorenzo.caltracker.ui.components.TimeField
import com.lorenzo.caltracker.ui.formatAmount
import com.lorenzo.caltracker.ui.formatWhole
import com.lorenzo.caltracker.ui.unitLabel
import com.lorenzo.caltracker.ui.toDecimalOrNull
import java.time.LocalTime

/**
 * Edits a logged (or about-to-be-logged) entry. Actions passed as null are hidden. Food entries edit the amount (nutrients scale with it)
 * and, when [unitChoices] offers a serving, can switch between g/ml and servings; quick adds edit their values directly.
 */
@Composable
fun EditEntryDialog(
    entry: LogEntry,
    unitChoices: UnitChoices?,
    onDismiss: () -> Unit,
    onSaveEntry: (LogEntry) -> Unit,
    onSaveQuickAdd: (String, Nutrients, LocalTime) -> Unit,
    onDuplicate: (() -> Unit)?,
    onCopyTo: (() -> Unit)?,
    onMoveTo: (() -> Unit)?,
    onSplit: (() -> Unit)?,
    onDelete: () -> Unit,
    deleteLabel: String = "Delete entry",
) {
    var time by remember { mutableStateOf(entry.time) }
    var name by remember { mutableStateOf(entry.name) }
    // The entry in the chosen unit, with its original nutrients; the amount field scales from it.
    var base by remember { mutableStateOf(entry) }
    var amount by remember { mutableStateOf(entry.amount.formatAmount()) }
    var kcal by remember { mutableStateOf(entry.kcal.formatAmount()) }
    var protein by remember { mutableStateOf(entry.protein.formatAmount()) }
    var carbs by remember { mutableStateOf(entry.carbs.formatAmount()) }
    var fat by remember { mutableStateOf(entry.fat.formatAmount()) }

    // Untouched text keeps the exact converted amount, so switching units and back doesn't drift.
    val parsedAmount = if (amount == base.amount.formatAmount()) base.amount else amount.toDecimalOrNull()
    val quickAddValues = listOf(kcal, protein, carbs, fat).map { it.ifBlank { "0" }.toDecimalOrNull() }
    val canSave = if (entry.isQuickAdd) {
        name.isNotBlank() && quickAddValues.all { it != null && it >= 0 }
    } else {
        parsedAmount != null && parsedAmount > 0
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (entry.isQuickAdd) "Edit quick add" else entry.name) },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                if (entry.isQuickAdd) {
                    OutlinedTextField(name, { name = it }, label = { Text("Name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    DecimalField(kcal, { kcal = it }, "Calories", suffix = "kcal", modifier = Modifier.fillMaxWidth())
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DecimalField(protein, { protein = it }, "Protein", suffix = "g", modifier = Modifier.weight(1f))
                        DecimalField(carbs, { carbs = it }, "Carbs", suffix = "g", modifier = Modifier.weight(1f))
                        DecimalField(fat, { fat = it }, "Fat", suffix = "g", modifier = Modifier.weight(1f))
                    }
                } else {
                    val units = unitChoices?.units.orEmpty()
                    if (unitChoices != null && units.size > 1 && entry.unit in units) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            units.forEach { option ->
                                FilterChip(
                                    selected = base.unit == option,
                                    onClick = {
                                        if (base.unit != option) {
                                            // Carry the typed amount over, e.g. 2 servings of 30 g become 60 g.
                                            val current = parsedAmount?.takeIf { it > 0 }?.let { base.withAmount(it) } ?: base
                                            base = current.inUnit(option, unitChoices)
                                            amount = base.amount.formatAmount()
                                        }
                                    },
                                    label = { Text(unitChoices.chipLabel(option)) },
                                )
                            }
                        }
                    }
                    DecimalField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = "Amount",
                        suffix = base.unitLabel(parsedAmount ?: base.amount),
                        isError = parsedAmount == null || parsedAmount <= 0,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (parsedAmount != null && parsedAmount > 0 && base.amount > 0) {
                        val e = base.withAmount(parsedAmount)
                        Text("${e.kcal.formatWhole()} kcal · P ${e.protein.formatWhole()} · C ${e.carbs.formatWhole()} · F ${e.fat.formatWhole()}")
                    }
                }
                TimeField(time, { time = it }, Modifier.fillMaxWidth())
                Column {
                    onDuplicate?.let { TextButton(onClick = it) { Text("Duplicate") } }
                    onCopyTo?.let { TextButton(onClick = it) { Text("Copy to another day…") } }
                    onMoveTo?.let { TextButton(onClick = it) { Text("Move to another day…") } }
                    if (entry.recipeId != null) onSplit?.let { TextButton(onClick = it) { Text("Split into ingredients") } }
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) { Text(deleteLabel) }
                }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = {
                    if (entry.isQuickAdd) {
                        val (k, p, c, f) = quickAddValues.map { it ?: 0.0 }
                        onSaveQuickAdd(name.trim(), Nutrients(k, p, c, f), time)
                    } else {
                        onSaveEntry(base.withAmount(parsedAmount!!).copy(time = time))
                    }
                },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}

private fun UnitChoices.chipLabel(unit: EntryUnit): String = when (unit) {
    EntryUnit.G -> "grams"
    EntryUnit.ML -> "ml"
    EntryUnit.SERVING -> "$servingLabel (${size?.formatAmount()} ${if (baseUnit == EntryUnit.ML) "ml" else "g"})"
    EntryUnit.QUICK -> ""
}
