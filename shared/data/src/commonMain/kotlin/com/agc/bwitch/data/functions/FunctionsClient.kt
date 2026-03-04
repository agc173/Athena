package com.agc.bwitch.data.functions

interface FunctionsClient {
    suspend fun call(name: String, data: Map<String, Any?>? = null): ApiResult<Map<String, Any?>>
}
