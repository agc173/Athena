package com.agc.bwitch.data.functions

import com.agc.bwitch.domain.shared.ApiResult

interface FunctionsClient {
    suspend fun call(name: String, data: Map<String, Any?>? = null): ApiResult<Map<String, Any?>>
}
