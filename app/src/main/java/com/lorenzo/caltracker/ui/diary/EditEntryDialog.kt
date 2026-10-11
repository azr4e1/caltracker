package com.lorenzo.caltracker.ui.diary

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
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
import com.lorenzo.caltracker.data.db.entity.LogEntry
import com.lorenzo.caltracker.domain.Nutrients
import com.lorenzo.caltracker.ui.components.DecimalField
import com.lorenzo.caltracker.ui.components.TimeField
import com.lorenzo.caltracker.ui.formatAmount
import com.lorenzo.caltracker.ui.formatWhole
import com.lorenzo.caltracker.ui.label
import com.lorenzo.caltracker.ui.toDecimalOrNull
import java.time.LocalTime

/**
 * Edits a logged entry. Food entries edit the amount (nutrients scale with it);
 * quick adds edit their values directly.
 */
@Composable
fun EditEntryDialog(
    entry: LogEntry,
    onDismiss: () -> Unit,
    onSaveAmount: (Double, LocalTime) -> Unit,
    onSaveQuickAdd: (String, Nutrients, LocalTime) -> Unit,
    onDuplicate: () -> Unit,
    onCopyTo: () -> Unit,
    onMoveTo: () -> Unit,
    onDelete: () -> Unit,
) {
    var time by remember { mutableStateOf(entry.time) }
    var name by remember { mutableStateOf(entry.name) }
    var amount by remember { mutableStateOf(entry.amount.formatAmount()) }
    var kcal by remember { mutableStateOf(entry.kcal.formatAmount()) }
    var protein by remember { mutableStateOf(entry.protein.formatAmount()) }
    var carbs by remember { mutableStateOf(entry.carbs.formatAmount()) }
    var fat by remember { mutableStateOf(entry.fat.formatAmount()) }

    val parsedAmount = amount.toDecimalOrNull()
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
                    DecimalField(
                        value = amount,
                        onValueChange = { amount = it },
                        label = "Amount",
                        suffix = entry.unit.label(parsedAmount ?: entry.amount),
                        isError = parsedAmount == null || parsedAmount <= 0,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    if (parsedAmount != null && parsedAmount > 0 && entry.amount > 0) {
                        val scaled = (parsedAmount / entry.amount)
                        Text(
                            "${(entry.kcal * scaled).formatWhole()} kcal · P ${(entry.protein * scaled).formatWhole()}" +
                                " · C ${(entry.carbs * scaled).formatWhole()} · F ${(entry.fat * scaled).formatWhole()}",
                        )
                    }
                }
                TimeField(time, { time = it }, Modifier.fillMaxWidth())
                Column {
                    TextButton(onClick = onDuplicate) { Text("Duplicate") }
                    TextButton(onClick = onCopyTo) { Text("Copy to another day…") }
                    TextButton(onClick = onMoveTo) { Text("Move to another day…") }
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) { Text("Delete entry") }
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
                        onSaveAmount(parsedAmount!!, time)
                    }
                },
            ) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
