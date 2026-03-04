package com.agc.bwitch.domain.tarot

import com.agc.bwitch.data.functions.ApiResult

interface TarotRepository {
    suspend fun tarotDraw(
        requestId: String,
        type: TarotRequestType,
        lang: String? = null,
        question: String? = null,
    ): ApiResult<TarotDrawResponse>
}
