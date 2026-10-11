package com.lorenzo.caltracker.ui

import com.lorenzo.caltracker.data.db.entity.EntryUnit
import com.lorenzo.caltracker.data.db.entity.LogEntry
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

private val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

fun LocalTime.formatHm(): String = format(timeFormatter)

/** Whole number for kcal and grams of macros, e.g. "1240". */
fun Double.formatWhole(): String = roundToInt().toString()

/** Up to one decimal, dropping ".0": 100.0 -> "100", 1.5 -> "1.5". */
fun Double.formatAmount(): String {
    val rounded = (this * 10).roundToInt() / 10.0
    return if (rounded % 1.0 == 0.0) rounded.toInt().toString() else rounded.toString()
}

fun EntryUnit.label(amount: Double): String = when (this) {
    EntryUnit.G -> "g"
    EntryUnit.ML -> "ml"
    EntryUnit.SERVING -> if (amount == 1.0) "serving" else "servings"
    EntryUnit.QUICK -> ""
}

/** Parses user input, accepting both "1.5" and "1,5". */
fun String.toDecimalOrNull(): Double? = trim().replace(',', '.').toDoubleOrNull()

/** Unit shown next to a logged amount; recipe servings read as portions. */
fun LogEntry.unitLabel(value: Double = amount): String =
    if (recipeId != null && unit == EntryUnit.SERVING) {
        if (value == 1.0) "portion" else "portions"
    } else {
        unit.label(value)
    }

/** "1 entry", "3 entries". */
fun plural(count: Int, one: String, many: String): String = "$count ${if (count == 1) one else many}"
