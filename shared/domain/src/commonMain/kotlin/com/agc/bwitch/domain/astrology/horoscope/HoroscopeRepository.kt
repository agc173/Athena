package com.agc.bwitch.domain.astrology.horoscope

import kotlinx.coroutines.flow.Flow

interface HoroscopeRepository {
    fun observeDaily(dateIso: String, sign: ZodiacSign): Flow<DailyHoroscope?>
    suspend fun getDaily(dateIso: String, sign: ZodiacSign): DailyHoroscope?
}
