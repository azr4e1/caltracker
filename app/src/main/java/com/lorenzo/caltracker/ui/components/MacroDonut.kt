package com.lorenzo.caltracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.lorenzo.caltracker.domain.Nutrients
import com.lorenzo.caltracker.domain.Nutrients.Companion.KCAL_PER_G_CARBS
import com.lorenzo.caltracker.domain.Nutrients.Companion.KCAL_PER_G_FAT
import com.lorenzo.caltracker.domain.Nutrients.Companion.KCAL_PER_G_PROTEIN
import com.lorenzo.caltracker.ui.formatWhole
import com.lorenzo.caltracker.ui.theme.MacroColors
import kotlin.math.roundToInt

/**
 * Donut of where the calories come from (protein / carbs / fat, by kcal), total in the middle,
 * and a legend with grams and share of each macro.
 */
@Composable
fun MacroDonut(n: Nutrients, colors: MacroColors, modifier: Modifier = Modifier) {
    val parts = listOf(
        Triple("Protein", n.protein, colors.protein) to n.protein * KCAL_PER_G_PROTEIN,
        Triple("Carbs", n.carbs, colors.carbs) to n.carbs * KCAL_PER_G_CARBS,
        Triple("Fat", n.fat, colors.fat) to n.fat * KCAL_PER_G_FAT,
    )
    val macroKcal = parts.sumOf { it.second }
    val track = MaterialTheme.colorScheme.surfaceVariant

    Row(modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(24.dp)) {
        Box(contentAlignment = Alignment.Center) {
            Canvas(
                Modifier
                    .size(140.dp)
                    .semantics {
                        contentDescription = parts.joinToString(", ") { (m, kcal) ->
                            "${m.first} ${share(kcal, macroKcal)} percent"
                        }
                    },
            ) {
                val stroke = 18.dp.toPx()
                val inset = stroke / 2
                val arcSize = Size(size.width - stroke, size.height - stroke)
                val topLeft = Offset(inset, inset)
                if (macroKcal <= 0) {
                    drawArc(track, 0f, 360f, useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(stroke))
                    return@Canvas
                }
                // A thin surface-coloured gap separates the segments.
                val gapDegrees = 2.dp.toPx() / (arcSize.width / 2) * (180f / Math.PI.toFloat())
                var start = -90f
                parts.filter { it.second > 0 }.let { visible ->
                    visible.forEach { (m, kcal) ->
                        val sweep = (kcal / macroKcal * 360).toFloat()
                        val gap = if (visible.size > 1) gapDegrees else 0f
                        drawArc(m.third, start + gap / 2, (sweep - gap).coerceAtLeast(0.5f), useCenter = false, topLeft = topLeft, size = arcSize, style = Stroke(stroke))
                        start += sweep
                    }
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(n.kcal.formatWhole(), style = MaterialTheme.typography.headlineSmall)
                Text("kcal", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            parts.forEach { (m, kcal) ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Canvas(Modifier.size(10.dp)) { drawRoundRect(m.third, cornerRadius = CornerRadius(2.dp.toPx())) }
                    Column {
                        Text(m.first, style = MaterialTheme.typography.labelLarge)
                        Text(
                            "${m.second.formatWhole()} g · ${share(kcal, macroKcal)}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

private fun share(part: Double, total: Double): Int = if (total > 0) (part / total * 100).roundToInt() else 0
