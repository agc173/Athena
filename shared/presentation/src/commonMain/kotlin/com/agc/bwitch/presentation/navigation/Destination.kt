package com.agc.bwitch.presentation.navigation

import com.agc.bwitch.domain.astrology.horoscope.ZodiacSign

sealed class Destination(val title: String) {

    data object Portal : Destination("BWitch")
    data object Astrology : Destination("Astrología")


    data class HoroscopeDaily(
        val preselectedSign: ZodiacSign? = null
    ) : Destination("Horóscopo diario")
}


