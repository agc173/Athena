package com.agc.bwitch.domain.settings

object KnownSubscriptionProducts {
    /**
     * ID real de la suscripción mensual Premium v1 en Google Play Console.
     */
    const val MONTHLY = "bwitch_premium_monthly"

    /**
     * Reservado para una fase posterior. No se consulta ni se muestra en v1.
     */
    const val ANNUAL = "bwitch_subscription_annual"

    /**
     * Catálogo visible v1: solo mensual.
     */
    val ordered: List<String> = listOf(MONTHLY)

    val all: Set<String> = setOf(MONTHLY)
}
