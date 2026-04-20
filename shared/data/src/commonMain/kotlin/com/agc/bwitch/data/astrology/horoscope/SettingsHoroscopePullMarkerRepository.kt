package com.agc.bwitch.data.astrology.horoscope

import com.agc.bwitch.data.storage.SettingsFactory
import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import com.agc.bwitch.domain.astrology.horoscope.HoroscopePullMarker

class SettingsHoroscopePullMarkerRepository(
    settingsFactory: SettingsFactory
) : HoroscopePullMarkerRepository, HoroscopePullMarker {

    private val settings: Settings = settingsFactory.create("horoscope_pull_marker")

    override fun getLastPulledDateIso(languageCode: String): String? =
        settings[key(languageCode)]

    override fun setLastPulledDateIso(dateIso: String, languageCode: String) {
        settings[key(languageCode)] = dateIso
    }

    private fun key(languageCode: String): String = "last_pulled_date_iso_${languageCode.lowercase()}"
}
