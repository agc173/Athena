package com.agc.bwitch.data.functions

import com.agc.bwitch.domain.shared.ApiResult
import kotlinx.serialization.json.JsonObject

interface FunctionsClient {
    suspend fun call(name: String, data: JsonObject? = null): ApiResult<JsonObject>
}
