package com.agc.bwitch.domain.oracle

interface OracleRepository {
    suspend fun getStatus(): com.agc.bwitch.data.functions.ApiResult<SystemMode>
}
