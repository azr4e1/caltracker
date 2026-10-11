@file:UseSerializers(LocalDateSerializer::class)

package com.lorenzo.caltracker.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.lorenzo.caltracker.data.db.LocalDateSerializer
import java.time.LocalDate
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers

/** One weigh-in. At most one per day; logging again on the same day replaces it. */
@Serializable
@Entity(tableName = "weight_entries", indices = [Index(value = ["date"], unique = true)])
data class WeightEntry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val date: LocalDate,
    val weightKg: Double,
    val createdAt: Long = System.currentTimeMillis(),
)
