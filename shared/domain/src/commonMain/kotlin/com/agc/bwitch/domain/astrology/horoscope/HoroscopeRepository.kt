package com.agc.bwitch.domain.astrology.horoscope

import kotlinx.coroutines.flow.Flow

interface HoroscopeRepository {
    fun observeDaily(dateIso: String, sign: ZodiacSign, languageCode: String): Flow<DailyHoroscope?>
    suspend fun getDaily(dateIso: String, sign: ZodiacSign, languageCode: String): DailyHoroscope?
    fun observeWeekly(weekKey: String, sign: ZodiacSign, languageCode: String): Flow<WeeklyHoroscope?>
    suspend fun getWeekly(weekKey: String, sign: ZodiacSign, languageCode: String): WeeklyHoroscope?
    fun observeMonthly(monthKey: String, sign: ZodiacSign, languageCode: String): Flow<MonthlyHoroscope?>
    suspend fun getMonthly(monthKey: String, sign: ZodiacSign, languageCode: String): MonthlyHoroscope?
}
