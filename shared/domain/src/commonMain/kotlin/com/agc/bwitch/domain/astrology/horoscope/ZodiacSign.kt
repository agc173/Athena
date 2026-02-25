package com.agc.bwitch.domain.astrology.horoscope

import kotlinx.serialization.Serializable

@Serializable
enum class ZodiacSign {
    aries,
    taurus,
    gemini,
    cancer,
    leo,
    virgo,
    libra,
    scorpio,
    sagittarius,
    capricorn,
    aquarius,
    pisces;

    val label: String
        get() = name.replaceFirstChar { it.titlecase() }
}
