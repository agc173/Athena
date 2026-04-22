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

    private companion object {
        const val GET_ECONOMY_BALANCE_CALLABLE = "getEconomyBalance"
        const val GET_ECONOMY_STATUS_CALLABLE = "getEconomyStatus"
    }
}

private fun com.agc.bwitch.domain.shared.ApiError.toException(): IllegalStateException {
    return IllegalStateException(message ?: "Economy backend request failed")
}
