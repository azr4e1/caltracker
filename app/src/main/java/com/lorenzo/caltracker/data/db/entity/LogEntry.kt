@file:UseSerializers(LocalDateSerializer::class, LocalTimeSerializer::class)

package com.lorenzo.caltracker.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.lorenzo.caltracker.data.db.LocalDateSerializer
import com.lorenzo.caltracker.data.db.LocalTimeSerializer
import java.time.LocalDate
import java.time.LocalTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

enum class EntryUnit {
    G,
    ML,
    SERVING,

    /** A quick add: calories and macros entered directly, not tied to a food. */
    QUICK,
}

/**
 * One logged item at a given date and time.
 *
 * Nutrients are a snapshot of the totals for [amount] at the time of logging, so editing or
 * deleting a food/recipe later never changes past days. Changing the quantity scales the
 * snapshot linearly. A quick add uses [EntryUnit.QUICK]. [foodId] becomes null if the food is
 * deleted later; the entry keeps its snapshot.
 */
@Serializable
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
    indices = [Index("date", "time"), Index("foodId"), Index("recipeId")],
)
data class LogEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val time: LocalTime,
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
) {
    val isQuickAdd: Boolean get() = unit == EntryUnit.QUICK
}
