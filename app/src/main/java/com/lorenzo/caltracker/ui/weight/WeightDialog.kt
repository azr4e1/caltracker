package com.lorenzo.caltracker.ui.weight

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lorenzo.caltracker.data.db.entity.WeightEntry
import com.lorenzo.caltracker.ui.components.DateSelectDialog
import com.lorenzo.caltracker.ui.components.DecimalField
import com.lorenzo.caltracker.ui.diary.dateLabel
import com.lorenzo.caltracker.ui.formatAmount
import com.lorenzo.caltracker.ui.toDecimalOrNull
import java.time.LocalDate

/**
 * Logs a new weigh-in ([existing] null, on [initialDate]) or edits one. [onDelete] null hides
 * the delete button.
 */
@Composable
fun WeightDialog(
    existing: WeightEntry?,
    suggestedKg: Double?,
    onDismiss: () -> Unit,
    onSave: (LocalDate, Double) -> Unit,
    onDelete: (() -> Unit)?,
    initialDate: LocalDate = existing?.date ?: LocalDate.now(),
) {
    var date by remember { mutableStateOf(initialDate) }
    var weight by remember { mutableStateOf((existing?.weightKg ?: suggestedKg)?.formatAmount().orEmpty()) }
    var pickDate by remember { mutableStateOf(false) }
    val kg = weight.toDecimalOrNull()?.takeIf { it in 20.0..400.0 }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Log weight" else "Edit weigh-in") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                OutlinedButton(onClick = { pickDate = true }, modifier = Modifier.fillMaxWidth()) { Text(dateLabel(date)) }
                DecimalField(weight, { weight = it }, "Weight", suffix = "kg", isError = weight.isNotBlank() && kg == null, modifier = Modifier.fillMaxWidth())
                Text(
                    "Your latest weigh-in also becomes your profile weight, and calculated macros update with it.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (existing != null && onDelete != null) {
                    TextButton(
                        onClick = onDelete,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) { Text("Delete weigh-in") }
                }
            }
        },
        confirmButton = { TextButton(onClick = { onSave(date, kg!!) }, enabled = kg != null) { Text("Save") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )

    if (pickDate) {
        DateSelectDialog(
            initial = date,
            onDismiss = { pickDate = false },
            onConfirm = {
                date = it
                pickDate = false
            },
        )
    }
}
