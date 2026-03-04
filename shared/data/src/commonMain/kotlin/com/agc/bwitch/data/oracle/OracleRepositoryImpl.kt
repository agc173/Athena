package com.agc.bwitch.data.oracle

import com.agc.bwitch.domain.shared.ApiError
import com.agc.bwitch.domain.shared.ApiResult
import com.agc.bwitch.data.functions.FunctionsClient
import com.agc.bwitch.domain.oracle.OracleRepository
import com.agc.bwitch.domain.oracle.SystemMode

class OracleRepositoryImpl(
    private val functionsClient: FunctionsClient,
) : OracleRepository {

    override suspend fun getStatus(): ApiResult<SystemMode> {
        return when (val result = functionsClient.call("oracleGetStatus", null)) {
            is ApiResult.Err -> result
            is ApiResult.Ok -> {
                val modeRaw = result.value["mode"] as? String
                    ?: return ApiResult.Err(ApiError.Internal("Invalid response: missing mode"))

                val mode = runCatching { SystemMode.valueOf(modeRaw) }.getOrNull()
                    ?: return ApiResult.Err(ApiError.Internal("Invalid response: mode=$modeRaw"))

                ApiResult.Ok(mode)
            }
        }
    }
}
