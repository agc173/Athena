package com.agc.bwitch.presentation.navigation

import com.agc.bwitch.domain.astrology.horoscope.ZodiacSign
import com.agc.bwitch.domain.rituals.RitualCategoryType
import com.agc.bwitch.domain.tarot.TarotRequestType

sealed class Destination(val title: String) {

    /**
     * Root gates
     */
    data object AuthGate : Destination("")

    data object OnboardingProfile : Destination("Completa tu perfil")

    /**
     * Main app destinations
     */
    data object Astrology : Destination("Astrología")

    data object BirthChart : Destination("Esencia natal")

    data object Synastry : Destination("Sinastría")

    /**
     * Próximo módulo
     */
    data object UserProfile : Destination("Perfil")

    data object Settings : Destination("Ajustes")

    data object Oracle : Destination("Oráculo")

    data object OracleDebug : Destination("Oracle debug")

    data object Guide : Destination("Guía")

    data object Rituals : Destination("Rituales")
    data class RitualsList(val category: RitualCategoryType) : Destination("Rituales")
    data class RitualDetail(val ritualId: String) : Destination("Ritual")
    data object DailyRitual : Destination("Ritual del día")

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
