package com.agc.bwitch.domain.settings

object KnownSubscriptionProducts {
    const val MONTHLY = "bwitch_subscription_monthly"
    const val ANNUAL = "bwitch_subscription_annual"

    val all: Set<String> = setOf(MONTHLY, ANNUAL)
}
