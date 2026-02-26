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

    private val KEY_LAST_PULLED_DATE_ISO = "last_pulled_date_iso"

    override fun getLastPulledDateIso(): String? =
        settings[KEY_LAST_PULLED_DATE_ISO]

    override fun setLastPulledDateIso(dateIso: String) {
        settings[KEY_LAST_PULLED_DATE_ISO] = dateIso
    }
}