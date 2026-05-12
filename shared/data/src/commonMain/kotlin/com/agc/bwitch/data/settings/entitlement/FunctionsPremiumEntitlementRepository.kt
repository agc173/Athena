package com.agc.bwitch.data.settings.entitlement

import com.agc.bwitch.data.functions.FunctionsClient
import com.agc.bwitch.domain.settings.GooglePlayPurchase
import com.agc.bwitch.domain.settings.KnownSubscriptionProducts
import com.agc.bwitch.domain.settings.PremiumEntitlement
import com.agc.bwitch.domain.settings.PremiumEntitlementRepository
import com.agc.bwitch.domain.settings.SubscriptionStatus
import com.agc.bwitch.domain.shared.ApiError
import com.agc.bwitch.domain.shared.ApiResult
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer

class FunctionsPremiumEntitlementRepository(
    private val functionsClient: FunctionsClient,
) : PremiumEntitlementRepository {

    override suspend fun validateGooglePlayPurchase(purchase: GooglePlayPurchase): PremiumEntitlement {
        val response = callBackend(
            name = VALIDATE_GOOGLE_PLAY_PURCHASE_CALLABLE,
            data = purchase.toDto(),
            requestSerializer = GooglePlayPurchaseDto.serializer(),
            responseSerializer = PremiumEntitlementDto.serializer(),
        )
        return response.toDomain(fallbackProductId = purchase.productId)
    }

    override suspend fun restoreGooglePlayPurchases(purchases: List<GooglePlayPurchase>): PremiumEntitlement {
        val response = callBackend(
            name = RESTORE_GOOGLE_PLAY_PURCHASES_CALLABLE,
            data = RestoreGooglePlayPurchasesRequestDto(purchases = purchases.map { it.toDto() }),
            requestSerializer = RestoreGooglePlayPurchasesRequestDto.serializer(),
            responseSerializer = PremiumEntitlementDto.serializer(),
        )
        return response.toDomain(fallbackProductId = purchases.firstOrNull()?.productId)
    }

    override suspend fun refreshEntitlement(): PremiumEntitlement {
        val response = callBackend(
            name = REFRESH_ENTITLEMENT_CALLABLE,
            data = Unit,
            requestSerializer = Unit.serializer(),
            responseSerializer = PremiumEntitlementDto.serializer(),
        )
        return response.toDomain(fallbackProductId = null)
    }

    private suspend fun <Req : Any, Res : Any> callBackend(
        name: String,
        data: Req,
        requestSerializer: kotlinx.serialization.KSerializer<Req>,
        responseSerializer: kotlinx.serialization.KSerializer<Res>,
    ): Res = when (
        val result = functionsClient.call(
            name = name,
            data = data,
            requestSerializer = requestSerializer,
            responseSerializer = responseSerializer,
        )
    ) {
        is ApiResult.Ok -> result.value
        is ApiResult.Err -> throw result.error.toException()
    }

    private companion object {
        const val VALIDATE_GOOGLE_PLAY_PURCHASE_CALLABLE = "validateGooglePlayPurchase"
        const val RESTORE_GOOGLE_PLAY_PURCHASES_CALLABLE = "restoreGooglePlayPurchases"
        const val REFRESH_ENTITLEMENT_CALLABLE = "refreshEntitlement"
    }
}

@Serializable
private data class GooglePlayPurchaseDto(
    val productId: String,
    val basePlanId: String? = null,
    val purchaseToken: String,
    val purchaseState: String,
    val isAcknowledged: Boolean,
    val orderId: String? = null,
    val packageName: String,
)

@Serializable
private data class RestoreGooglePlayPurchasesRequestDto(
    val purchases: List<GooglePlayPurchaseDto> = emptyList(),
)

@Serializable
private data class PremiumEntitlementDto(
    val active: Boolean? = null,
    val isActive: Boolean? = null,
    val isPremium: Boolean? = null,
    val premium: PremiumResponseDto? = null,
    val status: String? = null,
    val productId: String? = null,
    val planType: String? = null,
)

@Serializable
private data class PremiumResponseDto(
    val isPremium: Boolean? = null,
)

private fun GooglePlayPurchase.toDto(): GooglePlayPurchaseDto = GooglePlayPurchaseDto(
    productId = productId,
    basePlanId = if (productId == KnownSubscriptionProducts.MONTHLY) {
        KnownSubscriptionProducts.MONTHLY_BASE_PLAN_ID
    } else {
        null
    },
    purchaseToken = purchaseToken,
    purchaseState = purchaseState.name.uppercase(),
    isAcknowledged = isAcknowledged,
    orderId = orderId,
    packageName = packageName,
)

private fun PremiumEntitlementDto.toDomain(fallbackProductId: String?): PremiumEntitlement {
    val active = active ?: isActive ?: isPremium ?: premium?.isPremium ?: false
    val resolvedStatus = when {
        !active -> SubscriptionStatus.Inactive
        status.equals("ACTIVE_MONTHLY", ignoreCase = true) -> SubscriptionStatus.ActiveMonthly
        status.equals("ACTIVE", ignoreCase = true) -> SubscriptionStatus.ActiveMonthly
        planType.equals("MONTHLY", ignoreCase = true) -> SubscriptionStatus.ActiveMonthly
        productId == KnownSubscriptionProducts.MONTHLY -> SubscriptionStatus.ActiveMonthly
        fallbackProductId == KnownSubscriptionProducts.MONTHLY -> SubscriptionStatus.ActiveMonthly
        else -> SubscriptionStatus.ActiveMonthly
    }

    return PremiumEntitlement(
        isActive = active,
        status = resolvedStatus,
    )
}

private fun ApiError.toException(): IllegalStateException = IllegalStateException(message ?: "Premium entitlement backend request failed")
