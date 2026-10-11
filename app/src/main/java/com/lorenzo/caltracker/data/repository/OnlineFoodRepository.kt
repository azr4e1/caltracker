package com.lorenzo.caltracker.data.repository

import com.lorenzo.caltracker.data.remote.OpenFoodFactsApi
import com.lorenzo.caltracker.data.remote.RemoteFood
import com.lorenzo.caltracker.data.remote.UsdaApi

sealed interface BarcodeResult {
    /** The food is (now) saved locally and ready to log. */
    data class Found(val foodId: Long) : BarcodeResult

    /** Known product, but nutrition values are missing: the user has to complete them. */
    data class Incomplete(val draft: RemoteFood) : BarcodeResult

    data class NotFound(val barcode: String) : BarcodeResult
}

/** Online food search and barcode lookup. Network failures are thrown as exceptions. */
class OnlineFoodRepository(private val foodRepository: FoodRepository) {
    suspend fun searchProducts(query: String): List<RemoteFood> = OpenFoodFactsApi.search(query)

    suspend fun searchGeneric(query: String): List<RemoteFood> = UsdaApi.search(query)

    /** Checks saved foods first (works offline), then Open Food Facts. */
    suspend fun lookupBarcode(barcode: String): BarcodeResult {
        foodRepository.getByBarcode(barcode)?.let { return BarcodeResult.Found(it.id) }
        val remote = OpenFoodFactsApi.product(barcode) ?: return BarcodeResult.NotFound(barcode)
        return if (remote.isComplete) BarcodeResult.Found(foodRepository.saveRemote(remote)) else BarcodeResult.Incomplete(remote)
    }

    suspend fun save(remote: RemoteFood): Long = foodRepository.saveRemote(remote)
}
