package com.agc.bwitch.domain.settings

interface PremiumEntitlementRepository {
    suspend fun validateGooglePlayPurchase(purchase: GooglePlayPurchase): PremiumEntitlement
    suspend fun restoreGooglePlayPurchases(purchases: List<GooglePlayPurchase>): PremiumEntitlement
    suspend fun refreshEntitlement(): PremiumEntitlement
}
