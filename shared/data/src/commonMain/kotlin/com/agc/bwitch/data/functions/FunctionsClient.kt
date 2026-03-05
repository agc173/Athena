package com.agc.bwitch.data.functions

import com.agc.bwitch.domain.shared.ApiResult
import kotlinx.serialization.KSerializer

interface FunctionsClient {
    suspend fun <Req : Any, Res : Any> call(
        name: String,
        data: Req? = null,
        responseSerializer: KSerializer<Res>,
    ): ApiResult<Res>
}
