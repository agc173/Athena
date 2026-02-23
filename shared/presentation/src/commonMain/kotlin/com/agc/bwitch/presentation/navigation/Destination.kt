package com.agc.bwitch.presentation.navigation

import com.agc.bwitch.domain.astrology.horoscope.ZodiacSign

sealed class Destination(val title: String) {

    /**
     * Root gates
     */
    data object AuthGate : Destination("")

    /**
     * Main app destinations
     */
    data object Portal : Destination("BWitch")

    data object Astrology : Destination("Astrología")

    data object BirthChart : Destination("Carta astral")

    /**
     * Próximo módulo
     */
    data object UserProfile : Destination("Perfil")

    /**
     * Feature destinations with params
     */
    data class HoroscopeDaily(
        val preselectedSign: ZodiacSign? = null
    ) : Destination("Horóscopo diario")
}


