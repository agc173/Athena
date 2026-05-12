package com.agc.bwitch.domain.settings

class RestoreGooglePlayPurchasesUseCase(
    private val repository: PremiumEntitlementRepository,
) {
    suspend operator fun invoke(purchases: List<GooglePlayPurchase>): PremiumEntitlement =
        repository.restoreGooglePlayPurchases(purchases)
}
