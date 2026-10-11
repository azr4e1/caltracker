package com.lorenzo.caltracker.data.repository

import com.lorenzo.caltracker.data.remote.OpenFoodFactsApi
import com.lorenzo.caltracker.data.remote.RemoteFood
import com.lorenzo.caltracker.data.remote.UsdaApi
import java.io.IOException

sealed interface BarcodeResult {
    /** The food is (now) saved locally and ready to log. */
    data class Found(val foodId: Long) : BarcodeResult

    /** Known product, but nutrition values are missing: the user has to complete them. */
    data class Incomplete(val draft: RemoteFood) : BarcodeResult

    data class NotFound(val barcode: String) : BarcodeResult
}

sealed interface UsdaKeyCheck {
    data object Valid : UsdaKeyCheck

    data object Invalid : UsdaKeyCheck

    data class Unverified(val reason: String) : UsdaKeyCheck
}

/** Online food search and barcode lookup. Network failures are thrown as exceptions. */
class OnlineFoodRepository(
    private val foodRepository: FoodRepository,
    private val preferences: PreferencesRepository,
) {
    /** True when USDA searches use the shared key built into the app. */
    val usesSharedUsdaKey: Boolean get() = preferences.usdaApiKey.value == null

    suspend fun searchProducts(query: String): List<RemoteFood> = OpenFoodFactsApi.search(query)

    suspend fun searchGeneric(query: String): List<RemoteFood> =
        UsdaApi.search(query, preferences.usdaApiKey.value ?: UsdaApi.BUILT_IN_KEY)

    /**
     * Checks a USDA key with a tiny search. Only a key USDA rejects is [UsdaKeyCheck.Invalid];
     * a rate-limited key or a network problem can't be confirmed but isn't wrong.
     */
    suspend fun checkUsdaKey(key: String): UsdaKeyCheck = try {
        UsdaApi.search("apple", key)
        UsdaKeyCheck.Valid
    } catch (e: IOException) {
        when {
            e.message?.contains("403") == true -> UsdaKeyCheck.Invalid
            e.message?.contains("429") == true -> UsdaKeyCheck.Unverified("it has reached its hourly limit right now")
            else -> UsdaKeyCheck.Unverified("USDA couldn't be reached")
        }
    }

    /** Checks saved foods first (works offline), then Open Food Facts. */
    suspend fun lookupBarcode(barcode: String): BarcodeResult {
        foodRepository.getByBarcode(barcode)?.let { return BarcodeResult.Found(it.id) }
        val remote = OpenFoodFactsApi.product(barcode) ?: return BarcodeResult.NotFound(barcode)
        return if (remote.isComplete) BarcodeResult.Found(foodRepository.saveRemote(remote)) else BarcodeResult.Incomplete(remote)
    }

    suspend fun save(remote: RemoteFood): Long = foodRepository.saveRemote(remote)
}
