package com.agc.bwitch.domain.astrology.horoscope

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
