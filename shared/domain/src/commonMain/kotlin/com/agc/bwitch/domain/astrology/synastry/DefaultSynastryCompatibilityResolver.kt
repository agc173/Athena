package com.agc.bwitch.domain.astrology.synastry

import com.agc.bwitch.domain.astrology.horoscope.ZodiacSign
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toEpochDays
import kotlinx.datetime.toLocalDateTime

interface SynastryCompatibilityResolver {
    fun resolve(
        input: SynastryInput,
        date: LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date,
    ): SynastryReadingStructured
}

class DefaultSynastryCompatibilityResolver : SynastryCompatibilityResolver {

    override fun resolve(input: SynastryInput, date: LocalDate): SynastryReadingStructured {
        val completeness = buildCompleteness(input)
        val family = resolveFamily(input.personA.sunSign, input.personB.sunSign)
        val baseProfile = buildBaseProfile(input, family, input.personA.sunSign, input.personB.sunSign)
        val seed = deterministicSeed(input)

        val scores = SynastryDimension.entries.associateWith { dimension ->
            val profile = baseProfile.metrics.getValue(dimension)
            val raw = metricValueForDay(profile, date, seed + dimension.ordinal * 997)
            SynastryScore.from(raw.roundToInt())
        }

        val overallScore = SynastryScore.from(scores.values.map { it.value }.average().roundToInt())
        val archetype = resolveArchetype(scores, overallScore)
        val axisStates = resolveAxisStates(scores, baseProfile, date, seed)
        val strengths = resolveStrengths(scores, axisStates)
        val tensions = resolveTensions(scores, axisStates)
        val guidance = resolveGuidance(tensions)
        val tags = resolveTags(scores, overallScore, completeness.depth)

        return SynastryReadingStructured(
            depthInfo = completeness,
            confidenceLevel = confidenceFor(completeness.depth),
            archetype = archetype,
            overallScore = overallScore,
            scores = scores,
            baseProfile = baseProfile,
            strengths = strengths,
            tensions = tensions,
            guidance = guidance,
            tags = tags,
        )
    }

    private fun metricValueForDay(profile: SynastryMetricProfile, date: LocalDate, seed: Int): Double {
        val t = date.toEpochDays().toDouble()
        val longPhase = phase(seed, 0.11)
        val shortPhase = phase(seed, 0.29)
        val microPhase = phase(seed, 0.53)

        val longWave = sin((2 * PI * t / profile.longPeriodDays) + longPhase) * profile.longAmplitude
        val shortWave = sin((2 * PI * t / profile.shortPeriodDays) + shortPhase) * profile.shortAmplitude
        val micro = sin((2 * PI * t / 3.0) + microPhase) * profile.microAmplitude

        return (profile.center + longWave + shortWave + micro).coerceIn(15.0, 92.0)
    }

    private fun buildBaseProfile(
        input: SynastryInput,
        family: PairFamily,
        sunA: ZodiacSign,
        sunB: ZodiacSign,
    ): SynastryBaseProfile {
        val moonInfluence = optionalPairAverage(input.personA.moonSign, input.personB.moonSign)?.let { (it - 50) / 10 } ?: 0
        val risingInfluence = optionalPairAverage(input.personA.risingSign, input.personB.risingSign)?.let { (it - 50) / 12 } ?: 0
        val microProfile = resolveSolarMicroProfile(sunA, sunB)

        val familyCenters = family.centers
        val centers = mapOf(
            SynastryDimension.ATTRACTION to (
                familyCenters.getValue(SynastryDimension.ATTRACTION) + risingInfluence + microProfile.centerOffsets.getValue(SynastryDimension.ATTRACTION)
                ),
            SynastryDimension.EMOTIONAL to (
                familyCenters.getValue(SynastryDimension.EMOTIONAL) + moonInfluence + microProfile.centerOffsets.getValue(SynastryDimension.EMOTIONAL)
                ),
            SynastryDimension.COMMUNICATION to (
                familyCenters.getValue(SynastryDimension.COMMUNICATION) + risingInfluence / 2 + microProfile.centerOffsets.getValue(SynastryDimension.COMMUNICATION)
                ),
            SynastryDimension.GROWTH to (
                familyCenters.getValue(SynastryDimension.GROWTH) + ((moonInfluence + risingInfluence) / 2) + microProfile.centerOffsets.getValue(SynastryDimension.GROWTH)
                ),
        ).mapValues { it.value.coerceIn(35, 75) }

        val metrics = SynastryDimension.entries.associateWith { dimension ->
            SynastryMetricProfile(
                center = centers.getValue(dimension),
                longAmplitude = (family.longAmplitude.getValue(dimension) + microProfile.longAmplitudeOffsets.getValue(dimension)).coerceIn(8.0, 20.0),
                shortAmplitude = (family.shortAmplitude.getValue(dimension) + microProfile.shortAmplitudeOffsets.getValue(dimension)).coerceIn(3.0, 8.0),
                microAmplitude = (family.microAmplitude + microProfile.microAmplitudeShift).coerceIn(1.2, 3.0),
                longPeriodDays = family.longPeriodDays,
                shortPeriodDays = family.shortPeriodDays,
            )
        }

        return SynastryBaseProfile(
            familyKey = "${family.key}_${microProfile.key}",
            metrics = metrics,
        )
    }

    private fun resolveSolarMicroProfile(sunA: ZodiacSign, sunB: ZodiacSign): SolarMicroProfile {
        val distanceRaw = abs(sunA.ordinal - sunB.ordinal)
        val distance = minOf(distanceRaw, 12 - distanceRaw)
        val sameSign = sunA == sunB
        val opposite = areOpposite(sunA, sunB)
        val sameElement = sunA.element == sunB.element
        val modalities = setOf(sunA.modality, sunB.modality)

        val centerOffsets = mutableMapOf(
            SynastryDimension.ATTRACTION to 0,
            SynastryDimension.EMOTIONAL to 0,
            SynastryDimension.COMMUNICATION to 0,
            SynastryDimension.GROWTH to 0,
        )
        val longOffsets = mutableMapOf(
            SynastryDimension.ATTRACTION to 0.0,
            SynastryDimension.EMOTIONAL to 0.0,
            SynastryDimension.COMMUNICATION to 0.0,
            SynastryDimension.GROWTH to 0.0,
        )
        val shortOffsets = mutableMapOf(
            SynastryDimension.ATTRACTION to 0.0,
            SynastryDimension.EMOTIONAL to 0.0,
            SynastryDimension.COMMUNICATION to 0.0,
            SynastryDimension.GROWTH to 0.0,
        )
        var microShift = 0.0

        if (sameSign) {
            centerOffsets[SynastryDimension.EMOTIONAL] = centerOffsets.getValue(SynastryDimension.EMOTIONAL) + 6
            centerOffsets[SynastryDimension.COMMUNICATION] = centerOffsets.getValue(SynastryDimension.COMMUNICATION) + 5
            centerOffsets[SynastryDimension.GROWTH] = centerOffsets.getValue(SynastryDimension.GROWTH) - 3
            longOffsets[SynastryDimension.GROWTH] = longOffsets.getValue(SynastryDimension.GROWTH) - 1.2
            microShift -= 0.2
        }

        if (opposite) {
            centerOffsets[SynastryDimension.ATTRACTION] = centerOffsets.getValue(SynastryDimension.ATTRACTION) + 4
            centerOffsets[SynastryDimension.GROWTH] = centerOffsets.getValue(SynastryDimension.GROWTH) + 5
            shortOffsets[SynastryDimension.ATTRACTION] = shortOffsets.getValue(SynastryDimension.ATTRACTION) + 1.0
            shortOffsets[SynastryDimension.GROWTH] = shortOffsets.getValue(SynastryDimension.GROWTH) + 1.0
            microShift += 0.2
        }

        when (distance) {
            1, 2 -> {
                centerOffsets[SynastryDimension.ATTRACTION] = centerOffsets.getValue(SynastryDimension.ATTRACTION) + 3
                centerOffsets[SynastryDimension.EMOTIONAL] = centerOffsets.getValue(SynastryDimension.EMOTIONAL) - 2
                shortOffsets[SynastryDimension.ATTRACTION] = shortOffsets.getValue(SynastryDimension.ATTRACTION) + 0.6
            }

            3, 4 -> {
                centerOffsets[SynastryDimension.COMMUNICATION] = centerOffsets.getValue(SynastryDimension.COMMUNICATION) + 2
                centerOffsets[SynastryDimension.GROWTH] = centerOffsets.getValue(SynastryDimension.GROWTH) + 2
                longOffsets[SynastryDimension.GROWTH] = longOffsets.getValue(SynastryDimension.GROWTH) + 0.8
            }

            5 -> {
                centerOffsets[SynastryDimension.EMOTIONAL] = centerOffsets.getValue(SynastryDimension.EMOTIONAL) + 2
                centerOffsets[SynastryDimension.COMMUNICATION] = centerOffsets.getValue(SynastryDimension.COMMUNICATION) - 1
            }
        }

        if (sameElement && sunA.modality != sunB.modality) {
            centerOffsets[SynastryDimension.GROWTH] = centerOffsets.getValue(SynastryDimension.GROWTH) + 3
            centerOffsets[SynastryDimension.COMMUNICATION] = centerOffsets.getValue(SynastryDimension.COMMUNICATION) + 2
            longOffsets[SynastryDimension.GROWTH] = longOffsets.getValue(SynastryDimension.GROWTH) + 0.7
        }

        if (modalities.size == 1) {
            when (sunA.modality) {
                AstroModality.FIXED -> {
                    centerOffsets[SynastryDimension.EMOTIONAL] = centerOffsets.getValue(SynastryDimension.EMOTIONAL) + 1
                    centerOffsets[SynastryDimension.GROWTH] = centerOffsets.getValue(SynastryDimension.GROWTH) - 2
                }

                AstroModality.CARDINAL -> {
                    centerOffsets[SynastryDimension.ATTRACTION] = centerOffsets.getValue(SynastryDimension.ATTRACTION) + 2
                    shortOffsets[SynastryDimension.ATTRACTION] = shortOffsets.getValue(SynastryDimension.ATTRACTION) + 0.5
                }

                AstroModality.MUTABLE -> {
                    centerOffsets[SynastryDimension.GROWTH] = centerOffsets.getValue(SynastryDimension.GROWTH) + 2
                    centerOffsets[SynastryDimension.EMOTIONAL] = centerOffsets.getValue(SynastryDimension.EMOTIONAL) - 1
                    microShift += 0.1
                }
            }
        }

        val modalityKey = listOf(sunA.modality.name.lowercase(), sunB.modality.name.lowercase()).sorted().joinToString("_")
        return SolarMicroProfile(
            key = "d${distance}_${modalityKey}_${if (sameSign) "same" else "diff"}",
            centerOffsets = centerOffsets,
            longAmplitudeOffsets = longOffsets,
            shortAmplitudeOffsets = shortOffsets,
            microAmplitudeShift = microShift,
        )
    }

    private fun resolveArchetype(
        scores: Map<SynastryDimension, SynastryScore>,
        overallScore: SynastryScore,
    ): SynastryBondArchetype {
        val emotional = scores.require(SynastryDimension.EMOTIONAL).value
        val communication = scores.require(SynastryDimension.COMMUNICATION).value
        val attraction = scores.require(SynastryDimension.ATTRACTION).value
        val growth = scores.require(SynastryDimension.GROWTH).value

        val tension = ((100 - emotional) + (100 - communication) + (100 - growth)) / 3

        return when {
            attraction >= 72 && tension >= 36 -> SynastryBondArchetype.STORM
            emotional >= 70 && communication >= 66 -> SynastryBondArchetype.DEVOTIONAL
            growth >= 70 && abs(growth - emotional) >= 12 -> SynastryBondArchetype.ALCHEMICAL
            communication >= 70 && attraction >= 66 -> SynastryBondArchetype.ELECTRIC
            overallScore.value >= 68 -> SynastryBondArchetype.MAGNETIC
            abs(emotional - communication) <= 5 -> SynastryBondArchetype.MIRROR
            growth >= 65 -> SynastryBondArchetype.COSMIC_DANCE
            else -> SynastryBondArchetype.ANCHOR
        }
    }

    private fun resolveStrengths(
        scores: Map<SynastryDimension, SynastryScore>,
        axes: List<SynastryDailyAxisState>,
    ): List<SynastrySignal> {
        val strengths = linkedSetOf<SynastrySignal>()

        if (scores.require(SynastryDimension.EMOTIONAL).value >= 63) strengths += SynastrySignal.STRONG_EMOTIONAL_RESONANCE
        if (scores.require(SynastryDimension.ATTRACTION).value >= 65) strengths += SynastrySignal.NATURAL_SPARK
        if (scores.require(SynastryDimension.COMMUNICATION).value >= 62) {
            strengths += SynastrySignal.COMMUNICATION_FLOW
            strengths += SynastrySignal.MENTAL_STIMULATION
        }
        if (scores.require(SynastryDimension.GROWTH).value >= 62) strengths += SynastrySignal.GROWTH_THROUGH_DIFFERENCE
        if (axes.find { it.axis == SynastryEnergyAxis.STABILITY_TRANSFORMATION }?.value ?: 0 <= -25) {
            strengths += SynastrySignal.GROUNDING_BOND
            strengths += SynastrySignal.STABILITY_POTENTIAL
        }

        if (strengths.isEmpty()) strengths += SynastrySignal.NATURAL_SPARK
        return strengths.toList()
    }

    private fun resolveTensions(
        scores: Map<SynastryDimension, SynastryScore>,
        axes: List<SynastryDailyAxisState>,
    ): List<SynastrySignal> {
        val tensions = linkedSetOf<SynastrySignal>()

        if (scores.require(SynastryDimension.EMOTIONAL).value <= 52) tensions += SynastrySignal.DIFFERENT_EMOTIONAL_RHYTHMS
        if (scores.require(SynastryDimension.COMMUNICATION).value <= 53) tensions += SynastrySignal.NEED_FOR_PATIENCE
        if (scores.require(SynastryDimension.ATTRACTION).value >= 70 && scores.require(SynastryDimension.GROWTH).value <= 55) {
            tensions += SynastrySignal.HIGH_INTENSITY
        }
        if (axes.find { it.axis == SynastryEnergyAxis.CALM_MOVEMENT }?.value ?: 0 >= 40) {
            tensions += SynastrySignal.SLOW_DOWN_REACTIVITY
        }

        if (tensions.isEmpty()) tensions += SynastrySignal.CREATE_SHARED_RHYTHM
        return tensions.toList()
    }

    private fun resolveGuidance(tensions: List<SynastrySignal>): List<SynastrySignal> {
        val guidance = linkedSetOf<SynastrySignal>()

        tensions.forEach { tension ->
            when (tension) {
                SynastrySignal.DIFFERENT_EMOTIONAL_RHYTHMS -> guidance += SynastrySignal.PROTECT_THE_SOFTNESS
                SynastrySignal.NEED_FOR_PATIENCE -> guidance += SynastrySignal.CREATE_SHARED_RHYTHM
                SynastrySignal.HIGH_INTENSITY,
                SynastrySignal.SLOW_DOWN_REACTIVITY,
                -> guidance += SynastrySignal.SLOW_DOWN_REACTIVITY

                else -> guidance += SynastrySignal.USE_DIFFERENCE_AS_GROWTH
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
        if (scores.require(SynastryDimension.ATTRACTION).value >= 68) tags += SynastryInsightTag.INTENSE
        if (scores.require(SynastryDimension.GROWTH).value >= 64) tags += SynastryInsightTag.EVOLUTIVE
        if (scores.require(SynastryDimension.COMMUNICATION).value >= 62) tags += SynastryInsightTag.COMMUNICATIVE
        if (scores.require(SynastryDimension.EMOTIONAL).value >= 62) tags += SynastryInsightTag.EMOTIONAL
        if (depth != SynastryReadingDepth.COMPLETE || overallScore.value < 58) tags += SynastryInsightTag.NEEDS_PRACTICE
        if (overallScore.value in 58..70) tags += SynastryInsightTag.GROUNDING

        return tags.toList()
    }

    internal fun resolveAxisStates(
        scores: Map<SynastryDimension, SynastryScore>,
        baseProfile: SynastryBaseProfile,
        date: LocalDate,
        seed: Int,
    ): List<SynastryDailyAxisState> {
        val t = date.toEpochDays().toDouble()
        val harmonyIntensity = (
            ((scores.require(SynastryDimension.ATTRACTION).value + scores.require(SynastryDimension.COMMUNICATION).value) / 2.0 - 55.0) * 1.9 +
                sin((2 * PI * t / 6.0) + phase(seed, 0.33)) * 14.0
            ).roundToInt().coerceIn(-100, 100)

        val avgCenter = baseProfile.metrics.values.map { it.center }.average()
        val stabilityTransformation = (
            ((scores.require(SynastryDimension.GROWTH).value - avgCenter) * 2.1) +
                sin((2 * PI * t / 10.0) + phase(seed, 0.71)) * 18.0
            ).roundToInt().coerceIn(-100, 100)

        val calmMovement = (
            sin((2 * PI * t / 4.0) + phase(seed, 0.47)) * 34.0 +
                ((scores.require(SynastryDimension.ATTRACTION).value - scores.require(SynastryDimension.EMOTIONAL).value) * 0.9)
            ).roundToInt().coerceIn(-100, 100)

        return listOf(
            SynastryDailyAxisState(SynastryEnergyAxis.HARMONY_INTENSITY, harmonyIntensity),
            SynastryDailyAxisState(SynastryEnergyAxis.STABILITY_TRANSFORMATION, stabilityTransformation),
            SynastryDailyAxisState(SynastryEnergyAxis.CALM_MOVEMENT, calmMovement),
        )
    }

    private fun resolveFamily(signA: ZodiacSign, signB: ZodiacSign): PairFamily {
        val pair = setOf(signA.element, signB.element)
        val same = signA.element == signB.element
        val opposite = areOpposite(signA, signB)

        return when {
            same -> PairFamily.sameElement(signA.element)
            opposite -> PairFamily.opposed
            pair == setOf(AstroElement.FIRE, AstroElement.AIR) -> PairFamily.fireAir
            pair == setOf(AstroElement.EARTH, AstroElement.WATER) -> PairFamily.earthWater
            pair == setOf(AstroElement.FIRE, AstroElement.WATER) -> PairFamily.fireWater
            pair == setOf(AstroElement.AIR, AstroElement.EARTH) -> PairFamily.airEarth
            else -> PairFamily.mixed
        }
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

    private fun optionalPairAverage(signA: ZodiacSign?, signB: ZodiacSign?): Int? =
        if (signA == null || signB == null) null else pairScore(signA, signB)

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

    private fun areOpposite(signA: ZodiacSign, signB: ZodiacSign): Boolean {
        val distance = abs(signA.ordinal - signB.ordinal)
        return distance == 6
    }

    private fun deterministicSeed(input: SynastryInput): Int {
        val canonical = listOf(
            input.personA.sunSign.name,
            input.personB.sunSign.name,
            input.personA.moonSign?.name ?: "_",
            input.personB.moonSign?.name ?: "_",
            input.personA.risingSign?.name ?: "_",
            input.personB.risingSign?.name ?: "_",
        ).sorted().joinToString("|")

        return canonical.fold(97) { acc, char -> (acc * 31) + char.code }
    }

    private fun phase(seed: Int, factor: Double): Double = ((seed and Int.MAX_VALUE) * factor) % (2 * PI)

    private fun Map<SynastryDimension, SynastryScore>.require(dimension: SynastryDimension): SynastryScore =
        getValue(dimension)
}


private data class SolarMicroProfile(
    val key: String,
    val centerOffsets: Map<SynastryDimension, Int>,
    val longAmplitudeOffsets: Map<SynastryDimension, Double>,
    val shortAmplitudeOffsets: Map<SynastryDimension, Double>,
    val microAmplitudeShift: Double,
)

private data class PairFamily(
    val key: String,
    val centers: Map<SynastryDimension, Int>,
    val longAmplitude: Map<SynastryDimension, Double>,
    val shortAmplitude: Map<SynastryDimension, Double>,
    val microAmplitude: Double,
    val longPeriodDays: Int,
    val shortPeriodDays: Int,
) {
    companion object {
        val fireAir = PairFamily(
            key = "fire_air",
            centers = mapOf(
                SynastryDimension.ATTRACTION to 66,
                SynastryDimension.EMOTIONAL to 50,
                SynastryDimension.COMMUNICATION to 68,
                SynastryDimension.GROWTH to 63,
            ),
            longAmplitude = amplitudes(14.0, 11.0, 12.0, 14.0),
            shortAmplitude = amplitudes(5.0, 4.0, 6.0, 5.0),
            microAmplitude = 2.2,
            longPeriodDays = 31,
            shortPeriodDays = 7,
        )
        val earthWater = PairFamily(
            key = "earth_water",
            centers = mapOf(
                SynastryDimension.ATTRACTION to 52,
                SynastryDimension.EMOTIONAL to 67,
                SynastryDimension.COMMUNICATION to 54,
                SynastryDimension.GROWTH to 56,
            ),
            longAmplitude = amplitudes(9.0, 13.0, 9.0, 10.0),
            shortAmplitude = amplitudes(4.0, 4.0, 3.0, 4.0),
            microAmplitude = 1.8,
            longPeriodDays = 35,
            shortPeriodDays = 8,
        )
        val fireWater = PairFamily(
            key = "fire_water",
            centers = mapOf(
                SynastryDimension.ATTRACTION to 64,
                SynastryDimension.EMOTIONAL to 47,
                SynastryDimension.COMMUNICATION to 49,
                SynastryDimension.GROWTH to 66,
            ),
            longAmplitude = amplitudes(16.0, 14.0, 11.0, 17.0),
            shortAmplitude = amplitudes(6.0, 5.0, 4.0, 6.0),
            microAmplitude = 2.5,
            longPeriodDays = 29,
            shortPeriodDays = 6,
        )
        val airEarth = PairFamily(
            key = "air_earth",
            centers = mapOf(
                SynastryDimension.ATTRACTION to 55,
                SynastryDimension.EMOTIONAL to 48,
                SynastryDimension.COMMUNICATION to 62,
                SynastryDimension.GROWTH to 61,
            ),
            longAmplitude = amplitudes(10.0, 10.0, 13.0, 12.0),
            shortAmplitude = amplitudes(4.0, 4.0, 5.0, 5.0),
            microAmplitude = 2.0,
            longPeriodDays = 33,
            shortPeriodDays = 7,
        )
        val opposed = PairFamily(
            key = "opposed",
            centers = mapOf(
                SynastryDimension.ATTRACTION to 67,
                SynastryDimension.EMOTIONAL to 52,
                SynastryDimension.COMMUNICATION to 58,
                SynastryDimension.GROWTH to 68,
            ),
            longAmplitude = amplitudes(18.0, 14.0, 13.0, 18.0),
            shortAmplitude = amplitudes(6.0, 5.0, 5.0, 7.0),
            microAmplitude = 2.7,
            longPeriodDays = 28,
            shortPeriodDays = 6,
        )
        fun sameElement(element: AstroElement): PairFamily = PairFamily(
            key = "same_${element.name.lowercase()}",
            centers = mapOf(
                SynastryDimension.ATTRACTION to 58,
                SynastryDimension.EMOTIONAL to 60,
                SynastryDimension.COMMUNICATION to 60,
                SynastryDimension.GROWTH to 54,
            ),
            longAmplitude = amplitudes(10.0, 9.0, 9.0, 11.0),
            shortAmplitude = amplitudes(3.5, 3.5, 3.5, 4.0),
            microAmplitude = 1.5,
            longPeriodDays = 36,
            shortPeriodDays = 9,
        )

        val mixed = PairFamily(
            key = "mixed",
            centers = mapOf(
                SynastryDimension.ATTRACTION to 57,
                SynastryDimension.EMOTIONAL to 55,
                SynastryDimension.COMMUNICATION to 56,
                SynastryDimension.GROWTH to 58,
            ),
            longAmplitude = amplitudes(11.0, 11.0, 11.0, 12.0),
            shortAmplitude = amplitudes(4.0, 4.0, 4.0, 5.0),
            microAmplitude = 2.0,
            longPeriodDays = 32,
            shortPeriodDays = 7,
        )

        private fun amplitudes(
            attraction: Double,
            emotional: Double,
            communication: Double,
            growth: Double,
        ): Map<SynastryDimension, Double> = mapOf(
            SynastryDimension.ATTRACTION to attraction,
            SynastryDimension.EMOTIONAL to emotional,
            SynastryDimension.COMMUNICATION to communication,
            SynastryDimension.GROWTH to growth,
        )
    }
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
