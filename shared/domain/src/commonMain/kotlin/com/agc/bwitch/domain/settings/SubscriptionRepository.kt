package com.agc.bwitch.domain.settings

import kotlinx.coroutines.flow.Flow

interface SubscriptionRepository {
    suspend fun getStatus(): SubscriptionStatus
    fun observeStatus(): Flow<SubscriptionStatus>
    suspend fun restorePurchases(): RestorePurchasesResult
}
