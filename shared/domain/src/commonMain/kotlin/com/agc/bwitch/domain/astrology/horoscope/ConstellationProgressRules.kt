package com.agc.bwitch.domain.astrology.horoscope

object ConstellationProgressRules {
    val zodiacOrder: List<ZodiacSign> = listOf(
        ZodiacSign.Aries,
        ZodiacSign.Taurus,
        ZodiacSign.Gemini,
        ZodiacSign.Cancer,
        ZodiacSign.Leo,
        ZodiacSign.Virgo,
        ZodiacSign.Libra,
        ZodiacSign.Scorpio,
        ZodiacSign.Sagittarius,
        ZodiacSign.Capricorn,
        ZodiacSign.Aquarius,
        ZodiacSign.Pisces,
    )

    // Fuente única para cap global de progreso y reparto secuencial.
    // Debe mantenerse sincronizado con los revealSteps reales de las geometrías en composeApp.
    val stepsBySign: Map<ZodiacSign, Int> = mapOf(
        ZodiacSign.Aries to 9,
        ZodiacSign.Taurus to 21,
        ZodiacSign.Gemini to 17,
        ZodiacSign.Cancer to 9,
        ZodiacSign.Leo to 19,
        ZodiacSign.Virgo to 25,
        ZodiacSign.Libra to 15,
        ZodiacSign.Scorpio to 19,
        ZodiacSign.Sagittarius to 29,
        ZodiacSign.Capricorn to 19,
        ZodiacSign.Aquarius to 21,
        ZodiacSign.Pisces to 23,
    )

    val maxTotalProgress: Int = zodiacOrder.sumOf { stepsBySign[it] ?: 0 }
}
