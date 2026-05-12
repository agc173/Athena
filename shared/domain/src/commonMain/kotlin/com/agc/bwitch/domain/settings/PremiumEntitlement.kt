package com.agc.bwitch.domain.settings

data class PremiumEntitlement(
    val isActive: Boolean,
    val status: SubscriptionStatus,
)
