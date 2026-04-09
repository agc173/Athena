package com.agc.bwitch.domain.astrology.horoscope

import kotlinx.coroutines.flow.Flow

interface HoroscopeRepository {
    fun observeDaily(dateIso: String, sign: ZodiacSign, languageCode: String): Flow<DailyHoroscope?>
    suspend fun getDaily(dateIso: String, sign: ZodiacSign, languageCode: String): DailyHoroscope?
}
