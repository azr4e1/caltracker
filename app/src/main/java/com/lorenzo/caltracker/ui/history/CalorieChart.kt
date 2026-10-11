package com.lorenzo.caltracker.ui.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.lorenzo.caltracker.ui.formatWhole
import com.lorenzo.caltracker.ui.theme.MacroColors
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

private val SEGMENT_GAP = 2.dp
private val CORNER = 4.dp
private val MAX_COLUMN_WIDTH = 28.dp

/**
 * Stacked column chart of calories per day, split into protein / carbs / fat / other, with the
 * calorie goal as a dashed line. Shows [visibleCount] days at a time, ending today; drag
 * horizontally to see earlier days. Tapping a column selects it, tapping it again deselects.
 *
 * @param days every day that can be shown, oldest first, ending today.
 * @param onVisibleRangeChange indices of [days] currently on screen, updated while scrolling.
 */
@Composable
fun CalorieChart(
    days: List<DayColumn>,
    visibleCount: Int,
    goalKcal: Double?,
    colors: MacroColors,
    selectedIndex: Int?,
    onSelect: (Int?) -> Unit,
    onVisibleRangeChange: (IntRange) -> Unit,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val goalColor = MaterialTheme.colorScheme.onSurfaceVariant
    val surface = MaterialTheme.colorScheme.surface
    val density = LocalDensity.current

    // Gestures are set up once, so they must read the latest values through these.
    val currentDays by rememberUpdatedState(days)
    val currentSelected by rememberUpdatedState(selectedIndex)
    val currentOnSelect by rememberUpdatedState(onSelect)

    val daysBack = rememberDaysBack(visibleCount)
    val maxBack = (days.size - visibleCount).coerceAtLeast(0).toFloat()
    var widthPx by remember { mutableIntStateOf(0) }
    val slotPx = with(density) { (widthPx - Y_AXIS_WIDTH.toPx()) / visibleCount }.coerceAtLeast(1f)
    ReportVisibleRange(daysBack, days.size, visibleCount, onVisibleRangeChange)

    Canvas(
        modifier
            .fillMaxWidth()
            .height(240.dp)
            .onSizeChanged { widthPx = it.width }
            .semantics { contentDescription = "Calories per day, $visibleCount days at a time. Drag sideways for earlier days." }
            .dayScroll(daysBack, maxBack, slotPx)
            .pointerInput(visibleCount) {
                detectTapGestures { tap ->
                    val left = Y_AXIS_WIDTH.toPx()
                    val firstVisible = currentDays.lastIndex - daysBack.value - visibleCount + 1
                    val index = floor(firstVisible + (tap.x - left) / slotPx).toInt()
                    currentOnSelect(if (index in currentDays.indices && index != currentSelected) index else null)
                }
            },
    ) {
        if (days.isEmpty()) return@Canvas
        val left = Y_AXIS_WIDTH.toPx()
        val plotBottom = size.height - X_AXIS_HEIGHT.toPx()
        val plotTop = 8.dp.toPx()
        val plotHeight = plotBottom - plotTop
        // Position of the leftmost visible slot, in days (fractional while dragging).
        val firstVisible = days.lastIndex - daysBack.value - visibleCount + 1
        fun x(index: Int) = left + (index - firstVisible) * slotPx
        val drawn = (floor(firstVisible).toInt() - 1).coerceAtLeast(0)..min(ceil(firstVisible + visibleCount).toInt(), days.lastIndex)

        // The y-axis fits the days on screen once the scroll settles, so it doesn't jump while dragging.
        val settled = visibleRange(days.size, visibleCount, daysBack.targetValue.roundToInt())
        val maxValue = max(settled.maxOfOrNull { days[it].stackKcal } ?: 0.0, goalKcal ?: 0.0).coerceAtLeast(500.0) * 1.05
        val step = niceStep(maxValue)
        val yMax = ceil(maxValue / step) * step
        fun y(kcal: Double) = (plotBottom - kcal / yMax * plotHeight).toFloat()

        // Grid and y-axis labels
        var tick = 0.0
        while (tick <= yMax + 0.1) {
            val ty = y(tick)
            drawLine(gridColor, Offset(left, ty), Offset(size.width, ty), strokeWidth = 1.dp.toPx())
            drawAxisLabel(textMeasurer, compact(tick), labelStyle, ty, right = left - 6.dp.toPx())
            tick += step
        }

        val columnWidth = min(slotPx * 0.7f, MAX_COLUMN_WIDTH.toPx()).coerceAtLeast(1f)
        val gap = SEGMENT_GAP.toPx()
        clipRect(left = left, top = 0f, right = size.width, bottom = size.height) {
            // Columns
            for (i in drawn) {
                val day = days[i]
                if (!day.isLogged) continue
                val alpha = if (selectedIndex == null || selectedIndex == i) 1f else 0.35f
                val cx = x(i) + (slotPx - columnWidth) / 2
                val segments = listOf(
                    day.proteinKcal to colors.protein,
                    day.carbsKcal to colors.carbs,
                    day.fatKcal to colors.fat,
                    day.otherKcal to colors.other,
                ).filter { (kcal, _) -> kcal > 0 }
                var base = 0.0
                segments.forEachIndexed { s, (kcal, color) ->
                    // Leave a surface-coloured gap between stacked segments.
                    val bottom = y(base) - if (s > 0) gap / 2 else 0f
                    val top = y(base + kcal) + if (s < segments.lastIndex) gap / 2 else 0f
                    if (bottom - top > 0.5f) {
                        val radius = if (s == segments.lastIndex) min(CORNER.toPx(), columnWidth / 2) else 0f
                        drawSegment(Rect(cx, top, cx + columnWidth, bottom), color.copy(alpha = alpha), radius)
                    }
                    base += kcal
                }
            }

            drawDateLabels(
                days.map { it.date }, drawn.reversed(), visibleCount, ::x, slotPx, left,
                top = plotBottom + 4.dp.toPx(), measurer = textMeasurer, style = labelStyle,
            )
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
