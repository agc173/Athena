package com.agc.bwitch.data.settings.billing

import com.agc.bwitch.domain.settings.SubscriptionStatus

interface SubscriptionBillingDataSource {
    val isSupported: Boolean

    suspend fun querySubscriptionStatus(): SubscriptionStatus

    suspend fun restoreSubscriptionStatus(): SubscriptionStatus
}

object UnsupportedSubscriptionBillingDataSource : SubscriptionBillingDataSource {
    override val isSupported: Boolean = false

    override suspend fun querySubscriptionStatus(): SubscriptionStatus = SubscriptionStatus.Unknown

    override suspend fun restoreSubscriptionStatus(): SubscriptionStatus = SubscriptionStatus.Unknown
}
