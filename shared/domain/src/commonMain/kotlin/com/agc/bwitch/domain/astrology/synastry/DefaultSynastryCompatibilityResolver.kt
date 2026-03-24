package com.agc.bwitch.domain.astrology.synastry

import com.agc.bwitch.domain.astrology.horoscope.ZodiacSign
import kotlin.math.abs
import kotlin.math.roundToInt

interface SynastryCompatibilityResolver {
    fun resolve(input: SynastryInput): SynastryReadingStructured
}

class DefaultSynastryCompatibilityResolver : SynastryCompatibilityResolver {

    override fun resolve(input: SynastryInput): SynastryReadingStructured {
        val completeness = buildCompleteness(input)
        val baseScores = linkedMapOf(
            SynastryDimension.EMOTIONAL to emotionalScore(input),
            SynastryDimension.COMMUNICATION to communicationScore(input),
            SynastryDimension.ATTRACTION to attractionScore(input),
            SynastryDimension.STABILITY to stabilityScore(input),
            SynastryDimension.GROWTH to growthScore(input),
        )
        val scores = applySolarProfileContrast(input, baseScores)

        val overallScore = SynastryScore.from(scores.values.map { it.value }.average().roundToInt())
        val archetype = resolveArchetype(scores, overallScore)

        val strengths = resolveStrengths(scores)
        val tensions = resolveTensions(scores)
        val guidance = resolveGuidance(tensions)
        val tags = resolveTags(scores, overallScore, completeness.depth)

        return SynastryReadingStructured(
            depthInfo = completeness,
            confidenceLevel = confidenceFor(completeness.depth),
            archetype = archetype,
            overallScore = overallScore,
            scores = scores,
            strengths = strengths,
            tensions = tensions,
            guidance = guidance,
            tags = tags,
        )
    }

    private fun applySolarProfileContrast(
        input: SynastryInput,
        baseScores: Map<SynastryDimension, SynastryScore>,
    ): Map<SynastryDimension, SynastryScore> {
        val profile = resolveSolarProfile(input.personA.sunSign, input.personB.sunSign)
        return linkedMapOf<SynastryDimension, SynastryScore>().apply {
            SynastryDimension.entries.forEach { dimension ->
                val base = baseScores.require(dimension).value
                val stretched = stretchContrast(base)
                val adjusted = stretched + profile.offsets.getValue(dimension)
                put(dimension, SynastryScore.from(adjusted))
            }
        }
    }

    private fun stretchContrast(base: Int): Int {
        val centered = base - 50
        val factor = 1.18
        return (50 + (centered * factor)).roundToInt()
    }

    private fun buildCompleteness(input: SynastryInput): SynastryDataCompleteness {
        val personAHasMoon = input.personA.moonSign != null
        val personAHasRising = input.personA.risingSign != null
        val personBHasMoon = input.personB.moonSign != null
        val personBHasRising = input.personB.risingSign != null
        val extraPoints = listOf(personAHasMoon, personAHasRising, personBHasMoon, personBHasRising).count { it }
        val depth = when (extraPoints) {
            0 -> SynastryReadingDepth.BASIC
            4 -> SynastryReadingDepth.COMPLETE
            else -> SynastryReadingDepth.PARTIAL
        }

        return SynastryDataCompleteness(
            personAHasMoon = personAHasMoon,
            personAHasRising = personAHasRising,
            personBHasMoon = personBHasMoon,
            personBHasRising = personBHasRising,
            availablePoints = 2 + extraPoints,
            depth = depth,
        )
    }

    private fun confidenceFor(depth: SynastryReadingDepth): SynastryConfidenceLevel = when (depth) {
        SynastryReadingDepth.BASIC -> SynastryConfidenceLevel.LOW
        SynastryReadingDepth.PARTIAL -> SynastryConfidenceLevel.MEDIUM
        SynastryReadingDepth.COMPLETE -> SynastryConfidenceLevel.HIGH
    }

    private fun emotionalScore(input: SynastryInput): SynastryScore {
        val a = input.personA
        val b = input.personB

        val moonMoon = optionalPairAverage(a.moonSign, b.moonSign)
        val moonSunCross = availableAverage(
            optionalPairValue(a.moonSign, b.sunSign),
            optionalPairValue(b.moonSign, a.sunSign),
        )
        val solarFallback = pairScore(a.sunSign, b.sunSign)

        val base = when {
            moonMoon != null && moonSunCross != null -> weightedAverage(0.65 to moonMoon, 0.35 to moonSunCross)
            moonMoon != null -> weightedAverage(0.80 to moonMoon, 0.20 to solarFallback)
            moonSunCross != null -> weightedAverage(0.70 to moonSunCross, 0.30 to solarFallback)
            else -> weightedAverage(0.70 to solarFallback, 0.30 to 50)
        }

        val allEmotionalSigns = listOfNotNull(a.moonSign, b.moonSign, a.sunSign, b.sunSign)
        val waterCount = allEmotionalSigns.count { it.element == AstroElement.WATER }
        val earthCount = allEmotionalSigns.count { it.element == AstroElement.EARTH }
        val fireCount = allEmotionalSigns.count { it.element == AstroElement.FIRE }
        val airCount = allEmotionalSigns.count { it.element == AstroElement.AIR }

        val bonus = when {
            waterCount >= 3 -> 14
            waterCount >= 2 && earthCount >= 1 -> 10
            else -> 0
        }

        val penalty = when {
            waterCount > 0 && fireCount >= 2 -> 13
            waterCount > 0 && airCount >= 2 -> 11
            else -> 0
        }

        return SynastryScore.from(base + bonus - penalty)
    }

    private fun communicationScore(input: SynastryInput): SynastryScore {
        val a = input.personA
        val b = input.personB

        val sunSun = pairScore(a.sunSign, b.sunSign)
        val ascAsc = optionalPairAverage(a.risingSign, b.risingSign) ?: 50
        val sunAscCross = availableAverage(
            optionalPairValue(a.sunSign, b.risingSign),
            optionalPairValue(b.sunSign, a.risingSign),
        ) ?: 50

        val base = weightedAverage(
            0.40 to sunSun,
            0.35 to ascAsc,
            0.25 to sunAscCross,
        )

        val signs = listOfNotNull(a.sunSign, b.sunSign, a.risingSign, b.risingSign)
        val airFireCount = signs.count { it.element == AstroElement.AIR || it.element == AstroElement.FIRE }
        val waterCount = signs.count { it.element == AstroElement.WATER }

        val bonus = when {
            airFireCount >= 4 -> 14
            airFireCount >= 3 -> 9
            else -> 0
        }

        val penalty = when {
            waterCount >= 3 && airFireCount <= 1 -> 8
            else -> 0
        }

        return SynastryScore.from(base + bonus - penalty)
    }

    private fun attractionScore(input: SynastryInput): SynastryScore {
        val a = input.personA
        val b = input.personB

        val sunSun = pairScore(a.sunSign, b.sunSign)
        val ascAsc = optionalPairAverage(a.risingSign, b.risingSign) ?: 50
        val sunAscCross = availableAverage(
            optionalPairValue(a.sunSign, b.risingSign),
            optionalPairValue(b.sunSign, a.risingSign),
        ) ?: 50

        var score = weightedAverage(
            0.30 to sunSun,
            0.30 to ascAsc,
            0.40 to sunAscCross,
        )

        if (areOpposite(a.sunSign, b.sunSign)) score += 18

        val fireAirCoupling = setOf(a.sunSign.element, b.sunSign.element) == setOf(AstroElement.FIRE, AstroElement.AIR)
        if (fireAirCoupling) score += 12

        if (a.risingSign != null && b.risingSign != null && areOpposite(a.risingSign, b.risingSign)) score += 10

        val earthyWaterySunPair = setOf(a.sunSign.element, b.sunSign.element) == setOf(AstroElement.EARTH, AstroElement.WATER)
        if (earthyWaterySunPair) score -= 7

        return SynastryScore.from(score)
    }

    private fun stabilityScore(input: SynastryInput): SynastryScore {
        val a = input.personA
        val b = input.personB

        val sunSun = pairScore(a.sunSign, b.sunSign)
        val moonMoon = optionalPairAverage(a.moonSign, b.moonSign) ?: 50
        val ascAsc = optionalPairAverage(a.risingSign, b.risingSign) ?: 50

        var score = weightedAverage(
            0.45 to sunSun,
            0.30 to moonMoon,
            0.25 to ascAsc,
        )

        val primarySigns = listOf(a.sunSign, b.sunSign)
        val earthWaterCount = primarySigns.count { it.element == AstroElement.EARTH || it.element == AstroElement.WATER }
        val fixedCount = primarySigns.count { it.modality == AstroModality.FIXED }
        val cardinalMutableMix = primarySigns.map { it.modality }.toSet() == setOf(AstroModality.CARDINAL, AstroModality.MUTABLE)

        if (earthWaterCount == 2) score += 14
        if (fixedCount == 2) score += 8
        if (cardinalMutableMix) score -= 10
        if (setOf(a.sunSign.element, b.sunSign.element) == setOf(AstroElement.FIRE, AstroElement.AIR)) score -= 9

        return SynastryScore.from(score)
    }

    private fun growthScore(input: SynastryInput): SynastryScore {
        val a = input.personA
        val b = input.personB

        val sunSun = pairScore(a.sunSign, b.sunSign)
        val moonSunCross = availableAverage(
            optionalPairValue(a.moonSign, b.sunSign),
            optionalPairValue(b.moonSign, a.sunSign),
        ) ?: 50
        val ascSunCross = availableAverage(
            optionalPairValue(a.risingSign, b.sunSign),
            optionalPairValue(b.risingSign, a.sunSign),
        ) ?: 50

        var score = weightedAverage(
            0.35 to sunSun,
            0.35 to moonSunCross,
            0.30 to ascSunCross,
        )

        if (areOpposite(a.sunSign, b.sunSign)) score += 15

        val modalities = setOf(a.sunSign.modality, b.sunSign.modality)
        if (modalities.size == 2) score += 6

        val elementalContrast = a.sunSign.element != b.sunSign.element
        if (elementalContrast) score += 6

        if (setOf(a.sunSign.element, b.sunSign.element) == setOf(AstroElement.EARTH, AstroElement.WATER)) score -= 6

        return SynastryScore.from(score)
    }

    private fun resolveArchetype(
        scores: Map<SynastryDimension, SynastryScore>,
        overallScore: SynastryScore,
    ): SynastryBondArchetype {
        val emotional = scores.require(SynastryDimension.EMOTIONAL).value
        val communication = scores.require(SynastryDimension.COMMUNICATION).value
        val attraction = scores.require(SynastryDimension.ATTRACTION).value
        val stability = scores.require(SynastryDimension.STABILITY).value
        val growth = scores.require(SynastryDimension.GROWTH).value

        val friction = ((100 - emotional) + (100 - stability) + (100 - communication)) / 3
        val resonanceSpread = maxOf(
            abs(emotional - communication),
            abs(emotional - stability),
            abs(communication - growth),
        )

        val rankedArchetypes = linkedMapOf(
            SynastryBondArchetype.STORM to ((attraction * 3) + ((100 - stability) * 2) + friction),
            SynastryBondArchetype.DEVOTIONAL to ((emotional * 3) + (stability * 3) + communication - (friction * 2)),
            SynastryBondArchetype.ANCHOR to ((stability * 3) + (overallScore.value * 2) + emotional - (friction * 2)),
            SynastryBondArchetype.ALCHEMICAL to ((growth * 3) + (friction * 2) + (abs(growth - stability) * 2)),
            SynastryBondArchetype.ELECTRIC to ((communication * 3) + (attraction * 3) + (growth * 2) - stability),
            SynastryBondArchetype.MAGNETIC to ((attraction * 3) + (overallScore.value * 2) + emotional),
            SynastryBondArchetype.MIRROR to (((100 - resonanceSpread) * 3) + emotional + communication + growth),
            SynastryBondArchetype.COSMIC_DANCE to ((overallScore.value * 3) + ((100 - resonanceSpread) * 2) + growth + communication),
        )

        val eligibility = mapOf(
            SynastryBondArchetype.STORM to (attraction >= 72 && stability <= 60 && friction >= 38),
            SynastryBondArchetype.DEVOTIONAL to (emotional >= 70 && stability >= 68 && friction <= 36),
            SynastryBondArchetype.ANCHOR to (stability >= 70 && emotional >= 58),
            SynastryBondArchetype.ALCHEMICAL to (growth >= 66 && friction >= 34),
            SynastryBondArchetype.ELECTRIC to (communication >= 68 && attraction >= 68),
            SynastryBondArchetype.MAGNETIC to (attraction >= 68 && overallScore.value >= 64),
            SynastryBondArchetype.MIRROR to (resonanceSpread <= 9 && overallScore.value >= 60),
            SynastryBondArchetype.COSMIC_DANCE to true,
        )

        return rankedArchetypes
            .asSequence()
            .filter { (archetype, _) -> eligibility.getValue(archetype) }
            .maxByOrNull { it.value }
            ?.key
            ?: SynastryBondArchetype.COSMIC_DANCE
    }

    private fun resolveStrengths(scores: Map<SynastryDimension, SynastryScore>): List<SynastrySignal> {
        val strengths = linkedSetOf<SynastrySignal>()
        val emotional = scores.require(SynastryDimension.EMOTIONAL).value
        val communication = scores.require(SynastryDimension.COMMUNICATION).value
        val attraction = scores.require(SynastryDimension.ATTRACTION).value
        val stability = scores.require(SynastryDimension.STABILITY).value
        val growth = scores.require(SynastryDimension.GROWTH).value

        if (emotional >= 66) strengths += SynastrySignal.STRONG_EMOTIONAL_RESONANCE
        if (attraction >= 68) strengths += SynastrySignal.NATURAL_SPARK
        if (communication >= 64) {
            strengths += SynastrySignal.COMMUNICATION_FLOW
            strengths += SynastrySignal.MENTAL_STIMULATION
        }
        if (stability >= 64) {
            strengths += SynastrySignal.STABILITY_POTENTIAL
            strengths += SynastrySignal.GROUNDING_BOND
        }
        if (growth >= 64) strengths += SynastrySignal.GROWTH_THROUGH_DIFFERENCE

        if (emotional >= 70 && communication >= 62) strengths += SynastrySignal.PROTECT_THE_SOFTNESS

        return strengths.toList()
    }

    private fun resolveTensions(scores: Map<SynastryDimension, SynastryScore>): List<SynastrySignal> {
        val tensions = linkedSetOf<SynastrySignal>()
        val emotional = scores.require(SynastryDimension.EMOTIONAL).value
        val communication = scores.require(SynastryDimension.COMMUNICATION).value
        val attraction = scores.require(SynastryDimension.ATTRACTION).value
        val stability = scores.require(SynastryDimension.STABILITY).value
        val growth = scores.require(SynastryDimension.GROWTH).value

        if (emotional <= 55) tensions += SynastrySignal.DIFFERENT_EMOTIONAL_RHYTHMS
        if (attraction >= 72 && stability <= 56) tensions += SynastrySignal.HIGH_INTENSITY
        if (communication <= 56 || stability <= 56) tensions += SynastrySignal.NEED_FOR_PATIENCE
        if (emotional >= 70 && communication <= 52) tensions += SynastrySignal.CREATE_SHARED_RHYTHM
        if (stability >= 72 && growth <= 52) tensions += SynastrySignal.USE_DIFFERENCE_AS_GROWTH

        return tensions.toList()
    }

    private fun resolveGuidance(tensions: List<SynastrySignal>): List<SynastrySignal> {
        val guidance = linkedSetOf<SynastrySignal>()

        tensions.forEach { tension ->
            when (tension) {
                SynastrySignal.DIFFERENT_EMOTIONAL_RHYTHMS -> {
                    guidance += SynastrySignal.CREATE_SHARED_RHYTHM
                    guidance += SynastrySignal.PROTECT_THE_SOFTNESS
                }

                SynastrySignal.HIGH_INTENSITY -> guidance += SynastrySignal.SLOW_DOWN_REACTIVITY
                SynastrySignal.NEED_FOR_PATIENCE -> {
                    guidance += SynastrySignal.USE_DIFFERENCE_AS_GROWTH
                    guidance += SynastrySignal.CREATE_SHARED_RHYTHM
                }

                SynastrySignal.CREATE_SHARED_RHYTHM -> guidance += SynastrySignal.PROTECT_THE_SOFTNESS
                SynastrySignal.USE_DIFFERENCE_AS_GROWTH -> guidance += SynastrySignal.USE_DIFFERENCE_AS_GROWTH
                else -> Unit
            }
        }

        if (guidance.isEmpty()) guidance += SynastrySignal.USE_DIFFERENCE_AS_GROWTH

        return guidance.toList()
    }

    private fun resolveTags(
        scores: Map<SynastryDimension, SynastryScore>,
        overallScore: SynastryScore,
        depth: SynastryReadingDepth,
    ): List<SynastryInsightTag> {
        val tags = linkedSetOf<SynastryInsightTag>()

        if (overallScore.value >= 70) tags += SynastryInsightTag.HARMONIOUS
        if (scores.require(SynastryDimension.ATTRACTION).value >= 70) tags += SynastryInsightTag.INTENSE
        if (scores.require(SynastryDimension.STABILITY).value >= 66) tags += SynastryInsightTag.GROUNDING
        if (scores.require(SynastryDimension.GROWTH).value >= 64) tags += SynastryInsightTag.EVOLUTIVE
        if (scores.require(SynastryDimension.COMMUNICATION).value >= 64) tags += SynastryInsightTag.COMMUNICATIVE
        if (scores.require(SynastryDimension.EMOTIONAL).value >= 64) tags += SynastryInsightTag.EMOTIONAL
        if (depth != SynastryReadingDepth.COMPLETE || overallScore.value < 58) tags += SynastryInsightTag.NEEDS_PRACTICE

        return tags.toList()
    }

    private fun optionalPairAverage(signA: ZodiacSign?, signB: ZodiacSign?): Int? =
        if (signA == null || signB == null) null else pairScore(signA, signB)

    private fun optionalPairValue(signA: ZodiacSign?, signB: ZodiacSign?): Int? =
        if (signA == null || signB == null) null else pairScore(signA, signB)

    private fun availableAverage(vararg values: Int?): Int? {
        val available = values.filterNotNull()
        if (available.isEmpty()) return null
        return available.average().roundToInt()
    }

    private fun weightedAverage(vararg weightedValues: Pair<Double, Int>): Int {
        val totalWeight = weightedValues.sumOf { it.first }
        val weightedSum = weightedValues.sumOf { (weight, value) -> weight * value }
        return (weightedSum / totalWeight).roundToInt()
    }

    private fun pairScore(signA: ZodiacSign, signB: ZodiacSign): Int {
        val elementScore = elementAffinity(signA.element, signB.element)
        val modalityScore = modalityAffinity(signA.modality, signB.modality)
        return (50 + elementScore + modalityScore).coerceIn(0, 100)
    }

    private fun resolveSolarProfile(signA: ZodiacSign, signB: ZodiacSign): SolarCombinationProfile {
        val elementPair = setOf(signA.element, signB.element)
        val sameElement = signA.element == signB.element
        val opposite = areOpposite(signA, signB)

        val baseProfile = when (elementPair) {
            setOf(AstroElement.FIRE, AstroElement.AIR) -> SolarCombinationProfile(
                offsets = mapOf(
                    SynastryDimension.EMOTIONAL to -6,
                    SynastryDimension.COMMUNICATION to 10,
                    SynastryDimension.ATTRACTION to 12,
                    SynastryDimension.STABILITY to -14,
                    SynastryDimension.GROWTH to 9,
                )
            )

            setOf(AstroElement.EARTH, AstroElement.WATER) -> SolarCombinationProfile(
                offsets = mapOf(
                    SynastryDimension.EMOTIONAL to 10,
                    SynastryDimension.COMMUNICATION to -2,
                    SynastryDimension.ATTRACTION to -4,
                    SynastryDimension.STABILITY to 13,
                    SynastryDimension.GROWTH to 2,
                )
            )

            setOf(AstroElement.FIRE, AstroElement.WATER) -> SolarCombinationProfile(
                offsets = mapOf(
                    SynastryDimension.EMOTIONAL to -8,
                    SynastryDimension.COMMUNICATION to -4,
                    SynastryDimension.ATTRACTION to 11,
                    SynastryDimension.STABILITY to -10,
                    SynastryDimension.GROWTH to 12,
                )
            )

            setOf(AstroElement.AIR, AstroElement.EARTH) -> SolarCombinationProfile(
                offsets = mapOf(
                    SynastryDimension.EMOTIONAL to -5,
                    SynastryDimension.COMMUNICATION to 5,
                    SynastryDimension.ATTRACTION to 1,
                    SynastryDimension.STABILITY to -8,
                    SynastryDimension.GROWTH to 9,
                )
            )

            else -> SolarCombinationProfile()
        }

        val sameElementOffsets = if (sameElement) {
            mapOf(
                SynastryDimension.EMOTIONAL to 4,
                SynastryDimension.COMMUNICATION to 3,
                SynastryDimension.ATTRACTION to -2,
                SynastryDimension.STABILITY to 6,
                SynastryDimension.GROWTH to -1,
            )
        } else {
            emptyMap()
        }

        val oppositeOffsets = if (opposite) {
            mapOf(
                SynastryDimension.EMOTIONAL to -2,
                SynastryDimension.COMMUNICATION to 4,
                SynastryDimension.ATTRACTION to 8,
                SynastryDimension.STABILITY to -9,
                SynastryDimension.GROWTH to 11,
            )
        } else {
            emptyMap()
        }

        return SolarCombinationProfile(
            offsets = SynastryDimension.entries.associateWith { dimension ->
                baseProfile.offsets.getValue(dimension) +
                    sameElementOffsets.getOrDefault(dimension, 0) +
                    oppositeOffsets.getOrDefault(dimension, 0)
            }
        )
    }

    private fun elementAffinity(a: AstroElement, b: AstroElement): Int {
        if (a == b) return 18

        val pair = setOf(a, b)
        return when (pair) {
            setOf(AstroElement.FIRE, AstroElement.AIR),
            setOf(AstroElement.EARTH, AstroElement.WATER),
            -> 12

            setOf(AstroElement.FIRE, AstroElement.EARTH),
            setOf(AstroElement.AIR, AstroElement.WATER),
            -> 2

            else -> -8
        }
    }

    private fun modalityAffinity(a: AstroModality, b: AstroModality): Int {
        if (a == b) return 8
        return if (a.compatibleWith(b)) 3 else -5
    }

    private fun AstroModality.compatibleWith(other: AstroModality): Boolean =
        setOf(this, other) != setOf(AstroModality.CARDINAL, AstroModality.FIXED)

    private fun areOpposite(signA: ZodiacSign, signB: ZodiacSign): Boolean {
        val distance = abs(signA.ordinal - signB.ordinal)
        return distance == 6
    }

    private fun Map<SynastryDimension, SynastryScore>.require(dimension: SynastryDimension): SynastryScore =
        getValue(dimension)
}

private data class SolarCombinationProfile(
    val offsets: Map<SynastryDimension, Int> = SynastryDimension.entries.associateWith { 0 },
)

internal enum class AstroElement {
    FIRE,
    EARTH,
    AIR,
    WATER,
}

internal enum class AstroModality {
    CARDINAL,
    FIXED,
    MUTABLE,
}

internal val ZodiacSign.element: AstroElement
    get() = when (this) {
        ZodiacSign.aries, ZodiacSign.leo, ZodiacSign.sagittarius -> AstroElement.FIRE
        ZodiacSign.taurus, ZodiacSign.virgo, ZodiacSign.capricorn -> AstroElement.EARTH
        ZodiacSign.gemini, ZodiacSign.libra, ZodiacSign.aquarius -> AstroElement.AIR
        ZodiacSign.cancer, ZodiacSign.scorpio, ZodiacSign.pisces -> AstroElement.WATER
    }

internal val ZodiacSign.modality: AstroModality
    get() = when (this) {
        ZodiacSign.aries, ZodiacSign.cancer, ZodiacSign.libra, ZodiacSign.capricorn -> AstroModality.CARDINAL
        ZodiacSign.taurus, ZodiacSign.leo, ZodiacSign.scorpio, ZodiacSign.aquarius -> AstroModality.FIXED
        ZodiacSign.gemini, ZodiacSign.virgo, ZodiacSign.sagittarius, ZodiacSign.pisces -> AstroModality.MUTABLE
    }
