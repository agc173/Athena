package com.agc.bwitch.domain.settings

enum class SubscriptionStatus {
    Unknown,
    Inactive,
    ActiveMonthly,
    ActiveAnnual,
}

val SubscriptionStatus.isActive: Boolean
    get() = this == SubscriptionStatus.ActiveMonthly || this == SubscriptionStatus.ActiveAnnual
