package com.agc.bwitch.domain.astrology.horoscope

import kotlinx.serialization.Serializable

@Serializable
data class MonthlyHoroscope(
    val sign: ZodiacSign,
    val monthKey: String,
    val languageCode: String,
    val title: String,
    val monthTheme: String,
    val loveAndRelationships: String,
    val workAndMoney: String,
    val personalGrowth: String,
    val ritualSuggestion: String,
    val mantra: String,
    val shareText: String? = null,
    val updatedAtEpochMillis: Long = 0L,
)
