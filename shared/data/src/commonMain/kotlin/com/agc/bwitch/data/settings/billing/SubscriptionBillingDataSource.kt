package com.agc.bwitch.data.settings.billing

import com.agc.bwitch.domain.settings.GooglePlayPurchase
import com.agc.bwitch.domain.settings.SubscriptionStatus
import com.agc.bwitch.domain.settings.SubscriptionPlan

interface SubscriptionBillingDataSource {
    val isSupported: Boolean

    suspend fun querySubscriptionStatus(): SubscriptionStatus

    suspend fun querySubscriptionCatalog(): List<SubscriptionPlan>

    suspend fun queryGooglePlayPurchases(): List<GooglePlayPurchase>

    suspend fun restoreSubscriptionStatus(): SubscriptionStatus
}

object UnsupportedSubscriptionBillingDataSource : SubscriptionBillingDataSource {
    override val isSupported: Boolean = false

    override suspend fun querySubscriptionStatus(): SubscriptionStatus = SubscriptionStatus.Unknown

    override suspend fun querySubscriptionCatalog(): List<SubscriptionPlan> = emptyList()

    override suspend fun queryGooglePlayPurchases(): List<GooglePlayPurchase> = emptyList()

    override suspend fun restoreSubscriptionStatus(): SubscriptionStatus = SubscriptionStatus.Unknown
}
