package com.agc.bwitch.domain.astrology.synastry

import com.agc.bwitch.domain.astrology.horoscope.ZodiacSign

data class SynastryPersonInput(
    val displayName: String? = null,
    val sunSign: ZodiacSign,
    val moonSign: ZodiacSign? = null,
    val risingSign: ZodiacSign? = null,
)

data class SynastryInput(
    val personA: SynastryPersonInput,
    val personB: SynastryPersonInput,
)

enum class SynastryReadingDepth {
    BASIC,
    PARTIAL,
    COMPLETE,
}

enum class SynastryConfidenceLevel {
    LOW,
    MEDIUM,
    HIGH,
}

enum class SynastryDimension {
    EMOTIONAL,
    COMMUNICATION,
    ATTRACTION,
    STABILITY,
    GROWTH,
}

enum class SynastryBondArchetype {
    MAGNETIC,
    MIRROR,
    ALCHEMICAL,
    ANCHOR,
    STORM,
    DEVOTIONAL,
    ELECTRIC,
    COSMIC_DANCE,
}

enum class SynastrySignal {
    STRONG_EMOTIONAL_RESONANCE,
    DIFFERENT_EMOTIONAL_RHYTHMS,
    NATURAL_SPARK,
    COMMUNICATION_FLOW,
    STABILITY_POTENTIAL,
    GROWTH_THROUGH_DIFFERENCE,
    HIGH_INTENSITY,
    NEED_FOR_PATIENCE,
    GROUNDING_BOND,
    MENTAL_STIMULATION,
    CREATE_SHARED_RHYTHM,
    USE_DIFFERENCE_AS_GROWTH,
    PROTECT_THE_SOFTNESS,
    SLOW_DOWN_REACTIVITY,
}

enum class SynastryInsightTag {
    HARMONIOUS,
    INTENSE,
    GROUNDING,
    EVOLUTIVE,
    COMMUNICATIVE,
    EMOTIONAL,
    NEEDS_PRACTICE,
}

@JvmInline
value class SynastryScore private constructor(val value: Int) {
    companion object {
        fun from(rawValue: Int): SynastryScore = SynastryScore(rawValue.coerceIn(0, 100))
    }
}

data class SynastryDataCompleteness(
    val personAHasMoon: Boolean,
    val personAHasRising: Boolean,
    val personBHasMoon: Boolean,
    val personBHasRising: Boolean,
    val availablePoints: Int,
    val depth: SynastryReadingDepth,
)

data class SynastryReadingStructured(
    val depthInfo: SynastryDataCompleteness,
    val confidenceLevel: SynastryConfidenceLevel,
    val archetype: SynastryBondArchetype,
    val overallScore: SynastryScore,
    val scores: Map<SynastryDimension, SynastryScore>,
    val strengths: List<SynastrySignal>,
    val tensions: List<SynastrySignal>,
    val guidance: List<SynastrySignal>,
    val tags: List<SynastryInsightTag>,
)

data class SynastryReading(
    val personA: SynastryPersonInput,
    val personB: SynastryPersonInput,
    val structured: SynastryReadingStructured,
    val narrative: String,
)
