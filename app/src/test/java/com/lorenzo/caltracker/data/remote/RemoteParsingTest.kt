package com.lorenzo.caltracker.data.remote

import com.lorenzo.caltracker.data.db.entity.BaseUnit
import com.lorenzo.caltracker.data.db.entity.FoodSource
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Parses real API responses saved in src/test/resources/remote. */
class RemoteParsingTest {
    private fun fixture(name: String): JsonElement =
        Json.parseToJsonElement(javaClass.getResource("/remote/$name")!!.readText())

    @Test
    fun openFoodFactsProductByBarcode() {
        val food = OpenFoodFactsApi.parseProductResponse(fixture("off_product.json"))!!
        assertEquals("Spaghetti Nº5", food.name)
        assertEquals("Barilla", food.brand)
        assertEquals("8076800195057", food.barcode)
        assertEquals(FoodSource.OPEN_FOOD_FACTS, food.source)
        assertEquals(359.0, food.kcal!!, 0.01)
        assertEquals(13.0, food.protein!!, 0.01)
        assertEquals(71.0, food.carbs!!, 0.01)
        assertEquals(2.0, food.fat!!, 0.01)
        assertEquals(85.0, food.servingSize!!, 0.01)
        assertTrue(food.isComplete)
    }

    @Test
    fun openFoodFactsUnknownBarcodeIsNull() {
        assertNull(OpenFoodFactsApi.parseProductResponse(fixture("off_not_found.json")))
    }

    @Test
    fun openFoodFactsSearchReadsBrandListsAndSkipsFoodsWithoutCalories() {
        val results = OpenFoodFactsApi.parseSearchResponse(fixture("off_search.json"))
        assertTrue(results.isNotEmpty())
        assertTrue(results.all { it.kcal != null && it.brand != null })
    }

    @Test
    fun openFoodFactsHandlesStringNumbersKilojoulesAndLiquids() {
        val json = Json.parseToJsonElement(
            """{"code":"123","product_name":"Juice","brands":"A, B","product_quantity_unit":"ml",
               "nutriments":{"energy-kj_100g":"184.1","proteins_100g":"0,5","carbohydrates_100g":10,"fat_100g":null}}""",
        ) as JsonObject
        val food = OpenFoodFactsApi.parseProduct(json)!!
        assertEquals("A", food.brand)
        assertEquals(BaseUnit.ML, food.baseUnit)
        assertEquals(44.0, food.kcal!!, 0.01)
        assertEquals(0.5, food.protein!!, 0.001)
        assertNull(food.fat)
        assertFalse(food.isComplete)
    }

    @Test
    fun usdaSearch() {
        val results = UsdaApi.parseSearchResponse(fixture("usda_search.json"))
        val raw = results.first { it.name == "Bananas, raw" }
        assertEquals(FoodSource.USDA, raw.source)
        assertEquals("173944", raw.externalId)
        assertEquals(89.0, raw.kcal!!, 0.01)
        assertEquals(1.09, raw.protein!!, 0.001)
        assertEquals(22.8, raw.carbs!!, 0.001)
        assertEquals(0.33, raw.fat!!, 0.001)
        assertEquals(0.0025, raw.salt!!, 0.00001) // 1 mg sodium
        assertTrue(raw.isComplete)
    }
}
