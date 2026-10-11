package com.lorenzo.caltracker.ui

import com.lorenzo.caltracker.data.db.entity.EntryUnit
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
