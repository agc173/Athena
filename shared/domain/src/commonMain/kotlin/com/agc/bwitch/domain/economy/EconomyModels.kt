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

data class EconomyClaimResult(
    val result: EconomyClaimStatus,
    val balance: Int,
    val dailyLoginClaimed: Boolean,
    val rewardedAdsClaimed: Int,
    val rewardedAdsRemaining: Int,
)

enum class EconomyClaimStatus {
    CLAIMED,
    ALREADY_CLAIMED,
    DAILY_LIMIT_REACHED,
}
