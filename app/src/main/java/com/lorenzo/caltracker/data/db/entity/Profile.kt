@file:UseSerializers(LocalDateSerializer::class)

package com.lorenzo.caltracker.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.lorenzo.caltracker.data.db.LocalDateSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import java.time.LocalDate

enum class Sex { MALE, FEMALE }

enum class ActivityLevel(val multiplier: Double) {
    SEDENTARY(1.2),
    LIGHT(1.375),
    MODERATE(1.55),
    ACTIVE(1.725),
    VERY_ACTIVE(1.9),
}

/** Single-row table (id is always 1) with the body metrics used to calculate goals. */
@Serializable
@Entity(tableName = "profile")
data class Profile(
    @PrimaryKey val id: Int = 1,
    val sex: Sex,
    val birthDate: LocalDate,
    val heightCm: Double,
    val weightKg: Double,
    val activityLevel: ActivityLevel,
)

enum class GoalMode {
    /** Macros are calculated from the profile and the calorie target. */
    AUTO,

    /** Every value is set by hand. */
    MANUAL,
}

/** Single-row table (id is always 1) with the daily targets. */
@Serializable
@Entity(tableName = "goals")
data class Goals(
    @PrimaryKey val id: Int = 1,
    val mode: GoalMode = GoalMode.AUTO,
    val kcal: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    /** Used in AUTO mode: grams of protein per kg of body weight. */
    val proteinPerKg: Double = 2.0,
    /** Used in AUTO mode: share of calories from fat (0..1). */
    val fatShare: Double = 0.25,
)

/**
 * The targets in force from [date] until the next row. A row is written (or replaced) for today
 * whenever the goals change, so past days keep the targets they had. Days before the first row
 * use the first row.
 */
@Serializable
@Entity(tableName = "goal_history")
data class GoalPeriod(
    @PrimaryKey val date: LocalDate,
    val kcal: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
) {
    companion object {
        /** Start of goals carried over from before goals were kept per day. */
        val BEGINNING: LocalDate = LocalDate.of(1970, 1, 1)
    }
}
