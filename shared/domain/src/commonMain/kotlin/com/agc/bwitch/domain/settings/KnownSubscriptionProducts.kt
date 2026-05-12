package com.agc.bwitch.domain.settings

object KnownSubscriptionProducts {
    /**
     * ID real de suscripción mensual Android (Google Play Billing) para internal testing sandbox.
     */
    const val MONTHLY = "bwitch_premium_monthly"
    const val MONTHLY_BASE_PLAN_ID = "monthly"

    /** Reservado para una fase futura; no se consulta ni se renderiza en esta integración mensual. */
    const val ANNUAL = "bwitch_premium_annual_reserved"

    /**
     * Orden canónico para query/render de la fase sandbox actual: solo mensual.
     */
    val ordered: List<String> = listOf(MONTHLY)

    val all: Set<String> = setOf(MONTHLY)
}
