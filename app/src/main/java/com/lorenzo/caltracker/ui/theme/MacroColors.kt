package com.lorenzo.caltracker.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * One fixed colour per macro, used everywhere macros appear (diary bars, history chart).
 * Blue / orange / aqua is a colour-blind-safe trio, with steps tuned separately for dark mode.
 */
data class MacroColors(val protein: Color, val carbs: Color, val fat: Color, val other: Color)

@Composable
fun macroColors(): MacroColors {
    val other = MaterialTheme.colorScheme.outline
    return if (isSystemInDarkTheme()) {
        MacroColors(protein = Color(0xFF3987E5), carbs = Color(0xFFD95926), fat = Color(0xFF199E70), other = other)
    } else {
        MacroColors(protein = Color(0xFF2A78D6), carbs = Color(0xFFEB6834), fat = Color(0xFF1BAF7A), other = other)
    }
}
