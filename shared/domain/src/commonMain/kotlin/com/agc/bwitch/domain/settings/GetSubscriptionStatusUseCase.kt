package com.agc.bwitch.domain.settings

class GetSubscriptionStatusUseCase(
    private val repository: SubscriptionRepository,
) {
    suspend operator fun invoke(): SubscriptionStatus = repository.getStatus()
}
