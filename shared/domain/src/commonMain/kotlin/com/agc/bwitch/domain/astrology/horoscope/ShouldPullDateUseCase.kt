package com.agc.bwitch.domain.astrology.horoscope

class ShouldPullDateUseCase(
    private val marker: HoroscopePullMarker,
) {
    operator fun invoke(dateIso: String, languageCode: String): Boolean {
        val last = marker.getLastPulledDateIso(languageCode = languageCode)
        return last != dateIso
    }
}
