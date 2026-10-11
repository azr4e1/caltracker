package com.lorenzo.caltracker.data.remote

import com.lorenzo.caltracker.data.db.entity.BaseUnit
import com.lorenzo.caltracker.data.db.entity.FoodSource
import java.util.Locale
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * Open Food Facts: a free, crowd-sourced database of packaged products, looked up by barcode
 * or searched by name. Data is ODbL-licensed.
 */
object OpenFoodFactsApi {
    private const val FIELDS =
        "code,product_name,brands,nutriments,serving_quantity,product_quantity_unit"

    /** Looks up a barcode. Returns null if the product isn't in the database. */
    suspend fun product(barcode: String): RemoteFood? {
        val json = getJson("https://world.openfoodfacts.org/api/v2/product/${encode(barcode)}?fields=$FIELDS")
        return parseProductResponse(json)
    }

    /** Searches products by name, in the phone's language first. */
    suspend fun search(query: String): List<RemoteFood> {
        val langs = "${Locale.getDefault().language},en"
        val json = getJson(
            "https://search.openfoodfacts.org/search?q=${encode(query)}&page_size=25&langs=$langs&fields=$FIELDS",
        )
        return parseSearchResponse(json)
    }

    internal fun parseProductResponse(json: JsonElement): RemoteFood? {
        val root = json.obj() ?: return null
        if (root["status"].num() != 1.0) return null
        return parseProduct(root["product"].obj() ?: return null)
    }

    /** Results without calories are dropped: they can't be logged. */
    internal fun parseSearchResponse(json: JsonElement): List<RemoteFood> =
        json.obj()?.get("hits").arr().orEmpty().mapNotNull { parseProduct(it.obj() ?: return@mapNotNull null) }
            .filter { it.kcal != null }

    internal fun parseProduct(p: JsonObject): RemoteFood? {
        val code = p["code"].str() ?: return null
        val n = p["nutriments"].obj() ?: JsonObject(emptyMap())
        // Prefer kcal; otherwise convert kJ.
        val kcal = n["energy-kcal_100g"].num()
            ?: n["energy-kj_100g"].num()?.div(4.184)
            ?: n["energy_100g"].num()?.div(4.184)
        // "brands" is a comma-separated string in one API and a list in the other.
        val brand = p["brands"].arr()?.mapNotNull { it.str() }?.firstOrNull()
            ?: p["brands"].str()?.split(',')?.first()?.trim()
        return RemoteFood(
            source = FoodSource.OPEN_FOOD_FACTS,
            externalId = code,
            name = p["product_name"].str() ?: return null,
            brand = brand,
            barcode = code,
            baseUnit = if (p["product_quantity_unit"].str()?.lowercase() == "ml") BaseUnit.ML else BaseUnit.G,
            kcal = kcal,
            protein = n["proteins_100g"].num(),
            carbs = n["carbohydrates_100g"].num(),
            fat = n["fat_100g"].num(),
            fiber = n["fiber_100g"].num(),
            sugar = n["sugars_100g"].num(),
            saturatedFat = n["saturated-fat_100g"].num(),
            salt = n["salt_100g"].num(),
            servingSize = p["serving_quantity"].num()?.takeIf { it > 0 },
        )
    }
}
