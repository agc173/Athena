package com.agc.bwitch.domain.settings

interface PremiumEntitlementRepository {
    suspend fun refreshPremiumEntitlement(force: Boolean): PremiumEntitlement
    suspend fun validateGooglePlayPurchase(token: BillingPurchaseToken): PremiumEntitlement
    suspend fun restoreGooglePlayPurchases(tokens: List<BillingPurchaseToken>): PremiumRestoreResult
}
