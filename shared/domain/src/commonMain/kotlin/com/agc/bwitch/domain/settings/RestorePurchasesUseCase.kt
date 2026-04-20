package com.agc.bwitch.domain.settings

class RestorePurchasesUseCase(
    private val repository: SubscriptionRepository,
) {
    suspend operator fun invoke(): RestorePurchasesResult = repository.restorePurchases()
}
