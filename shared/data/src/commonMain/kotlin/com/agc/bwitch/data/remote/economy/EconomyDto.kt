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
    val weekly: Int? = null,
    val monthly: Int? = null,
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

@Serializable
data class UnlockHoroscopeWeeklyRequestDto(
    val requestId: String,
    val weekKey: String,
    val sign: String,
)

@Serializable
data class GetHoroscopeWeeklyUnlocksRequestDto(
    val weekKeyList: List<String>,
)

@Serializable
data class GetHoroscopeWeeklyUnlocksResponseDto(
    val unlockedWeekKeyList: List<String> = emptyList(),
)

@Serializable
data class UnlockHoroscopeMonthlyRequestDto(
    val requestId: String,
    val monthKey: String,
    val sign: String,
)

@Serializable
data class GetHoroscopeMonthlyUnlocksRequestDto(
    val monthKeyList: List<String>,
)

@Serializable
data class GetHoroscopeMonthlyUnlocksResponseDto(
    val unlockedMonthKeyList: List<String> = emptyList(),
)


@Serializable
data class EconomyModulePreviewDto(
    val module: String = "",
    val nextSource: String = "UNKNOWN",
    val cost: Int = 0,
    val balance: Int = 0,
    val canExecute: Boolean = false,
    val reasonIfRejected: String? = null,
    val labelKey: String? = null,
    val uiHint: String? = null,
    val freeRemaining: Int? = null,
    val premiumRemaining: Int? = null,
    val moonRemaining: Int? = null,
    val moonPackUsesPerMoon: Int? = null,
    val dailyCap: Int? = null,
)

@Serializable
data class EconomyModulePreviewsResponseDto(
    val previews: List<EconomyModulePreviewDto> = emptyList(),
)


@Serializable
data class EconomyModulePreviewsRequestDto(
    val modules: List<String> = emptyList(),
)

@Serializable
data class SynastryAuthorizeRequestDto(
    val requestId: String,
    val languageCode: String? = null,
)

@Serializable
data class SynastryAuthorizeResponseDto(
    val authorized: Boolean = false,
    val economyDisabled: Boolean = false,
    val status: String? = null,
    val requestId: String? = null,
    val source: String? = null,
    val moonCost: Int = 0,
)

@Serializable
data class PendulumAuthorizeRequestDto(
    val requestId: String,
    val languageCode: String? = null,
)

@Serializable
data class PendulumAuthorizeResponseDto(
    val authorized: Boolean = false,
    val economyDisabled: Boolean = false,
    val status: String? = null,
    val requestId: String? = null,
    val source: String? = null,
    val moonCost: Int = 0,
)
