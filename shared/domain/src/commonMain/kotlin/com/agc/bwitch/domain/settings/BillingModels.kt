package com.agc.bwitch.domain.settings

/** Product metadata exposed by the platform billing SDK. It is catalog information only. */
data class BillingProduct(
    val productId: String,
    val basePlanId: String? = null,
    val offerToken: String? = null,
    val title: String = productId,
    val formattedPrice: String,
    val priceAmountMicros: Long? = null,
    val priceCurrencyCode: String? = null,
    val billingPeriod: String? = null,
)

/** Local purchase token metadata to be sent to backend validation/restore in a later PR. */
data class BillingPurchaseToken(
    val productId: String,
    val purchaseToken: String,
    val purchaseState: PurchaseState,
    val acknowledged: Boolean,
    val packageName: String? = null,
    val basePlanId: String? = null,
)

enum class PurchaseState {
    Purchased,
    Pending,
    Unspecified,
}

sealed interface BillingPurchaseResult {
    data class Purchased(val token: BillingPurchaseToken) : BillingPurchaseResult
    data class Pending(val token: BillingPurchaseToken) : BillingPurchaseResult
    data object Cancelled : BillingPurchaseResult
    data class Failed(val reason: String, val code: Int? = null) : BillingPurchaseResult
    data object Unsupported : BillingPurchaseResult
}

/** Backend-owned subscription status. BillingClient must not construct this as local authority. */
enum class PremiumSubscriptionStatus {
    None,
    Active,
    Pending,
    Expired,
    Canceled,
    GracePeriod,
    AccountHold,
    Paused,
    Revoked,
    Unknown,
}

/** Backend-owned entitlement shape. BillingClient must not construct this as local authority. */
data class PremiumEntitlement(
    val isSubscriber: Boolean,
    val status: PremiumSubscriptionStatus,
    val needsRestore: Boolean = false,
    val premiumUntilEpochMillis: Long? = null,
    val productId: String? = null,
    val basePlanId: String? = null,
    val platform: String? = null,
    val environment: String? = null,
    val autoRenewing: Boolean? = null,
)

data class PremiumRestoreResult(
    val entitlement: PremiumEntitlement,
    val restoredCount: Int,
    val activeTokenFound: Boolean,
)

fun PremiumEntitlement.toSubscriptionStatus(): SubscriptionStatus = when {
    isSubscriber -> SubscriptionStatus.ActiveMonthly
    else -> SubscriptionStatus.Inactive
}
