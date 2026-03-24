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
        val scores = linkedMapOf(
            SynastryDimension.EMOTIONAL to emotionalScore(input),
            SynastryDimension.COMMUNICATION to communicationScore(input),
            SynastryDimension.ATTRACTION to attractionScore(input),
            SynastryDimension.STABILITY to stabilityScore(input),
            SynastryDimension.GROWTH to growthScore(input),
        )

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
        return weightedDimensionScore(
            weightedComponent(0.40, pairScore(a.sunSign, b.sunSign)),
            optionalWeightedComponent(0.40, a.moonSign, b.moonSign),
            optionalWeightedComponent(0.10, a.sunSign, b.moonSign),
            optionalWeightedComponent(0.10, a.moonSign, b.sunSign),
        )
    }

    private fun communicationScore(input: SynastryInput): SynastryScore {
        val a = input.personA
        val b = input.personB
        return weightedDimensionScore(
            weightedComponent(0.45, pairScore(a.sunSign, b.sunSign) + mentalBonus(a.sunSign, b.sunSign)),
            optionalWeightedComponent(0.25, a.moonSign, b.moonSign),
            optionalWeightedComponent(0.30, a.risingSign, b.risingSign),
        )
    }

    private fun attractionScore(input: SynastryInput): SynastryScore {
        val a = input.personA
        val b = input.personB
        return weightedDimensionScore(
            weightedComponent(0.45, pairScore(a.sunSign, b.sunSign) + intensityBonus(a.sunSign, b.sunSign)),
            optionalWeightedComponent(0.30, a.risingSign, b.risingSign),
            optionalWeightedComponent(0.125, a.sunSign, b.risingSign),
            optionalWeightedComponent(0.125, a.risingSign, b.sunSign),
        )
    }

    private fun stabilityScore(input: SynastryInput): SynastryScore {
        val a = input.personA
        val b = input.personB
        return weightedDimensionScore(
            weightedComponent(0.55, pairScore(a.sunSign, b.sunSign) + stabilityBonus(a.sunSign, b.sunSign)),
            optionalWeightedComponent(0.20, a.moonSign, b.moonSign),
            optionalWeightedComponent(0.25, a.risingSign, b.risingSign),
        )
    }

    private fun growthScore(input: SynastryInput): SynastryScore {
        val a = input.personA
        val b = input.personB
        return weightedDimensionScore(
            weightedComponent(0.40, pairScore(a.sunSign, b.sunSign)),
            optionalWeightedComponent(0.20, a.sunSign, b.moonSign),
            optionalWeightedComponent(0.20, a.moonSign, b.sunSign),
            optionalWeightedComponent(0.20, a.risingSign, b.risingSign),
        )
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
            SynastryBondArchetype.STORM to (
                (attraction * 3) +
                    ((100 - stability) * 2) +
                    friction
                ),
            SynastryBondArchetype.DEVOTIONAL to (
                (emotional * 3) +
                    (stability * 3) +
                    communication -
                    (friction * 2)
                ),
            SynastryBondArchetype.ANCHOR to (
                (stability * 3) +
                    (overallScore.value * 2) +
                    emotional -
                    (friction * 2)
                ),
            SynastryBondArchetype.ALCHEMICAL to (
                (growth * 3) +
                    (friction * 2) +
                    (abs(growth - stability) * 2)
                ),
            SynastryBondArchetype.ELECTRIC to (
                (communication * 3) +
                    (attraction * 3) +
                    (growth * 2) -
                    stability
                ),
            SynastryBondArchetype.MAGNETIC to (
                (attraction * 3) +
                    (overallScore.value * 2) +
                    emotional
                ),
            SynastryBondArchetype.MIRROR to (
                ((100 - resonanceSpread) * 3) +
                    emotional +
                    communication +
                    growth
                ),
            SynastryBondArchetype.COSMIC_DANCE to (
                (overallScore.value * 3) +
                    ((100 - resonanceSpread) * 2) +
                    growth +
                    communication
                ),
        )

        val eligibility = mapOf(
            SynastryBondArchetype.STORM to (attraction >= 74 && stability <= 58 && friction >= 42),
            SynastryBondArchetype.DEVOTIONAL to (emotional >= 72 && stability >= 70 && friction <= 34),
            SynastryBondArchetype.ANCHOR to (stability >= 72 && emotional >= 60),
            SynastryBondArchetype.ALCHEMICAL to (growth >= 68 && friction >= 36),
            SynastryBondArchetype.ELECTRIC to (communication >= 70 && attraction >= 68),
            SynastryBondArchetype.MAGNETIC to (attraction >= 70 && overallScore.value >= 68),
            SynastryBondArchetype.MIRROR to (resonanceSpread <= 10 && overallScore.value >= 60),
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

        if (scores.require(SynastryDimension.EMOTIONAL).value >= 70) {
            strengths += SynastrySignal.STRONG_EMOTIONAL_RESONANCE
        }
        if (scores.require(SynastryDimension.ATTRACTION).value >= 70) {
            strengths += SynastrySignal.NATURAL_SPARK
        }
        if (scores.require(SynastryDimension.COMMUNICATION).value >= 68) {
            strengths += SynastrySignal.COMMUNICATION_FLOW
            strengths += SynastrySignal.MENTAL_STIMULATION
        }
        if (scores.require(SynastryDimension.STABILITY).value >= 68) {
            strengths += SynastrySignal.STABILITY_POTENTIAL
            strengths += SynastrySignal.GROUNDING_BOND
        }
        if (scores.require(SynastryDimension.GROWTH).value >= 68) {
            strengths += SynastrySignal.GROWTH_THROUGH_DIFFERENCE
        }

        return strengths.toList()
    }

    private fun resolveTensions(scores: Map<SynastryDimension, SynastryScore>): List<SynastrySignal> {
        val tensions = linkedSetOf<SynastrySignal>()

        if (scores.require(SynastryDimension.EMOTIONAL).value <= 52) {
            tensions += SynastrySignal.DIFFERENT_EMOTIONAL_RHYTHMS
        }
        if (scores.require(SynastryDimension.ATTRACTION).value >= 74 &&
            scores.require(SynastryDimension.STABILITY).value <= 58
        ) {
            tensions += SynastrySignal.HIGH_INTENSITY
        }
        if (scores.require(SynastryDimension.COMMUNICATION).value <= 54 ||
            scores.require(SynastryDimension.STABILITY).value <= 54
        ) {
            tensions += SynastrySignal.NEED_FOR_PATIENCE
        }

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

                else -> Unit
            }
        }

        if (guidance.isEmpty()) {
            guidance += SynastrySignal.USE_DIFFERENCE_AS_GROWTH
        }

        return guidance.toList()
    }

    private fun resolveTags(
        scores: Map<SynastryDimension, SynastryScore>,
        overallScore: SynastryScore,
        depth: SynastryReadingDepth,
    ): List<SynastryInsightTag> {
        val tags = linkedSetOf<SynastryInsightTag>()

        if (overallScore.value >= 72) tags += SynastryInsightTag.HARMONIOUS
        if (scores.require(SynastryDimension.ATTRACTION).value >= 74) tags += SynastryInsightTag.INTENSE
        if (scores.require(SynastryDimension.STABILITY).value >= 70) tags += SynastryInsightTag.GROUNDING
        if (scores.require(SynastryDimension.GROWTH).value >= 68) tags += SynastryInsightTag.EVOLUTIVE
        if (scores.require(SynastryDimension.COMMUNICATION).value >= 68) tags += SynastryInsightTag.COMMUNICATIVE
        if (scores.require(SynastryDimension.EMOTIONAL).value >= 68) tags += SynastryInsightTag.EMOTIONAL
        if (depth != SynastryReadingDepth.COMPLETE || overallScore.value < 60) tags += SynastryInsightTag.NEEDS_PRACTICE

        return tags.toList()
    }

    private fun optionalWeightedComponent(weight: Double, signA: ZodiacSign?, signB: ZodiacSign?): WeightedComponent? {
        if (signA == null || signB == null) return null
        return weightedComponent(weight, pairScore(signA, signB))
    }

    private fun weightedComponent(weight: Double, rawScore: Int): WeightedComponent =
        WeightedComponent(weight = weight, score = rawScore.coerceIn(0, 100))

    private fun weightedDimensionScore(vararg components: WeightedComponent?): SynastryScore {
        val availableComponents = components.filterNotNull()
        val totalWeight = availableComponents.sumOf { it.weight }
        val weightedScore = availableComponents.sumOf { it.score * it.weight }
        return SynastryScore.from((weightedScore / totalWeight).roundToInt())
    }

    private fun pairScore(signA: ZodiacSign, signB: ZodiacSign): Int {
        val elementScore = elementAffinity(signA.element, signB.element)
        val modalityScore = modalityAffinity(signA.modality, signB.modality)
        return (50 + elementScore + modalityScore).coerceIn(0, 100)
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

    private fun mentalBonus(signA: ZodiacSign, signB: ZodiacSign): Int {
        val airyCount = listOf(signA.element, signB.element).count { it == AstroElement.AIR }
        return when (airyCount) {
            2 -> 7
            1 -> 3
            else -> 0
        }
    }

    private fun intensityBonus(signA: ZodiacSign, signB: ZodiacSign): Int {
        val fieryCount = listOf(signA.element, signB.element).count { it == AstroElement.FIRE }
        return when (fieryCount) {
            2 -> 8
            1 -> 3
            else -> 0
        }
    }

    private fun stabilityBonus(signA: ZodiacSign, signB: ZodiacSign): Int {
        val earthyWateryCount = listOf(signA.element, signB.element).count {
            it == AstroElement.EARTH || it == AstroElement.WATER
        }
        return when (earthyWateryCount) {
            2 -> 8
            1 -> 3
            else -> 0
        }
    }

    private fun Map<SynastryDimension, SynastryScore>.require(dimension: SynastryDimension): SynastryScore =
        getValue(dimension)

    private data class WeightedComponent(
        val weight: Double,
        val score: Int,
    )
}

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
