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


enum class EconomyNextSource {
    FREE,
    PREMIUM,
    MOON,
    REJECTED,
    NOT_CONFIGURED,
    COMING_SOON,
    UNKNOWN,
    RULE_CONFIGURED_NOT_WIRED,
}

data class EconomyModulePreview(
    val module: String,
    val nextSource: EconomyNextSource,
    val cost: Int,
    val balance: Int,
    val canExecute: Boolean,
    val reasonIfRejected: String? = null,
    val labelKey: String? = null,
    val uiHint: String? = null,
    val freeRemaining: Int? = null,
    val premiumRemaining: Int? = null,
    val moonRemaining: Int? = null,
    val moonPackUsesPerMoon: Int? = null,
    val dailyCap: Int? = null,
)
