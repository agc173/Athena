package com.agc.bwitch.data.astrology.horoscope

interface HoroscopePullMarkerRepository {
    fun getLastPulledDateIso(): String?
    fun setLastPulledDateIso(dateIso: String)
}