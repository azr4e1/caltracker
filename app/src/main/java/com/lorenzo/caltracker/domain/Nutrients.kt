package com.lorenzo.caltracker.domain

import com.lorenzo.caltracker.data.db.entity.Food
import com.lorenzo.caltracker.data.db.entity.LogEntry

data class Nutrients(
    val kcal: Double = 0.0,
    val protein: Double = 0.0,
    val carbs: Double = 0.0,
    val fat: Double = 0.0,
) {
    operator fun plus(other: Nutrients) =
        Nutrients(kcal + other.kcal, protein + other.protein, carbs + other.carbs, fat + other.fat)

    operator fun times(factor: Double) = Nutrients(kcal * factor, protein * factor, carbs * factor, fat * factor)

    companion object {
        const val KCAL_PER_G_PROTEIN = 4.0
        const val KCAL_PER_G_CARBS = 4.0
        const val KCAL_PER_G_FAT = 9.0
    }
}

/** Nutrients for [baseAmount] grams (or ml) of this food. */
fun Food.nutrientsFor(baseAmount: Double): Nutrients =
    Nutrients(kcalPer100, proteinPer100, carbsPer100, fatPer100) * (baseAmount / 100.0)

val LogEntry.nutrients: Nutrients get() = Nutrients(kcal, protein, carbs, fat)

fun List<LogEntry>.totalNutrients(): Nutrients = fold(Nutrients()) { acc, entry -> acc + entry.nutrients }
