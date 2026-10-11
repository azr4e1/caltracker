package com.lorenzo.caltracker.data.remote

import com.lorenzo.caltracker.BuildConfig
import com.lorenzo.caltracker.data.db.entity.FoodSource
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

/**
 * USDA FoodData Central: lab-measured values for generic foods ("Bananas, raw").
 * Only the Foundation and SR Legacy datasets are searched; branded products come from Open Food Facts.
 */
object UsdaApi {
    // Nutrient ids in FoodData Central.
    private const val ENERGY_KCAL = 1008
    private const val ENERGY_ATWATER_GENERAL = 2047
    private const val ENERGY_ATWATER_SPECIFIC = 2048
    private const val PROTEIN = 1003
    private const val FAT = 1004
    private const val CARBS = 1005
    private const val FIBER = 1079
    private const val SUGARS = 2000
    private const val SATURATED_FAT = 1258
    private const val SODIUM_MG = 1093

    /** Key built into the app: from local.properties at build time, otherwise USDA's shared DEMO_KEY. */
    const val BUILT_IN_KEY: String = BuildConfig.USDA_API_KEY

    suspend fun search(query: String, apiKey: String = BUILT_IN_KEY): List<RemoteFood> {
        val json = getJson(
            "https://api.nal.usda.gov/fdc/v1/foods/search?query=${encode(query)}" +
                "&dataType=Foundation,SR%20Legacy&pageSize=25&api_key=${encode(apiKey)}",
        )
        return parseSearchResponse(json)
    }

    internal fun parseSearchResponse(json: JsonElement): List<RemoteFood> =
        json.obj()?.get("foods").arr().orEmpty().mapNotNull { parseFood(it.obj() ?: return@mapNotNull null) }
            .filter { it.kcal != null }

    internal fun parseFood(f: JsonObject): RemoteFood? {
        val id = f["fdcId"].str() ?: return null
        val values = f["foodNutrients"].arr().orEmpty().mapNotNull { element ->
            val n = element.obj() ?: return@mapNotNull null
            val nutrientId = n["nutrientId"].num()?.toInt() ?: return@mapNotNull null
            val value = n["value"].num() ?: return@mapNotNull null
            nutrientId to value
        }.toMap()
        // All values in these datasets are per 100 g.
        return RemoteFood(
            source = FoodSource.USDA,
            externalId = id,
            name = f["description"].str() ?: return null,
            kcal = values[ENERGY_KCAL] ?: values[ENERGY_ATWATER_SPECIFIC] ?: values[ENERGY_ATWATER_GENERAL],
            protein = values[PROTEIN],
            carbs = values[CARBS],
            fat = values[FAT],
            fiber = values[FIBER],
            sugar = values[SUGARS],
            saturatedFat = values[SATURATED_FAT],
            // Salt = sodium × 2.5
            salt = values[SODIUM_MG]?.let { it * 2.5 / 1000 },
        )
    }
}
