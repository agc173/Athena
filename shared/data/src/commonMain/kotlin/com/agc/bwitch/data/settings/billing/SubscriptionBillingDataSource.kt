package com.agc.bwitch.data.settings.billing

import com.agc.bwitch.domain.settings.BillingProduct
import com.agc.bwitch.domain.settings.BillingPurchaseToken

interface SubscriptionBillingDataSource {
    val isSupported: Boolean

    suspend fun getProducts(): List<BillingProduct>

    suspend fun queryRestorablePurchases(): List<BillingPurchaseToken>
}

object UnsupportedSubscriptionBillingDataSource : SubscriptionBillingDataSource {
    override val isSupported: Boolean = false

    override suspend fun getProducts(): List<BillingProduct> = emptyList()

    override suspend fun queryRestorablePurchases(): List<BillingPurchaseToken> = emptyList()
}
