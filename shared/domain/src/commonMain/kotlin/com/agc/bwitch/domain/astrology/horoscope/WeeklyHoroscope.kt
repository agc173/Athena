package com.agc.bwitch.domain.astrology.horoscope

import kotlinx.serialization.Serializable

@Serializable
data class WeeklyHoroscope(
    val sign: ZodiacSign,
    val weekKey: String,
    val languageCode: String,
    val title: String,
    val overview: String,
    val loveAndRelationships: String,
    val workAndMoney: String,
    val spiritualEnergy: String,
    val weeklyAdvice: String,
    val mantra: String,
    val shareText: String? = null,
    val updatedAtEpochMillis: Long = 0L,
)
