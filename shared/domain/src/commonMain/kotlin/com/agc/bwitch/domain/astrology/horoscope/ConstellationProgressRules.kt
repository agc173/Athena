package com.agc.bwitch.domain.astrology.horoscope

object ConstellationProgressRules {
    val zodiacOrder: List<String> = listOf(
        "Aries",
        "Taurus",
        "Gemini",
        "Cancer",
        "Leo",
        "Virgo",
        "Libra",
        "Scorpio",
        "Sagittarius",
        "Capricorn",
        "Aquarius",
        "Pisces",
    )

    // Must stay in sync with ZodiacStylizedTemplates totalSteps in composeApp.
    val stepsBySign: Map<String, Int> = mapOf(
        "Aries" to 9,
        "Taurus" to 21,
        "Gemini" to 17,
        "Cancer" to 9,
        "Leo" to 19,
        "Virgo" to 25,
        "Libra" to 15,
        "Scorpio" to 19,
        "Sagittarius" to 29,
        "Capricorn" to 19,
        "Aquarius" to 21,
        "Pisces" to 23,
    )

    val maxTotalProgress: Int = zodiacOrder.sumOf { stepsBySign.getValue(it) }
}
