package com.agc.bwitch.data.moons

import com.agc.bwitch.domain.settings.GooglePlayPurchase

data class MoonPackProduct(
    val productId: String,
    val localizedPrice: String,
)

interface MoonPackBillingDataSource {
    val isSupported: Boolean
    suspend fun queryMoonPackProductDetails(): List<MoonPackProduct>
    suspend fun launchMoonPackPurchase(productId: String): Result<GooglePlayPurchase>
    suspend fun consumePurchase(purchaseToken: String): Result<Unit>
}

object UnsupportedMoonPackBillingDataSource : MoonPackBillingDataSource {
    override val isSupported: Boolean = false
    override suspend fun queryMoonPackProductDetails(): List<MoonPackProduct> = emptyList()
    override suspend fun launchMoonPackPurchase(productId: String): Result<GooglePlayPurchase> =
        Result.failure(IllegalStateException("Moon pack billing unsupported"))
    override suspend fun consumePurchase(purchaseToken: String): Result<Unit> =
        Result.failure(IllegalStateException("Moon pack consume unsupported"))
}
