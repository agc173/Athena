package com.agc.bwitch.domain.settings

object KnownSubscriptionProducts {
    /**
     * IDs actuales de suscripción Android (Google Play Billing).
     * Reemplazar estos valores por los IDs reales de Play Console cuando estén listos.
     */
    const val MONTHLY = "bwitch_subscription_monthly"
    const val ANNUAL = "bwitch_subscription_annual"

    /**
     * Orden canónico para query/render (mensual -> anual).
     */
    val ordered: List<String> = listOf(MONTHLY, ANNUAL)

    val all: Set<String> = setOf(MONTHLY, ANNUAL)
}
