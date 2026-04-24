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

    private companion object {
        const val GET_ECONOMY_BALANCE_CALLABLE = "getEconomyBalance"
        const val GET_ECONOMY_STATUS_CALLABLE = "getEconomyStatus"
        const val CLAIM_DAILY_LOGIN_CALLABLE = "claimDailyLogin"
        const val CLAIM_REWARDED_AD_CALLABLE = "claimRewardedAd"
        const val UNLOCK_HOROSCOPE_DAY_CALLABLE = "unlockHoroscopeDay"
    }
}

private fun com.agc.bwitch.domain.shared.ApiError.toException(): IllegalStateException {
    return IllegalStateException(message ?: "Economy backend request failed")
}
