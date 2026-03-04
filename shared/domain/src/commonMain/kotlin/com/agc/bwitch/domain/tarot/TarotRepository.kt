package com.agc.bwitch.domain.tarot

import com.agc.bwitch.domain.shared.ApiResult

interface TarotRepository {
    suspend fun tarotDraw(
        requestId: String,
        type: TarotRequestType,
        lang: String? = null,
        question: String? = null,
    ): ApiResult<TarotDrawResponse>
}
