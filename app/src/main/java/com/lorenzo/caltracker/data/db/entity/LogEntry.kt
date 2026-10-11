package com.lorenzo.caltracker.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.time.LocalDate

enum class Meal { BREAKFAST, LUNCH, DINNER, SNACK }

enum class EntryUnit { G, ML, SERVING }

/**
 * One logged item on a given day.
 *
 * Nutrients are a snapshot of the totals for [amount] at the time of logging, so editing or
 * deleting a food/recipe later never changes past days. Changing the quantity scales the
 * snapshot linearly. A quick add has neither [foodId] nor [recipeId].
 */
@Entity(
    tableName = "log_entries",
    foreignKeys = [
        ForeignKey(
            entity = Food::class,
            parentColumns = ["id"],
            childColumns = ["foodId"],
            onDelete = ForeignKey.SET_NULL,
        ),
        ForeignKey(
            entity = Recipe::class,
            parentColumns = ["id"],
            childColumns = ["recipeId"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("date"), Index("foodId"), Index("recipeId")],
)
data class LogEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val meal: Meal,
    val position: Int = 0,
    val foodId: Long? = null,
    val recipeId: Long? = null,
    val name: String,
    val amount: Double,
    val unit: EntryUnit,
    val kcal: Double,
    val protein: Double,
    val carbs: Double,
    val fat: Double,
    val createdAt: Long = System.currentTimeMillis(),
)
