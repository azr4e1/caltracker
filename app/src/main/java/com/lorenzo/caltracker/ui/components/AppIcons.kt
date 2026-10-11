package com.lorenzo.caltracker.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

/** Icons not in the core Material set. Colours here are placeholders; Icon() tints them. */
object AppIcons {
    /** Fork and knife (Material "restaurant"). */
    val Food: ImageVector by lazy {
        ImageVector.Builder("Food", 24.dp, 24.dp, 24f, 24f).apply {
            addPath(
                pathData = addPathNodes(
                    "M11,9H9V2H7v7H5V2H3v7c0,2.12 1.66,3.84 3.75,3.97V22h2.5v-9.03C11.34,12.84 13,11.12 13,9V2h-2V9z" +
                        "M16,6v8h2.5v8H21V2C18.24,2 16,4.24 16,6z",
                ),
                fill = SolidColor(Color.Black),
            )
        }.build()
    }

    /** Bathroom scale: rounded body, dial and needle. */
    val Weight: ImageVector by lazy {
        ImageVector.Builder("Weight", 24.dp, 24.dp, 24f, 24f).apply {
            fun stroke(path: String) = addPath(
                pathData = addPathNodes(path),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = 2f,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round,
            )
            stroke("M7,3h10a4,4 0 0 1 4,4v10a4,4 0 0 1 -4,4H7a4,4 0 0 1 -4,-4V7a4,4 0 0 1 4,-4z")
            stroke("M7.5,11a4.5,4.5 0 0 1 9,0")
            stroke("M12,11l2,-2.5")
        }.build()
    }
}
