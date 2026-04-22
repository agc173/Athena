package com.agc.bwitch.domain.economy

data class EconomyBalance(
    val balance: Int,
    val dailyLoginClaimed: Boolean,
    val rewardedAdsClaimed: Int,
    val rewardedAdsRemaining: Int,
)

data class EconomyStatus(
    val balance: Int,
    val isPremium: Boolean,
    val todayDateIso: String,
)
