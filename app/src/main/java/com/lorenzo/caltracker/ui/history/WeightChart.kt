package com.lorenzo.caltracker.ui.history

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.lorenzo.caltracker.ui.formatAmount
import java.time.LocalDate
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Weight over time: a line through the weigh-ins, [visibleCount] days at a time, ending today.
 * Drag horizontally for earlier days. The y-axis fits the weigh-ins on screen.
 *
 * @param dates every day that can be shown, oldest first, ending today.
 */
@Composable
fun WeightChart(
    dates: List<LocalDate>,
    weights: Map<LocalDate, Double>,
    visibleCount: Int,
    onVisibleRangeChange: (IntRange) -> Unit,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.colorScheme.onSurfaceVariant)
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val lineColor = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surface
    val density = LocalDensity.current

    val daysBack = rememberDaysBack(visibleCount)
    val maxBack = (dates.size - visibleCount).coerceAtLeast(0).toFloat()
    var widthPx by remember { mutableIntStateOf(0) }
    val slotPx = with(density) { (widthPx - Y_AXIS_WIDTH.toPx()) / visibleCount }.coerceAtLeast(1f)
    ReportVisibleRange(daysBack, dates.size, visibleCount, onVisibleRangeChange)

    // Weigh-ins as (day index, kg), in date order.
    val points = remember(dates, weights) {
        dates.withIndex().mapNotNull { (i, d) -> weights[d]?.let { i to it } }
    }
    val average = remember(points) { rollingAverage(points) }

    Canvas(
        modifier
            .fillMaxWidth()
            .height(240.dp)
            .onSizeChanged { widthPx = it.width }
            .semantics { contentDescription = "Weight over time, $visibleCount days at a time. Drag sideways for earlier days." }
            .dayScroll(daysBack, maxBack, slotPx),
    ) {
        if (dates.isEmpty()) return@Canvas
        val left = Y_AXIS_WIDTH.toPx()
        val plotBottom = size.height - X_AXIS_HEIGHT.toPx()
        val plotTop = 12.dp.toPx()
        val firstVisible = dates.lastIndex - daysBack.value - visibleCount + 1
        fun x(index: Int) = left + (index - firstVisible) * slotPx + slotPx / 2
        val drawn = (floor(firstVisible).toInt() - 1).coerceAtLeast(0)..min(ceil(firstVisible + visibleCount).toInt(), dates.lastIndex)

        // Fit the y-axis to the weigh-ins on screen once scrolling settles (all of them if none are on screen).
        val settled = visibleRange(dates.size, visibleCount, daysBack.targetValue.roundToInt())
        val inView = points.filter { it.first in settled }.map { it.second }.ifEmpty { points.map { it.second } }
        val (yMin, yMax, step) = weightAxis(inView.minOrNull() ?: 70.0, inView.maxOrNull() ?: 80.0)
        fun y(kg: Double) = (plotBottom - (kg - yMin) / (yMax - yMin) * (plotBottom - plotTop)).toFloat()

        var tick = yMin
        while (tick <= yMax + 1e-6) {
            val ty = y(tick)
            drawLine(gridColor, Offset(left, ty), Offset(size.width, ty), strokeWidth = 1.dp.toPx())
            drawAxisLabel(textMeasurer, tick.formatAmount(), labelStyle, ty, right = left - 6.dp.toPx())
            tick += step
        }

        clipRect(left = left, top = 0f, right = size.width, bottom = size.height) {
            // Include the nearest weigh-in on each side of the screen, so the line runs off the edges.
            val first = points.indexOfLast { it.first < drawn.first }.coerceAtLeast(0)
            val last = points.indexOfFirst { it.first > drawn.last }.let { if (it == -1) points.lastIndex else it }
            val shown = if (points.isEmpty()) emptyList() else points.subList(first, last + 1)
            // 7-day average: faded and dotted, drawn under the weigh-ins.
            val avgShown = average.filter { it.first in (drawn.first - 1)..(drawn.last + 1) }
            if (avgShown.size > 1) {
                val avgPath = Path()
                avgShown.forEachIndexed { i, (index, kg) ->
                    if (i == 0) avgPath.moveTo(x(index), y(kg)) else avgPath.lineTo(x(index), y(kg))
                }
                drawPath(
                    avgPath,
                    lineColor.copy(alpha = AVERAGE_ALPHA),
                    style = Stroke(
                        width = 2.dp.toPx(),
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(1.dp.toPx(), 5.dp.toPx())),
                    ),
                )
            }
            if (shown.size > 1) {
                val path = Path()
                shown.forEachIndexed { i, (index, kg) ->
                    if (i == 0) path.moveTo(x(index), y(kg)) else path.lineTo(x(index), y(kg))
                }
                drawPath(path, lineColor, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            // Dots with a surface-coloured ring, so they stay distinct where the line passes through.
            shown.forEach { (index, kg) ->
                val center = Offset(x(index), y(kg))
                drawCircle(surface, radius = 6.dp.toPx(), center = center)
                drawCircle(lineColor, radius = 4.dp.toPx(), center = center)
            }
            drawDateLabels(
                dates, drawn.reversed(), visibleCount, { x(it) - slotPx / 2 }, slotPx, left,
                top = plotBottom + 4.dp.toPx(), measurer = textMeasurer, style = labelStyle,
            )
        }
    }
}

internal const val AVERAGE_ALPHA = 0.5f

/**
 * 7-day rolling average for every day from the first to the last weigh-in, as (day index, kg):
 * the mean of the weigh-ins in that day and the 6 before it. Days with none in the window are skipped.
 */
internal fun rollingAverage(points: List<Pair<Int, Double>>, window: Int = 7): List<Pair<Int, Double>> {
    if (points.isEmpty()) return emptyList()
    return (points.first().first..points.last().first).mapNotNull { day ->
        val inWindow = points.filter { it.first in (day - window + 1)..day }
        if (inWindow.isEmpty()) null else day to inWindow.sumOf { it.second } / inWindow.size
    }
}

/** A y-axis range around [min]..[max] kg with a round step, giving about 4–6 grid lines. */
internal fun weightAxis(min: Double, max: Double): Triple<Double, Double, Double> {
    val span = (max - min).coerceAtLeast(1.0)
    val step = listOf(0.5, 1.0, 2.0, 5.0, 10.0, 20.0).first { span / it <= 5 }
    val low = floor((min - step / 2) / step) * step
    val high = ceil((max + step / 2) / step) * step
    return Triple(low, high, step)
}
