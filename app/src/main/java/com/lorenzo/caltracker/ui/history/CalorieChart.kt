package com.lorenzo.caltracker.ui.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.lorenzo.caltracker.ui.formatWhole
import com.lorenzo.caltracker.ui.theme.MacroColors
import java.time.format.DateTimeFormatter
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

private val Y_AXIS_WIDTH = 36.dp
private val X_AXIS_HEIGHT = 22.dp
private val SEGMENT_GAP = 2.dp
private val CORNER = 4.dp
private val MAX_COLUMN_WIDTH = 28.dp

/**
 * Stacked column chart of calories per day, split into protein / carbs / fat / other,
 * with the calorie goal as a dashed line. Tapping a column selects it.
 */
@Composable
fun CalorieChart(
    days: List<DayColumn>,
    goalKcal: Double?,
    colors: MacroColors,
    selectedIndex: Int?,
    onSelect: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val goalColor = MaterialTheme.colorScheme.onSurfaceVariant
    val surface = MaterialTheme.colorScheme.surface
    val logged = days.count { it.isLogged }

    Canvas(
        modifier
            .fillMaxWidth()
            .height(240.dp)
            .semantics { contentDescription = "Calories per day for the last ${days.size} days, $logged days logged" }
            .pointerInput(days) {
                detectTapGestures { tap ->
                    val left = Y_AXIS_WIDTH.toPx()
                    val slot = (size.width - left) / days.size
                    val index = ((tap.x - left) / slot).toInt()
                    onSelect(if (index in days.indices && index != selectedIndex) index else null)
                }
            },
    ) {
        if (days.isEmpty()) return@Canvas
        val left = Y_AXIS_WIDTH.toPx()
        val plotBottom = size.height - X_AXIS_HEIGHT.toPx()
        val plotTop = 8.dp.toPx()
        val plotHeight = plotBottom - plotTop

        val maxValue = max(days.maxOf { it.stackKcal }, goalKcal ?: 0.0).coerceAtLeast(500.0) * 1.05
        val step = niceStep(maxValue)
        val yMax = ceil(maxValue / step) * step
        fun y(kcal: Double) = (plotBottom - kcal / yMax * plotHeight).toFloat()

        // Grid and y-axis labels
        var tick = 0.0
        while (tick <= yMax + 0.1) {
            val ty = y(tick)
            drawLine(gridColor, Offset(left, ty), Offset(size.width, ty), strokeWidth = 1.dp.toPx())
            drawLabel(textMeasurer, compact(tick), labelStyle, Offset(0f, ty), alignRight = left - 6.dp.toPx())
            tick += step
        }

        // Columns
        val slot = (size.width - left) / days.size
        val columnWidth = min(slot * 0.7f, MAX_COLUMN_WIDTH.toPx()).coerceAtLeast(1f)
        val gap = SEGMENT_GAP.toPx()
        days.forEachIndexed { i, day ->
            if (!day.isLogged) return@forEachIndexed
            val alpha = if (selectedIndex == null || selectedIndex == i) 1f else 0.35f
            val x = left + slot * i + (slot - columnWidth) / 2
            val segments = listOf(
                day.proteinKcal to colors.protein,
                day.carbsKcal to colors.carbs,
                day.fatKcal to colors.fat,
                day.otherKcal to colors.other,
            ).filter { (kcal, _) -> kcal > 0 }
            var base = 0.0
            segments.forEachIndexed { s, (kcal, color) ->
                val top = y(base + kcal)
                // Leave a surface-coloured gap below every segment except the bottom one.
                val bottom = y(base) - if (s > 0) gap / 2 else 0f
                val segTop = top + if (s < segments.lastIndex) gap / 2 else 0f
                if (bottom - segTop > 0.5f) {
                    val isTop = s == segments.lastIndex
                    drawSegment(Rect(x, segTop, x + columnWidth, bottom), color.copy(alpha = alpha), if (isTop) min(CORNER.toPx(), columnWidth / 2) else 0f)
                }
                base += kcal
            }
        }

        // Goal line, drawn over the columns with a surface halo so it stays visible
        if (goalKcal != null && goalKcal > 0) {
            val gy = y(goalKcal)
            drawLine(surface, Offset(left, gy), Offset(size.width, gy), strokeWidth = 4.dp.toPx())
            drawLine(
                goalColor, Offset(left, gy), Offset(size.width, gy),
                strokeWidth = 1.5.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
            )
            val label = textMeasurer.measure("Goal ${goalKcal.formatWhole()}", labelStyle)
            val lx = size.width - label.size.width
            drawRect(surface, Offset(lx - 4.dp.toPx(), gy - label.size.height - 2.dp.toPx()), Size(label.size.width + 4.dp.toPx(), label.size.height.toFloat()))
            drawText(label, topLeft = Offset(lx, gy - label.size.height - 2.dp.toPx()))
        }

        // X-axis labels
        val every = when {
            days.size <= 7 -> 1
            days.size <= 31 -> 7
            else -> 15
        }
        val formatter = if (days.size <= 7) DateTimeFormatter.ofPattern("EEE") else DateTimeFormatter.ofPattern("d MMM")
        // Right to left from today; skip any label that would touch the one drawn before it.
        var nextLabelRight = Float.MAX_VALUE
        days.indices.reversed().filter { (days.lastIndex - it) % every == 0 }.forEach { i ->
            val center = left + slot * i + slot / 2
            val label = textMeasurer.measure(days[i].date.format(formatter), labelStyle)
            val lx = (center - label.size.width / 2).coerceIn(left, size.width - label.size.width)
            if (lx + label.size.width + 8.dp.toPx() <= nextLabelRight) {
                drawText(label, topLeft = Offset(lx, plotBottom + 4.dp.toPx()))
                nextLabelRight = lx
            }
        }
    }
}

private fun DrawScope.drawSegment(rect: Rect, color: Color, topRadius: Float) {
    if (topRadius <= 0f) {
        drawRect(color, rect.topLeft, rect.size)
        return
    }
    val path = Path().apply {
        addRoundRect(
            RoundRect(
                rect = rect,
                topLeft = CornerRadius(topRadius),
                topRight = CornerRadius(topRadius),
                bottomRight = CornerRadius.Zero,
                bottomLeft = CornerRadius.Zero,
            ),
        )
    }
    drawPath(path, color)
}

private fun DrawScope.drawLabel(measurer: TextMeasurer, text: String, style: TextStyle, anchor: Offset, alignRight: Float) {
    val layout = measurer.measure(text, style)
    drawText(layout, topLeft = Offset(alignRight - layout.size.width, anchor.y - layout.size.height / 2))
}

/** Grid step giving at most ~5 lines: 250, 500, 1000, ... kcal. */
private fun niceStep(maxValue: Double): Double =
    listOf(250.0, 500.0, 1000.0, 2000.0, 5000.0).firstOrNull { maxValue / it <= 5 } ?: 10_000.0

/** 0, 500, 1K, 1.5K, 2K ... */
private fun compact(value: Double): String = when {
    value == 0.0 -> "0"
    value < 1000 -> value.formatWhole()
    value % 1000 == 0.0 -> "${(value / 1000).toInt()}K"
    else -> "${value / 1000}K"
}
