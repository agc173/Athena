package com.agc.bwitch.domain.astrology.horoscope

interface HoroscopePullMarker {
    fun getLastPulledDateIso(): String?
    fun setLastPulledDateIso(dateIso: String)
}