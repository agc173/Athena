package com.agc.bwitch.domain.astrology.horoscope

object ConstellationProgressRules {
    val zodiacOrder: List<ZodiacSign> = listOf(
        ZodiacSign.aries,
        ZodiacSign.taurus,
        ZodiacSign.gemini,
        ZodiacSign.cancer,
        ZodiacSign.leo,
        ZodiacSign.virgo,
        ZodiacSign.libra,
        ZodiacSign.scorpio,
        ZodiacSign.sagittarius,
        ZodiacSign.capricorn,
        ZodiacSign.aquarius,
        ZodiacSign.pisces,
    )

    val stepsBySign: Map<ZodiacSign, Int> = mapOf(
        ZodiacSign.aries to 9,
        ZodiacSign.taurus to 21,
        ZodiacSign.gemini to 17,
        ZodiacSign.cancer to 9,
        ZodiacSign.leo to 19,
        ZodiacSign.virgo to 25,
        ZodiacSign.libra to 15,
        ZodiacSign.scorpio to 19,
        ZodiacSign.sagittarius to 29,
        ZodiacSign.capricorn to 19,
        ZodiacSign.aquarius to 21,
        ZodiacSign.pisces to 23,
    )

    val maxTotalProgress: Int = zodiacOrder.sumOf { stepsBySign.getValue(it) }
}
