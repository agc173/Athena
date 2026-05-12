package com.agc.bwitch.domain.settings

class RefreshPremiumEntitlementUseCase(
    private val repository: PremiumEntitlementRepository,
) {
    suspend operator fun invoke(): PremiumEntitlement = repository.refreshEntitlement()
}
