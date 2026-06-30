package com.agc.bwitch.data.remote.economy

import com.agc.bwitch.data.functions.FunctionsClient
import com.agc.bwitch.domain.shared.ApiResult
import kotlinx.serialization.builtins.serializer

class EconomyRemoteDataSource(
    private val functionsClient: FunctionsClient,
) {
    suspend fun getBalance(): EconomyBalanceDto {
        return when (
            val result = functionsClient.call(
                name = GET_ECONOMY_BALANCE_CALLABLE,
                data = Unit,
                requestSerializer = Unit.serializer(),
                responseSerializer = EconomyBalanceDto.serializer(),
            )
        ) {
            is ApiResult.Ok -> result.value
            is ApiResult.Err -> throw result.error.toException()
        }
    }

    suspend fun getStatus(): EconomyStatusDto {
        return when (
            val result = functionsClient.call(
                name = GET_ECONOMY_STATUS_CALLABLE,
                data = Unit,
                requestSerializer = Unit.serializer(),
                responseSerializer = EconomyStatusDto.serializer(),
            )
        ) {
            is ApiResult.Ok -> result.value
            is ApiResult.Err -> throw result.error.toException()
        }
    }

    suspend fun claimDailyLogin(requestId: String): EconomyClaimResultDto {
        return when (
            val result = functionsClient.call(
                name = CLAIM_DAILY_LOGIN_CALLABLE,
                data = EconomyClaimDailyLoginRequestDto(requestId = requestId),
                requestSerializer = EconomyClaimDailyLoginRequestDto.serializer(),
                responseSerializer = EconomyClaimResultDto.serializer(),
            )
        ) {
            is ApiResult.Ok -> result.value
            is ApiResult.Err -> throw result.error.toException()
        }
    }

    suspend fun claimRewardedAd(
        requestId: String,
        adProof: String,
        placement: String?,
    ): EconomyClaimResultDto {
        return when (
            val result = functionsClient.call(
                name = CLAIM_REWARDED_AD_CALLABLE,
                data = EconomyClaimRewardedAdRequestDto(
                    requestId = requestId,
                    adProof = adProof,
                    placement = placement,
                ),
                requestSerializer = EconomyClaimRewardedAdRequestDto.serializer(),
                responseSerializer = EconomyClaimResultDto.serializer(),
            )
        ) {
            is ApiResult.Ok -> result.value
            is ApiResult.Err -> throw result.error.toException()
        }
    }



    suspend fun unlockHoroscopeDay(
        requestId: String,
        dateIso: String,
        sign: String,
    ): UnlockHoroscopeDayResponseDto {
        return when (
            val result = functionsClient.call(
                name = UNLOCK_HOROSCOPE_DAY_CALLABLE,
                data = UnlockHoroscopeDayRequestDto(
                    requestId = requestId,
                    dateIso = dateIso,
                    sign = sign,
                ),
                requestSerializer = UnlockHoroscopeDayRequestDto.serializer(),
                responseSerializer = UnlockHoroscopeDayResponseDto.serializer(),
            )
        ) {
            is ApiResult.Ok -> result.value
            is ApiResult.Err -> throw result.error.toException()
        }
    }

    suspend fun getHoroscopeDailyUnlocks(dateIsoList: List<String>): Set<String> {
        if (dateIsoList.isEmpty()) return emptySet()
        return when (
            val result = functionsClient.call(
                name = GET_HOROSCOPE_DAILY_UNLOCKS_CALLABLE,
                data = GetHoroscopeDailyUnlocksRequestDto(dateIsoList = dateIsoList),
                requestSerializer = GetHoroscopeDailyUnlocksRequestDto.serializer(),
                responseSerializer = GetHoroscopeDailyUnlocksResponseDto.serializer(),
            )
        ) {
            is ApiResult.Ok -> result.value.unlockedDateIsoList.toSet()
            is ApiResult.Err -> throw result.error.toException()
        }
    }

    suspend fun unlockHoroscopeWeek(
        requestId: String,
        weekKey: String,
        sign: String,
    ): UnlockHoroscopeWeeklyResponseDto {
        return when (
            val result = functionsClient.call(
                name = UNLOCK_HOROSCOPE_WEEKLY_CALLABLE,
                data = UnlockHoroscopeWeeklyRequestDto(
                    requestId = requestId,
                    weekKey = weekKey,
                    sign = sign,
                ),
                requestSerializer = UnlockHoroscopeWeeklyRequestDto.serializer(),
                responseSerializer = UnlockHoroscopeWeeklyResponseDto.serializer(),
            )
        ) {
            is ApiResult.Ok -> result.value
            is ApiResult.Err -> throw result.error.toException()
        }
    }

    suspend fun getHoroscopeWeeklyUnlocks(weekKeyList: List<String>): Set<String> {
        if (weekKeyList.isEmpty()) return emptySet()
        return when (
            val result = functionsClient.call(
                name = GET_HOROSCOPE_WEEKLY_UNLOCKS_CALLABLE,
                data = GetHoroscopeWeeklyUnlocksRequestDto(weekKeyList = weekKeyList),
                requestSerializer = GetHoroscopeWeeklyUnlocksRequestDto.serializer(),
                responseSerializer = GetHoroscopeWeeklyUnlocksResponseDto.serializer(),
            )
        ) {
            is ApiResult.Ok -> result.value.unlockedWeekKeyList.toSet()
            is ApiResult.Err -> throw result.error.toException()
        }
    }

    suspend fun unlockHoroscopeMonth(
        requestId: String,
        monthKey: String,
        sign: String,
    ): UnlockHoroscopeMonthlyResponseDto {
        return when (
            val result = functionsClient.call(
                name = UNLOCK_HOROSCOPE_MONTHLY_CALLABLE,
                data = UnlockHoroscopeMonthlyRequestDto(
                    requestId = requestId,
                    monthKey = monthKey,
                    sign = sign,
                ),
                requestSerializer = UnlockHoroscopeMonthlyRequestDto.serializer(),
                responseSerializer = UnlockHoroscopeMonthlyResponseDto.serializer(),
            )
        ) {
            is ApiResult.Ok -> result.value
            is ApiResult.Err -> throw result.error.toException()
        }
    }

    suspend fun getHoroscopeMonthlyUnlocks(monthKeyList: List<String>): Set<String> {
        if (monthKeyList.isEmpty()) return emptySet()
        return when (
            val result = functionsClient.call(
                name = GET_HOROSCOPE_MONTHLY_UNLOCKS_CALLABLE,
                data = GetHoroscopeMonthlyUnlocksRequestDto(monthKeyList = monthKeyList),
                requestSerializer = GetHoroscopeMonthlyUnlocksRequestDto.serializer(),
                responseSerializer = GetHoroscopeMonthlyUnlocksResponseDto.serializer(),
            )
        ) {
            is ApiResult.Ok -> result.value.unlockedMonthKeyList.toSet()
            is ApiResult.Err -> throw result.error.toException()
        }
    }



    suspend fun getModulePreviews(modules: List<String>): List<EconomyModulePreviewDto> {
        return when (
            val result = functionsClient.call(
                name = GET_ECONOMY_MODULE_PREVIEWS_CALLABLE,
                data = EconomyModulePreviewsRequestDto(modules = modules),
                requestSerializer = EconomyModulePreviewsRequestDto.serializer(),
                responseSerializer = EconomyModulePreviewsResponseDto.serializer(),
            )
        ) {
            is ApiResult.Ok -> result.value.previews
            is ApiResult.Err -> throw result.error.toException()
        }
    }

    suspend fun authorizeSynastry(requestId: String, languageCode: String?): SynastryAuthorizeResponseDto {
        return when (
            val result = functionsClient.call(
                name = SYNASTRY_AUTHORIZE_CALLABLE,
                data = SynastryAuthorizeRequestDto(requestId = requestId, languageCode = languageCode),
                requestSerializer = SynastryAuthorizeRequestDto.serializer(),
                responseSerializer = SynastryAuthorizeResponseDto.serializer(),
            )
        ) {
            is ApiResult.Ok -> result.value
            is ApiResult.Err -> throw result.error.toException()
        }
    }


    suspend fun authorizeBasicNatal(requestId: String, languageCode: String?): BasicNatalAuthorizeResponseDto {
        return when (
            val result = functionsClient.call(
                name = BASIC_NATAL_AUTHORIZE_CALLABLE,
                data = BasicNatalAuthorizeRequestDto(requestId = requestId, languageCode = languageCode),
                requestSerializer = BasicNatalAuthorizeRequestDto.serializer(),
                responseSerializer = BasicNatalAuthorizeResponseDto.serializer(),
            )
        ) {
            is ApiResult.Ok -> result.value
            is ApiResult.Err -> throw result.error.toException()
        }
    }


    suspend fun authorizePendulum(requestId: String, languageCode: String?): PendulumAuthorizeResponseDto {
        return when (
            val result = functionsClient.call(
                name = PENDULUM_AUTHORIZE_CALLABLE,
                data = PendulumAuthorizeRequestDto(requestId = requestId, languageCode = languageCode),
                requestSerializer = PendulumAuthorizeRequestDto.serializer(),
                responseSerializer = PendulumAuthorizeResponseDto.serializer(),
            )
        ) {
            is ApiResult.Ok -> result.value
            is ApiResult.Err -> throw result.error.toException()
        }
    }

    private companion object {
        const val GET_ECONOMY_BALANCE_CALLABLE = "getEconomyBalance"
        const val GET_ECONOMY_STATUS_CALLABLE = "getEconomyStatus"
        const val CLAIM_DAILY_LOGIN_CALLABLE = "claimDailyLogin"
        const val CLAIM_REWARDED_AD_CALLABLE = "claimRewardedAd"
        const val UNLOCK_HOROSCOPE_DAY_CALLABLE = "unlockHoroscopeDay"
        const val GET_HOROSCOPE_DAILY_UNLOCKS_CALLABLE = "getHoroscopeDailyUnlocks"
        const val UNLOCK_HOROSCOPE_WEEKLY_CALLABLE = "unlockHoroscopeWeekly"
        const val GET_HOROSCOPE_WEEKLY_UNLOCKS_CALLABLE = "getHoroscopeWeeklyUnlocks"
        const val UNLOCK_HOROSCOPE_MONTHLY_CALLABLE = "unlockHoroscopeMonthly"
        const val GET_HOROSCOPE_MONTHLY_UNLOCKS_CALLABLE = "getHoroscopeMonthlyUnlocks"
        const val GET_ECONOMY_MODULE_PREVIEWS_CALLABLE = "getEconomyModulePreviews"
        const val SYNASTRY_AUTHORIZE_CALLABLE = "synastryAuthorize"
        const val PENDULUM_AUTHORIZE_CALLABLE = "pendulumAuthorize"
        const val BASIC_NATAL_AUTHORIZE_CALLABLE = "basicNatalAuthorize"
    }
}

private fun com.agc.bwitch.domain.shared.ApiError.toException(): IllegalStateException {
    val code = when (this) {
        is com.agc.bwitch.domain.shared.ApiError.Unauthenticated -> "unauthenticated"
        is com.agc.bwitch.domain.shared.ApiError.PermissionDenied -> "permission_denied"
        is com.agc.bwitch.domain.shared.ApiError.ResourceExhausted -> "resource_exhausted"
        is com.agc.bwitch.domain.shared.ApiError.FailedPrecondition -> "failed_precondition"
        is com.agc.bwitch.domain.shared.ApiError.InvalidArgument -> "invalid_argument"
        is com.agc.bwitch.domain.shared.ApiError.NotFound -> "not_found"
        is com.agc.bwitch.domain.shared.ApiError.Internal -> "internal"
        is com.agc.bwitch.domain.shared.ApiError.Network -> "network"
        is com.agc.bwitch.domain.shared.ApiError.Unknown -> "unknown"
    }
    val backendMessage = message?.takeIf { it.isNotBlank() } ?: "Economy backend request failed"
    println("[EconomyRemoteDataSource] backend_error code=$code message=$backendMessage")
    return EconomyBackendException(code = code, backendMessage = backendMessage)
}

class EconomyBackendException(
    val code: String,
    val backendMessage: String,
) : IllegalStateException("$code:$backendMessage")
