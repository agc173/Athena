package com.agc.bwitch.domain.tarot

import com.agc.bwitch.domain.shared.ApiResult

interface TarotRepository {
    suspend fun tarotDraw(
        requestId: String,
        type: TarotRequestType,
        // ISO-like app language code propagated to backend (es/en/pt/ru/fr/it/de).
        lang: String? = null,
        question: String? = null,
    ): ApiResult<TarotDrawResponse>
}

interface LastTarotReadingRepository {
    fun get(): TarotDrawResponse?
    fun save(response: TarotDrawResponse)
}
