package com.agc.bwitch.domain.astrology.horoscope

interface HoroscopeDailySyncController {
    suspend fun pull(dateIso: String, languageCode: String)
}
