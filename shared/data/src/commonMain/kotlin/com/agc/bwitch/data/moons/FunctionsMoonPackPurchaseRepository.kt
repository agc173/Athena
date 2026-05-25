package com.agc.bwitch.data.moons

import com.agc.bwitch.data.functions.FunctionsClient
import com.agc.bwitch.domain.moons.MoonPackClaimResult
import com.agc.bwitch.domain.moons.MoonPackClaimStatus
import com.agc.bwitch.domain.moons.MoonPackPurchaseRepository
import com.agc.bwitch.domain.settings.GooglePlayPurchase
import com.agc.bwitch.domain.shared.ApiResult
import kotlinx.serialization.Serializable

class FunctionsMoonPackPurchaseRepository(private val functionsClient: FunctionsClient) : MoonPackPurchaseRepository {
    override suspend fun claimGooglePlayMoonPackPurchase(purchase: GooglePlayPurchase): MoonPackClaimResult {
        val req = ClaimMoonPackPurchaseRequestDto(
            productId = purchase.productId,
            purchaseToken = purchase.purchaseToken,
            packageName = purchase.packageName,
            orderId = purchase.orderId,
        )
        return when (val result = functionsClient.call("claimMoonPackPurchase", req, ClaimMoonPackPurchaseRequestDto.serializer(), ClaimMoonPackPurchaseResponseDto.serializer())) {
            is ApiResult.Ok -> MoonPackClaimResult(
                status = if (result.value.result == "ALREADY_CLAIMED") MoonPackClaimStatus.ALREADY_CLAIMED else MoonPackClaimStatus.CLAIMED,
                shouldConsume = result.value.shouldConsume,
            )
            is ApiResult.Err -> throw IllegalStateException(result.error.message ?: "claimMoonPackPurchase failed")
        }
    }
}

@Serializable
data class ClaimMoonPackPurchaseRequestDto(
    val productId: String,
    val purchaseToken: String,
    val packageName: String,
    val orderId: String? = null,
)

@Serializable
data class ClaimMoonPackPurchaseResponseDto(
    val result: String,
    val shouldConsume: Boolean = false,
)
