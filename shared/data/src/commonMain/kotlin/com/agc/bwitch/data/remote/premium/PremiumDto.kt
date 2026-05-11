package com.agc.bwitch.data.remote.premium

import kotlinx.serialization.Serializable

@Serializable
data class PremiumEntitlementDto(
    val isSubscriber: Boolean = false,
    val tier: String? = null,
    val status: String = "NONE",
    val productId: String? = null,
    val basePlanId: String? = null,
    val platform: String? = null,
    val environment: String? = null,
    val premiumUntilIso: String? = null,
    val autoRenewing: Boolean? = null,
    val lastValidatedAtIso: String? = null,
    val gracePeriodUntilIso: String? = null,
    val needsRestore: Boolean = false,
    val updatedAtIso: String? = null,
)

@Serializable
data class ValidateGooglePlaySubscriptionRequest(
    val productId: String,
    val purchaseToken: String,
    val packageName: String? = null,
    val basePlanId: String? = null,
    val offerId: String? = null,
    val clientPurchaseState: String? = null,
    val clientAcknowledged: Boolean? = null,
)

@Serializable
data class RestoreGooglePlayPurchasesRequest(
    val purchases: List<GooglePlayPurchaseDto>,
)

@Serializable
data class BillingPurchaseTokenDto(
    val productId: String,
    val purchaseToken: String,
    val purchaseState: String,
    val acknowledged: Boolean,
    val packageName: String? = null,
    val basePlanId: String? = null,
)

@Serializable
data class GooglePlayPurchaseDto(
    val productId: String,
    val purchaseToken: String,
    val packageName: String? = null,
    val basePlanId: String? = null,
    val clientPurchaseState: String? = null,
    val clientAcknowledged: Boolean? = null,
)

@Serializable
data class RestoreGooglePlayPurchasesResponse(
    val entitlement: PremiumEntitlementDto = PremiumEntitlementDto(),
    val restoredCount: Int = 0,
    val activeTokenFound: Boolean = false,
)

@Serializable
data class RefreshEntitlementRequest(
    val force: Boolean = false,
)
