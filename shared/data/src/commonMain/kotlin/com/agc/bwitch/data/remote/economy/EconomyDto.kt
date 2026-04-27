package com.agc.bwitch.data.remote.economy

import kotlinx.serialization.Serializable

@Serializable
data class EconomyBalanceDto(
    val balance: Int = 0,
    val dailyLoginClaimed: Boolean = false,
    val rewardedAdsClaimed: Int = 0,
    val rewardedAdsRemaining: Int = 0,
)

/**
 * Mantener este DTO con tipos primitivos/objetos serializables básicos para compatibilidad
 * con GitLive FirebaseDecoder (no JsonObject/JsonElement).
 */
@Serializable
data class EconomyStatusDto(
    val balance: Int = 0,
    val premium: PremiumDto = PremiumDto(),
    val todayDateIso: String = "",
    val rules: EconomyRulesDto? = null,
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

@Serializable
data class EconomyRulesDto(
    val horoscope: HoroscopeRulesDto? = null,
)

@Serializable
data class HoroscopeRulesDto(
    val costs: HoroscopeCostsDto? = null,
)

@Serializable
data class HoroscopeCostsDto(
    val futureDay: Int? = null,
)

@Serializable
data class UnlockHoroscopeDayRequestDto(
    val requestId: String,
    val dateIso: String,
    val sign: String,
)

@Serializable
data class UnlockHoroscopeDayResponseDto(
    val result: String = "",
    val unlocked: Boolean = false,
    val alreadyUnlocked: Boolean = false,
    val balance: Int = 0,
    val costCharged: Int = 0,
)

@Serializable
data class GetHoroscopeDailyUnlocksRequestDto(
    val dateIsoList: List<String>,
)

@Serializable
data class GetHoroscopeDailyUnlocksResponseDto(
    val unlockedDateIsoList: List<String> = emptyList(),
)
