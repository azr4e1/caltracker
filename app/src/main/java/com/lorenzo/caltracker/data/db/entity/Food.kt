package com.lorenzo.caltracker.data.db.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable

enum class FoodSource { CUSTOM, OPEN_FOOD_FACTS, USDA }

/** Whether nutrient values are per 100 g (solids) or per 100 ml (liquids). */
enum class BaseUnit { G, ML }

/**
 * A food with nutrients stored per 100 g / 100 ml.
 * A food can optionally define a named serving, e.g. "1 slice" = 30 g.
 */
@Serializable
@Entity(
    tableName = "foods",
    indices = [Index(value = ["barcode"], unique = true), Index(value = ["name"])],
)
data class Food(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val brand: String? = null,
    val barcode: String? = null,
    val baseUnit: BaseUnit = BaseUnit.G,
    val servingName: String? = null,
    val servingSize: Double? = null,
    val kcalPer100: Double,
    val proteinPer100: Double,
    val carbsPer100: Double,
    val fatPer100: Double,
    val fiberPer100: Double? = null,
    val sugarPer100: Double? = null,
    val saturatedFatPer100: Double? = null,
    val saltPer100: Double? = null,
    val source: FoodSource = FoodSource.CUSTOM,
    val externalId: String? = null,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
)
