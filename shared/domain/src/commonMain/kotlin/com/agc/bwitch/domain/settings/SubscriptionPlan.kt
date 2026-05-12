package com.agc.bwitch.domain.settings

enum class SubscriptionPlanType {
    Monthly,
    Annual,
    Unknown,
}

data class SubscriptionPlan(
    val productId: String,
    val title: String,
    val formattedPrice: String,
    val type: SubscriptionPlanType,
    val basePlanId: String? = null,
)
