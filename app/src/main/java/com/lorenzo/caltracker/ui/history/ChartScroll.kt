package com.lorenzo.caltracker.ui.history

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt
import kotlinx.coroutines.launch

// Shared by the history charts: a window of `visibleCount` days that scrolls back from today.

internal val Y_AXIS_WIDTH = 36.dp
internal val X_AXIS_HEIGHT = 22.dp

/** How many days the window is scrolled back from today (0 = ends today). Fractional while dragging. */
@Composable
internal fun rememberDaysBack(visibleCount: Int): Animatable<Float, AnimationVector1D> =
    remember(visibleCount) { Animatable(0f) }

/** Indices of the days on screen when scrolled [daysBack] whole days from the end. */
internal fun visibleRange(dayCount: Int, visibleCount: Int, daysBack: Int): IntRange {
    val end = dayCount - 1 - daysBack
    return (end - visibleCount + 1).coerceAtLeast(0)..end
}

/** Calls [onChange] with the visible day indices whenever the (rounded) scroll position changes. */
@Composable
internal fun ReportVisibleRange(daysBack: Animatable<Float, AnimationVector1D>, dayCount: Int, visibleCount: Int, onChange: (IntRange) -> Unit) {
    val count by rememberUpdatedState(dayCount)
    val callback by rememberUpdatedState(onChange)
    LaunchedEffect(daysBack, visibleCount) {
        snapshotFlow { daysBack.value.roundToInt() to count }
            .collect { (back, n) -> callback(visibleRange(n, visibleCount, back)) }
    }
}

/** Horizontal drag that scrolls the window, then settles on a whole day (coasting a little after a fling). */
@Composable
internal fun Modifier.dayScroll(daysBack: Animatable<Float, AnimationVector1D>, maxBack: Float, slotPx: Float): Modifier {
    val scope = rememberCoroutineScope()
    val state = rememberDraggableState { delta ->
        scope.launch { daysBack.snapTo((daysBack.value + delta / slotPx).coerceIn(0f, maxBack)) }
    }
    return draggable(
        state = state,
        orientation = Orientation.Horizontal,
        onDragStopped = { velocity ->
            val target = (daysBack.value + velocity / slotPx * 0.15f).roundToInt().toFloat().coerceIn(0f, maxBack)
            daysBack.animateTo(target)
        },
    )
}

/**
 * Date labels under the plot, anchored to days counted back from today so they scroll with the
 * data. Labels cut off at the edges or touching their neighbour are skipped.
 */
internal fun DrawScope.drawDateLabels(
    dates: List<LocalDate>,
    indices: IntProgression,
    visibleCount: Int,
    x: (Int) -> Float,
    slotPx: Float,
    left: Float,
    top: Float,
    measurer: TextMeasurer,
    style: TextStyle,
) {
    val every = when {
        visibleCount <= 7 -> 1
        visibleCount <= 31 -> 7
        else -> 15
    }
    val formatter = if (visibleCount <= 7) DateTimeFormatter.ofPattern("EEE") else DateTimeFormatter.ofPattern("d MMM")
    var nextLabelRight = Float.MAX_VALUE
    for (i in indices) {
        if ((dates.lastIndex - i) % every != 0) continue
        val label = measurer.measure(dates[i].format(formatter), style)
        val lx = x(i) + slotPx / 2 - label.size.width / 2
        val fits = lx >= left && lx + label.size.width <= size.width
        if (fits && lx + label.size.width + 8.dp.toPx() <= nextLabelRight) {
            drawText(label, topLeft = Offset(lx, top))
            nextLabelRight = lx
        }
    }
}

/** A y-axis label right-aligned at [right], vertically centred on [y]. */
internal fun DrawScope.drawAxisLabel(measurer: TextMeasurer, text: String, style: TextStyle, y: Float, right: Float) {
    val layout = measurer.measure(text, style)
    drawText(layout, topLeft = Offset(right - layout.size.width, y - layout.size.height / 2))
}
