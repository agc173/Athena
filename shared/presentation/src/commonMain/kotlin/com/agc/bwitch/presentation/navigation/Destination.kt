package com.agc.bwitch.presentation.navigation

import com.agc.bwitch.domain.astrology.horoscope.ZodiacSign
import com.agc.bwitch.domain.tarot.TarotRequestType

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

    data object Oracle : Destination("Oráculo")

    data object OracleDebug : Destination("Oracle debug")

    data object Guide : Destination("Guía")

    data object TarotHome : Destination("Tarot")

    data class Tarot(
        val requestType: TarotRequestType? = null,
    ) : Destination("Tarot")

    data object Pendulum : Destination("El Péndulo")

    /**
     * Feature destinations with params
     */
    data class HoroscopeDaily(
        val preselectedSign: ZodiacSign? = null
    ) : Destination("Horóscopo diario")
}

