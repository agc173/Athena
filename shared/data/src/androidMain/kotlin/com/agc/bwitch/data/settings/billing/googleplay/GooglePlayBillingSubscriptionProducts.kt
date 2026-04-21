package com.agc.bwitch.data.settings.billing.googleplay

object GooglePlayBillingSubscriptionProducts {
    // TODO(product-catalog): reemplazar por los IDs reales de Play Console en el próximo corte.
    const val MONTHLY = "bwitch_subscription_monthly"
    const val ANNUAL = "bwitch_subscription_annual"

    val knownProducts: Set<String> = setOf(MONTHLY, ANNUAL)
}
