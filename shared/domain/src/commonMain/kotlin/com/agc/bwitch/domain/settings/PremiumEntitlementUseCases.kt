package com.agc.bwitch.domain.settings

class RefreshPremiumEntitlementUseCase(
    private val repository: PremiumEntitlementRepository,
) {
    suspend operator fun invoke(force: Boolean): PremiumEntitlement = repository.refreshPremiumEntitlement(force)
}

class ValidateGooglePlayPurchaseUseCase(
    private val repository: PremiumEntitlementRepository,
) {
    suspend operator fun invoke(token: BillingPurchaseToken): PremiumEntitlement = repository.validateGooglePlayPurchase(token)
}

class RestoreGooglePlayPurchasesUseCase(
    private val repository: PremiumEntitlementRepository,
) {
    suspend operator fun invoke(tokens: List<BillingPurchaseToken>): PremiumRestoreResult = repository.restoreGooglePlayPurchases(tokens)
}
