package com.agc.bwitch.data.astrology.horoscope

interface HoroscopePullMarkerRepository {
    fun getLastPulledDateIso(languageCode: String): String?
    fun setLastPulledDateIso(dateIso: String, languageCode: String)
}
