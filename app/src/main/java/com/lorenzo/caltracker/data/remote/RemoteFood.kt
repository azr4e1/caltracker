package com.lorenzo.caltracker.data.remote

import com.lorenzo.caltracker.data.db.entity.BaseUnit
import com.lorenzo.caltracker.data.db.entity.Food
import com.lorenzo.caltracker.data.db.entity.FoodSource
import kotlinx.serialization.Serializable

/**
 * A food from an online database, with nutrients per 100 g/ml.
 * Values are null when the source doesn't have them (common for user-contributed products).
 * Serializable so an incomplete one can be handed to the food editor as a draft.
 */
@Serializable
data class RemoteFood(
    val source: FoodSource,
    val externalId: String,
    val name: String,
    val brand: String? = null,
    val barcode: String? = null,
    val baseUnit: BaseUnit = BaseUnit.G,
    val kcal: Double? = null,
    val protein: Double? = null,
    val carbs: Double? = null,
    val fat: Double? = null,
    val fiber: Double? = null,
    val sugar: Double? = null,
    val saturatedFat: Double? = null,
    val salt: Double? = null,
    val servingSize: Double? = null,
) {
    /** Calories and all three macros are known, so it can be logged without editing. */
    val isComplete: Boolean get() = kcal != null && protein != null && carbs != null && fat != null

    /** Converts to a [Food]; missing required values become 0 (only used for complete foods or editor drafts). */
    fun toFood() = Food(
        name = name,
        brand = brand,
        barcode = barcode,
        baseUnit = baseUnit,
        servingName = servingSize?.let { "serving" },
        servingSize = servingSize,
        kcalPer100 = kcal ?: 0.0,
        proteinPer100 = protein ?: 0.0,
        carbsPer100 = carbs ?: 0.0,
        fatPer100 = fat ?: 0.0,
        fiberPer100 = fiber,
        sugarPer100 = sugar,
        saturatedFatPer100 = saturatedFat,
        saltPer100 = salt,
        source = source,
        externalId = externalId,
    )

    companion object {
        /** Empty draft for a barcode that no database knows. */
        fun blank(barcode: String) = RemoteFood(source = FoodSource.CUSTOM, externalId = "", name = "", barcode = barcode)
    }
}
