package com.agc.bwitch.domain.astrology.horoscope

import com.agc.bwitch.domain.model.ApiResult

interface HoroscopeRepository {
    suspend fun getDaily(sign: ZodiacSign, dateIso: String? = null): ApiResult<DailyHoroscope>
}
