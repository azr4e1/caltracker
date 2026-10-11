package com.lorenzo.caltracker.domain

import com.lorenzo.caltracker.data.db.entity.ActivityLevel
import com.lorenzo.caltracker.data.db.entity.Sex
import com.lorenzo.caltracker.domain.Nutrients.Companion.KCAL_PER_G_CARBS
import com.lorenzo.caltracker.domain.Nutrients.Companion.KCAL_PER_G_FAT
import com.lorenzo.caltracker.domain.Nutrients.Companion.KCAL_PER_G_PROTEIN
import java.time.LocalDate
import java.time.Period

object GoalCalculator {
    /** Basal metabolic rate (kcal/day) using the Mifflin-St Jeor equation. */
    fun bmr(sex: Sex, ageYears: Int, heightCm: Double, weightKg: Double): Double {
        val base = 10 * weightKg + 6.25 * heightCm - 5 * ageYears
        return when (sex) {
            Sex.MALE -> base + 5
            Sex.FEMALE -> base - 161
        }
    }

    /** Total daily energy expenditure: BMR scaled by activity level. */
    fun tdee(sex: Sex, ageYears: Int, heightCm: Double, weightKg: Double, activity: ActivityLevel): Double =
        bmr(sex, ageYears, heightCm, weightKg) * activity.multiplier

    fun age(birthDate: LocalDate, today: LocalDate = LocalDate.now()): Int = Period.between(birthDate, today).years

    /**
     * Splits [kcal] into macros: protein from body weight, fat as a share of calories,
     * carbs fill whatever is left (never negative).
     */
    fun autoMacros(kcal: Double, weightKg: Double, proteinPerKg: Double, fatShare: Double): Nutrients {
        val protein = weightKg * proteinPerKg
        val fat = kcal * fatShare / KCAL_PER_G_FAT
        val carbs = ((kcal - protein * KCAL_PER_G_PROTEIN - fat * KCAL_PER_G_FAT) / KCAL_PER_G_CARBS).coerceAtLeast(0.0)
        return Nutrients(kcal = kcal, protein = protein, carbs = carbs, fat = fat)
    }

    /** Calories implied by a set of macro grams. */
    fun kcalFromMacros(protein: Double, carbs: Double, fat: Double): Double =
        protein * KCAL_PER_G_PROTEIN + carbs * KCAL_PER_G_CARBS + fat * KCAL_PER_G_FAT
}
