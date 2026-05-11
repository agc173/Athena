package com.agc.bwitch.data.premium

import com.agc.bwitch.data.remote.premium.GooglePlayPurchaseDto
import com.agc.bwitch.data.remote.premium.PremiumEntitlementDto
import com.agc.bwitch.data.remote.premium.PremiumRemoteDataSource
import com.agc.bwitch.data.remote.premium.RestoreGooglePlayPurchasesRequest
import com.agc.bwitch.data.remote.premium.RestoreGooglePlayPurchasesResponse
import com.agc.bwitch.data.remote.premium.ValidateGooglePlaySubscriptionRequest
import com.agc.bwitch.domain.settings.BillingPurchaseToken
import com.agc.bwitch.domain.settings.PremiumEntitlement
import com.agc.bwitch.domain.settings.PremiumEntitlementRepository
import com.agc.bwitch.domain.settings.PremiumRestoreResult
import com.agc.bwitch.domain.settings.PremiumSubscriptionStatus
import com.agc.bwitch.domain.settings.PurchaseState
import kotlinx.datetime.Instant

class PremiumEntitlementRepositoryImpl(
    private val remoteDataSource: PremiumRemoteDataSource,
) : PremiumEntitlementRepository {
    override suspend fun refreshPremiumEntitlement(force: Boolean): PremiumEntitlement {
        return remoteDataSource.refreshEntitlement(force).toDomain()
    }

    override suspend fun validateGooglePlayPurchase(token: BillingPurchaseToken): PremiumEntitlement {
        return remoteDataSource
            .validateGooglePlaySubscription(token.toValidateRequest())
            .toDomain()
    }

    override suspend fun restoreGooglePlayPurchases(tokens: List<BillingPurchaseToken>): PremiumRestoreResult {
        return remoteDataSource
            .restoreGooglePlayPurchases(
                RestoreGooglePlayPurchasesRequest(purchases = tokens.map { it.toGooglePlayPurchaseDto() }),
            )
            .toDomain()
    }
}

private fun BillingPurchaseToken.toValidateRequest(): ValidateGooglePlaySubscriptionRequest =
    ValidateGooglePlaySubscriptionRequest(
        productId = productId,
        purchaseToken = purchaseToken,
        packageName = packageName,
        basePlanId = basePlanId,
        clientPurchaseState = purchaseState.toBackendValue(),
        clientAcknowledged = acknowledged,
    )

private fun BillingPurchaseToken.toGooglePlayPurchaseDto(): GooglePlayPurchaseDto =
    GooglePlayPurchaseDto(
        productId = productId,
        purchaseToken = purchaseToken,
        packageName = packageName,
        basePlanId = basePlanId,
        clientPurchaseState = purchaseState.toBackendValue(),
        clientAcknowledged = acknowledged,
    )

private fun PurchaseState.toBackendValue(): String = when (this) {
    PurchaseState.Purchased -> "PURCHASED"
    PurchaseState.Pending -> "PENDING"
    PurchaseState.Unspecified -> "UNSPECIFIED"
}

private fun RestoreGooglePlayPurchasesResponse.toDomain(): PremiumRestoreResult = PremiumRestoreResult(
    entitlement = entitlement.toDomain(),
    restoredCount = restoredCount,
    activeTokenFound = activeTokenFound,
)

private fun PremiumEntitlementDto.toDomain(): PremiumEntitlement = PremiumEntitlement(
    isSubscriber = isSubscriber,
    status = status.toPremiumSubscriptionStatus(),
    needsRestore = needsRestore,
    premiumUntilEpochMillis = premiumUntilIso?.toEpochMillisOrNull(),
    productId = productId,
    basePlanId = basePlanId,
    platform = platform,
    environment = environment,
    autoRenewing = autoRenewing,
)

private fun String.toPremiumSubscriptionStatus(): PremiumSubscriptionStatus = when (uppercase()) {
    "NONE" -> PremiumSubscriptionStatus.None
    "ACTIVE" -> PremiumSubscriptionStatus.Active
    "PENDING" -> PremiumSubscriptionStatus.Pending
    "EXPIRED" -> PremiumSubscriptionStatus.Expired
    "CANCELED" -> PremiumSubscriptionStatus.Canceled
    "GRACE_PERIOD" -> PremiumSubscriptionStatus.GracePeriod
    "ACCOUNT_HOLD" -> PremiumSubscriptionStatus.AccountHold
    "PAUSED" -> PremiumSubscriptionStatus.Paused
    "REVOKED" -> PremiumSubscriptionStatus.Revoked
    else -> PremiumSubscriptionStatus.Unknown
}

private fun String.toEpochMillisOrNull(): Long? = runCatching { Instant.parse(this).toEpochMilliseconds() }.getOrNull()
