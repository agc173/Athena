package com.agc.bwitch.domain.astrology.horoscope

interface HoroscopePullMarker {
    fun getLastPulledDateIso(languageCode: String): String?
    fun setLastPulledDateIso(dateIso: String, languageCode: String)
}
