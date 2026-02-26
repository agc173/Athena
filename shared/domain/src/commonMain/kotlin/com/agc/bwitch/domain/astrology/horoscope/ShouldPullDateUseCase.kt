package com.agc.bwitch.domain.astrology.horoscope

class ShouldPullDateUseCase(
    private val marker: HoroscopePullMarker,
) {
    operator fun invoke(dateIso: String): Boolean {
        val last = marker.getLastPulledDateIso()
        return last != dateIso
    }
}