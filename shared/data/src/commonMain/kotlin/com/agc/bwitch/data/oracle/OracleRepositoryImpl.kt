package com.agc.bwitch.data.oracle

import com.agc.bwitch.domain.shared.ApiError
import com.agc.bwitch.domain.shared.ApiResult
import com.agc.bwitch.data.functions.FunctionsClient
import com.agc.bwitch.domain.oracle.OracleRepository
import com.agc.bwitch.domain.oracle.SystemMode
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer

class OracleRepositoryImpl(
    private val functionsClient: FunctionsClient,
) : OracleRepository {

    override suspend fun getStatus(): ApiResult<SystemMode> {
        return when (
            val result = functionsClient.call<Unit, OracleStatusResponse>(
                name = "oracleGetStatus",
                data = Unit,
                requestSerializer = Unit.serializer(),
                responseSerializer = OracleStatusResponse.serializer(),
            )
        ) {
            is ApiResult.Err -> result
            is ApiResult.Ok -> {
                val modeRaw = result.value.mode

                val mode = runCatching { SystemMode.valueOf(modeRaw) }.getOrNull()
                    ?: return ApiResult.Err(ApiError.Internal("Invalid response: mode=$modeRaw"))

                ApiResult.Ok(mode)
            }
        }
    }

    @Serializable
    private data class OracleStatusResponse(
        val mode: String,
    )
}
