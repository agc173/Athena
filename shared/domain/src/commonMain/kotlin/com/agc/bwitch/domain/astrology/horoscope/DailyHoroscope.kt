package com.agc.bwitch.domain.astrology.horoscope

import kotlinx.serialization.Serializable

@Serializable
data class DailyHoroscope(
    val sign: ZodiacSign,
    val dateIso: String,
    val text: String,
    val mood: String,
    val luckyNumber: Int,
    val luckyColor: String,
    val shareText: String? = null,
    val updatedAtEpochMillis: Long = 0L,
)
