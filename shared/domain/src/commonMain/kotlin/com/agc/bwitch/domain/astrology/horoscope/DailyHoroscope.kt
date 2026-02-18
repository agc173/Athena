package com.agc.bwitch.domain.astrology.horoscope

data class DailyHoroscope(
    val sign: ZodiacSign,
    val dateIso: String,
    val text: String,
    val mood: String,
    val luckyNumber: Int,
    val luckyColor: String,
    val shareText: String? = null,
)
