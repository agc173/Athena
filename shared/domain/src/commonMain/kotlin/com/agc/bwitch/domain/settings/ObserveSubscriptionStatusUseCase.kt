package com.agc.bwitch.domain.settings

import kotlinx.coroutines.flow.Flow

class ObserveSubscriptionStatusUseCase(
    private val repository: SubscriptionRepository,
) {
    operator fun invoke(): Flow<SubscriptionStatus> = repository.observeStatus()
}
