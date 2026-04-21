package com.agc.bwitch.data.settings.billing.googleplay

import com.agc.bwitch.domain.settings.KnownSubscriptionProducts

object GooglePlayBillingSubscriptionProducts {
    const val MONTHLY = KnownSubscriptionProducts.MONTHLY
    const val ANNUAL = KnownSubscriptionProducts.ANNUAL

    val knownProducts: Set<String> = KnownSubscriptionProducts.all
}
