package com.agc.bwitch.data.remote.economy

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject

@Serializable
data class EconomyBalanceDto(
    val balance: Int = 0,
    val dailyLoginClaimed: Boolean = false,
    val rewardedAdsClaimed: Int = 0,
    val rewardedAdsRemaining: Int = 0,
)

@Serializable
data class EconomyStatusDto(
    val balance: Int = 0,
    val premium: PremiumDto = PremiumDto(),
    val todayDateIso: String = "",
    val rules: JsonObject? = null,
)

@Serializable
data class PremiumDto(
    val isPremium: Boolean = false,
)

@Serializable
data class EconomyClaimDailyLoginRequestDto(
    val requestId: String,
)

@Serializable
data class EconomyClaimRewardedAdRequestDto(
    val requestId: String,
    val adProof: String,
    val placement: String? = null,
)

@Serializable
data class EconomyClaimResultDto(
    val result: String = "",
    val balance: Int = 0,
    val dailyLoginClaimed: Boolean = false,
    val rewardedAdsClaimed: Int = 0,
    val rewardedAdsRemaining: Int = 0,
)
