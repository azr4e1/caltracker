package com.lorenzo.caltracker.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.lorenzo.caltracker.ui.formatWhole

/**
 * Progress towards [goal]: what's already logged ([base], solid) plus what's about to be added
 * ([added], lighter). Turns the error colour when the total goes over the goal.
 */
@Composable
fun StackedProgress(label: String, base: Double, added: Double, goal: Double, unit: String, color: Color, modifier: Modifier = Modifier) {
    val total = base + added
    val over = goal > 0 && total > goal
    val barColor = if (over) MaterialTheme.colorScheme.error else color
    val track = ProgressIndicatorDefaults.linearTrackColor
    val surface = MaterialTheme.colorScheme.surfaceContainerHighest
    Column(modifier, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium)
        Canvas(Modifier.fillMaxWidth().height(8.dp)) {
            val r = CornerRadius(size.height / 2)
            drawRoundRect(track, cornerRadius = r)
            if (goal <= 0) return@Canvas
            val baseW = (base / goal).toFloat().coerceIn(0f, 1f) * size.width
            val totalW = (total / goal).toFloat().coerceIn(0f, 1f) * size.width
            if (totalW > 0) drawRoundRect(barColor.copy(alpha = 0.45f), size = Size(totalW, size.height), cornerRadius = r)
            if (baseW > 0) drawRoundRect(barColor, size = Size(baseW, size.height), cornerRadius = r)
            // Surface gap where logged meets added.
            if (baseW > 0 && totalW > baseW + 2.dp.toPx()) {
                drawLine(surface, Offset(baseW, 0f), Offset(baseW, size.height), strokeWidth = 2.dp.toPx())
            }
        }
        Text(
            "+${added.formatWhole()} $unit",
            style = MaterialTheme.typography.bodySmall,
            color = if (over) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
