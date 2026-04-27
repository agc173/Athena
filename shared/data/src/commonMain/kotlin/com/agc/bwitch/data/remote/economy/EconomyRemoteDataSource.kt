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
    ): UnlockHoroscopeDayResponseDto {
        return when (
            val result = functionsClient.call(
                name = UNLOCK_HOROSCOPE_WEEKLY_CALLABLE,
                data = UnlockHoroscopeWeeklyRequestDto(
                    requestId = requestId,
                    weekKey = weekKey,
                    sign = sign,
                ),
                requestSerializer = UnlockHoroscopeWeeklyRequestDto.serializer(),
                responseSerializer = UnlockHoroscopeDayResponseDto.serializer(),
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
    ): UnlockHoroscopeDayResponseDto {
        return when (
            val result = functionsClient.call(
                name = UNLOCK_HOROSCOPE_MONTHLY_CALLABLE,
                data = UnlockHoroscopeMonthlyRequestDto(
                    requestId = requestId,
                    monthKey = monthKey,
                    sign = sign,
                ),
                requestSerializer = UnlockHoroscopeMonthlyRequestDto.serializer(),
                responseSerializer = UnlockHoroscopeDayResponseDto.serializer(),
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
    }
}

private fun com.agc.bwitch.domain.shared.ApiError.toException(): IllegalStateException {
    return IllegalStateException(message ?: "Economy backend request failed")
}
