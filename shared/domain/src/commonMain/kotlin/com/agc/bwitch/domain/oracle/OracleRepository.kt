package com.agc.bwitch.domain.oracle

import com.agc.bwitch.domain.shared.ApiResult

interface OracleRepository {
    suspend fun getStatus(): ApiResult<SystemMode>
}
