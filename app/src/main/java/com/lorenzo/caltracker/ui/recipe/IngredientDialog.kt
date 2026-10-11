package com.lorenzo.caltracker.ui.recipe

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.lorenzo.caltracker.data.db.entity.BaseUnit
import com.lorenzo.caltracker.domain.nutrientsFor
import com.lorenzo.caltracker.ui.components.DecimalField
import com.lorenzo.caltracker.ui.formatAmount
import com.lorenzo.caltracker.ui.formatWhole
import com.lorenzo.caltracker.ui.toDecimalOrNull

/** Asks how much of a food goes into the recipe, in g/ml or in the food's servings. */
@Composable
fun IngredientDialog(
    edit: IngredientEdit,
    onDismiss: () -> Unit,
    onConfirm: (amountInBaseUnit: Double) -> Unit,
    onRemove: () -> Unit,
) {
    val food = edit.food
    val baseLabel = if (food.baseUnit == BaseUnit.ML) "ml" else "g"
    val servingSize = food.servingSize?.takeIf { it > 0 }
    var inServings by remember { mutableStateOf(false) }
    var amount by remember { mutableStateOf(edit.amount.formatAmount()) }

    val value = amount.toDecimalOrNull()?.takeIf { it > 0 }
    val baseAmount = value?.let { if (inServings && servingSize != null) it * servingSize else it }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(food.name) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (servingSize != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        FilterChip(
                            selected = !inServings,
                            onClick = {
                                if (inServings) baseAmount?.let { amount = it.formatAmount() }
                                inServings = false
                            },
                            label = { Text(baseLabel) },
                        )
                        FilterChip(
                            selected = inServings,
                            onClick = {
                                if (!inServings) baseAmount?.let { amount = (it / servingSize).formatAmount() }
                                inServings = true
                            },
                            label = { Text("${food.servingName ?: "serving"} (${servingSize.formatAmount()} $baseLabel)") },
                        )
                    }
                }
                DecimalField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = "Amount",
                    suffix = if (inServings) "× ${food.servingName ?: "serving"}" else baseLabel,
                    isError = value == null,
                    modifier = Modifier.fillMaxWidth(),
                )
                baseAmount?.let {
                    val n = food.nutrientsFor(it)
                    Text("${n.kcal.formatWhole()} kcal · P ${n.protein.formatWhole()} · C ${n.carbs.formatWhole()} · F ${n.fat.formatWhole()}")
                }
                if (edit.index != null) {
                    TextButton(
                        onClick = onRemove,
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                    ) { Text("Remove from recipe") }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = baseAmount != null, onClick = { onConfirm(baseAmount!!) }) {
                Text(if (edit.index == null) "Add" else "Save")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
}
