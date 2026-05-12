package com.agc.bwitch.domain.settings

class ValidateGooglePlayPurchaseUseCase(
    private val repository: PremiumEntitlementRepository,
) {
    suspend operator fun invoke(purchase: GooglePlayPurchase): PremiumEntitlement =
        repository.validateGooglePlayPurchase(purchase)
}
