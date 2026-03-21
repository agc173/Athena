package com.agc.bwitch.domain.astrology.birthchart

import com.agc.bwitch.domain.astrology.horoscope.ZodiacSign

class BirthEssenceArchetypeResolver {

    fun resolve(
        sunSign: ZodiacSign,
        moonSign: ZodiacSign,
        risingSign: ZodiacSign,
    ): BirthEssenceArchetype {
        val scores = linkedMapOf(
            Element.FIRE to 0.0,
            Element.WATER to 0.0,
            Element.AIR to 0.0,
            Element.EARTH to 0.0,
        )

        scores.accumulate(sunSign.toElement(), SUN_WEIGHT)
        scores.accumulate(moonSign.toElement(), MOON_WEIGHT)
        scores.accumulate(risingSign.toElement(), RISING_WEIGHT)

        val ranking = scores.entries.sortedByDescending { it.value }
        val top1 = ranking[0]
        val top2 = ranking[1]

        if ((top1.value - top2.value) <= ALCHEMIST_DIFF_THRESHOLD) {
            return BirthEssenceArchetype.ALQUIMISTA
        }

        val waterAir = (scores[Element.WATER] ?: 0.0) + (scores[Element.AIR] ?: 0.0)
        val fire = scores[Element.FIRE] ?: 0.0
        if (waterAir >= MYSTIC_WATER_AIR_THRESHOLD && fire <= MYSTIC_FIRE_MAX) {
            return BirthEssenceArchetype.MISTICA
        }

        return when (top1.key) {
            Element.FIRE -> BirthEssenceArchetype.GUERRERA
            Element.WATER -> BirthEssenceArchetype.SANADORA
            Element.AIR -> BirthEssenceArchetype.VIDENTE
            Element.EARTH -> BirthEssenceArchetype.GUARDIANA
        }
    }

    private fun MutableMap<Element, Double>.accumulate(element: Element, weight: Double) {
        this[element] = (this[element] ?: 0.0) + weight
    }

    private fun ZodiacSign.toElement(): Element = when (this) {
        ZodiacSign.aries, ZodiacSign.leo, ZodiacSign.sagittarius -> Element.FIRE
        ZodiacSign.cancer, ZodiacSign.scorpio, ZodiacSign.pisces -> Element.WATER
        ZodiacSign.gemini, ZodiacSign.libra, ZodiacSign.aquarius -> Element.AIR
        ZodiacSign.taurus, ZodiacSign.virgo, ZodiacSign.capricorn -> Element.EARTH
    }

    private enum class Element { FIRE, WATER, AIR, EARTH }

    private companion object {
        const val SUN_WEIGHT = 0.50
        const val MOON_WEIGHT = 0.30
        const val RISING_WEIGHT = 0.20
        const val ALCHEMIST_DIFF_THRESHOLD = 0.10
        const val MYSTIC_WATER_AIR_THRESHOLD = 0.60
        const val MYSTIC_FIRE_MAX = 0.20
    }
}
