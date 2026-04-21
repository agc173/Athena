package com.agc.bwitch.domain.settings

class GetSubscriptionCatalogUseCase(
    private val repository: SubscriptionRepository,
) {
    suspend operator fun invoke(): List<SubscriptionPlan> = repository.getCatalog()
}
