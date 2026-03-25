package com.agc.bwitch.domain.astrology.synastry

import com.agc.bwitch.domain.astrology.horoscope.ZodiacSign
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

interface SynastryNarrativeGenerator {
    fun generate(
        input: SynastryInput,
        structured: SynastryReadingStructured,
    ): String
}

class DefaultSynastryNarrativeGenerator : SynastryNarrativeGenerator {

    override fun generate(
        input: SynastryInput,
        structured: SynastryReadingStructured,
    ): String {
        val pairLabel = "${input.personA.sunSign.humanLabel()} y ${input.personB.sunSign.humanLabel()}"
        val orderedDimensions = structured.scores.entries.sortedByDescending { it.value.value }
        val strongestDimension = orderedDimensions.first().key
        val secondDimension = orderedDimensions.getOrNull(1)?.key ?: strongestDimension
        val challengeDimension = orderedDimensions.last().key

        val paragraphOne = buildString {
            append("$pairLabel forman una combinación de ${strongestDimension.humanLabel()} y ${secondDimension.humanLabel()}. ")
            append("El vínculo se percibe vivo cuando alternan impulso y escucha con un ritmo compartido.")
        }

        val paragraphTwo = buildString {
            append("El equilibrio general mejora cuando las diferencias se usan para ajustar expectativas, en lugar de forzar respuestas rápidas. ")
            append("La zona que pide más conciencia es ${challengeDimension.humanLabelWithArticle()}, porque ahí suele definirse el tono de fondo de la relación.")
        }

        val paragraphThree = structured.depthInfo.depth.closingCopy()

        return listOf(paragraphOne, paragraphTwo, paragraphThree).joinToString("\n\n")
    }
}

class SynastryReadingGenerator(
    private val compatibilityResolver: SynastryCompatibilityResolver = DefaultSynastryCompatibilityResolver(),
    private val narrativeGenerator: SynastryNarrativeGenerator = DefaultSynastryNarrativeGenerator(),
    private val dailyOverlayGenerator: SynastryDailyOverlayGenerator = SynastryDailyOverlayGenerator(),
) {
    operator fun invoke(
        input: SynastryInput,
        today: LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date,
    ): SynastryReading {
        val structured = compatibilityResolver.resolve(input, today)
        val dailyOverlay = dailyOverlayGenerator.generate(
            input = input,
            structured = structured,
            date = today,
        )
        return SynastryReading(
            personA = input.personA,
            personB = input.personB,
            structured = structured,
            narrative = narrativeGenerator.generate(input, structured),
            dailyOverlay = dailyOverlay,
        )
    }
}

class SynastryDailyOverlayGenerator {

    fun generate(
        input: SynastryInput,
        structured: SynastryReadingStructured,
        date: LocalDate,
    ): SynastryDailyOverlay {
        val seed = dailySeed(input, date)
        val resolver = DefaultSynastryCompatibilityResolver()
        val axes = resolver.resolveAxisStates(
            scores = structured.scores,
            baseProfile = structured.baseProfile,
            date = date,
            seed = seed,
        )

        val highlightedRanking = rankDimensions(
            dimensions = structured.scores.keys.toList(),
            baseScores = structured.scores,
            axes = axes,
            seed = seed,
            highlighted = true,
        )
        val highlighted = highlightedRanking.first()

        val sensitiveRanking = rankDimensions(
            dimensions = structured.scores.keys.toList(),
            baseScores = structured.scores,
            axes = axes,
            seed = seed + 911,
            highlighted = false,
        )
        val sensitive = sensitiveRanking.firstOrNull { it != highlighted }
            ?: highlightedRanking.firstOrNull { it != highlighted }
            ?: SynastryDimension.entries.first { it != highlighted }

        val primaryAxis = axes.maxByOrNull { kotlin.math.abs(it.value) } ?: axes.first()
        val energyLabel = axisEnergyLabel(primaryAxis)
        val guidance = DAILY_GUIDANCES[(seed and Int.MAX_VALUE) % DAILY_GUIDANCES.size]

        return SynastryDailyOverlay(
            dateIso = date.toString(),
            highlightedDimension = highlighted,
            sensitiveDimension = sensitive,
            dailyEnergyLabel = energyLabel,
            dailyGuidance = guidance,
            dailyNarrativeFragment = buildString {
                append("El clima de hoy se define por ${primaryAxis.toAxisSentence()}. ")
                append("Conviene ajustar el ritmo entre iniciativa y receptividad para sostener el equilibrio.")
            },
            axes = axes,
        )
    }

    private fun rankDimensions(
        dimensions: List<SynastryDimension>,
        baseScores: Map<SynastryDimension, SynastryScore>,
        axes: List<SynastryDailyAxisState>,
        seed: Int,
        highlighted: Boolean,
    ): List<SynastryDimension> {
        return dimensions.sortedByDescending { dimension ->
            val scorePart = if (highlighted) {
                baseScores.getValue(dimension).value * 0.7
            } else {
                (100 - baseScores.getValue(dimension).value) * 0.7
            }
            val axisPart = axisInfluence(dimension, axes, highlighted) * 0.2
            val bias = deterministicDimensionBias(seed, dimension) * 0.1
            scorePart + axisPart + bias
        }
    }

    private fun axisInfluence(
        dimension: SynastryDimension,
        axes: List<SynastryDailyAxisState>,
        highlighted: Boolean,
    ): Double {
        val harmony = axes.firstOrNull { it.axis == SynastryEnergyAxis.HARMONY_INTENSITY }?.value ?: 0
        val stability = axes.firstOrNull { it.axis == SynastryEnergyAxis.STABILITY_TRANSFORMATION }?.value ?: 0
        val movement = axes.firstOrNull { it.axis == SynastryEnergyAxis.CALM_MOVEMENT }?.value ?: 0

        val attractionBias = (harmony * 0.32) + (movement * 0.28) + (stability * 0.12)
        val emotionalBias = (-harmony * 0.22) + (-movement * 0.3) + (-stability * 0.18)
        val communicationBias = (harmony * 0.26) + (movement * 0.18) + (-stability * 0.14)
        val growthBias = (stability * 0.36) + (-harmony * 0.12) + (movement * 0.08)

        val raw = when (dimension) {
            SynastryDimension.ATTRACTION -> attractionBias
            SynastryDimension.EMOTIONAL -> emotionalBias
            SynastryDimension.COMMUNICATION -> communicationBias
            SynastryDimension.GROWTH -> growthBias
        }

        return if (highlighted) raw else -raw
    }

    private fun deterministicDimensionBias(seed: Int, dimension: SynastryDimension): Double {
        val value = (seed * 31) + (dimension.ordinal * 997)
        val normalized = ((value and Int.MAX_VALUE) % 1000) / 1000.0
        return (normalized * 24.0) - 12.0
    }

    private fun dailySeed(input: SynastryInput, date: LocalDate): Int {
        val tokens = listOf(
            input.personA.sunSign.name,
            input.personB.sunSign.name,
            input.personA.moonSign?.name ?: "_",
            input.personB.moonSign?.name ?: "_",
            input.personA.risingSign?.name ?: "_",
            input.personB.risingSign?.name ?: "_",
        ).sorted()
        val key = tokens.joinToString("|") + "|$date"
        return key.fold(23) { acc, c -> (acc * 37) + c.code }
    }

    private fun axisEnergyLabel(primaryAxis: SynastryDailyAxisState): String = when (primaryAxis.axis) {
        SynastryEnergyAxis.HARMONY_INTENSITY -> if (primaryAxis.value >= 0) "Energía de intensidad creativa" else "Energía de armonización"
        SynastryEnergyAxis.STABILITY_TRANSFORMATION -> if (primaryAxis.value >= 0) "Energía de transformación" else "Energía de estabilidad"
        SynastryEnergyAxis.CALM_MOVEMENT -> if (primaryAxis.value >= 0) "Energía de movimiento" else "Energía de calma"
    }
}

private fun SynastryDailyAxisState.toAxisSentence(): String = when (axis) {
    SynastryEnergyAxis.HARMONY_INTENSITY -> if (value >= 0) "más Intensidad que Armonía" else "más Armonía que Intensidad"
    SynastryEnergyAxis.STABILITY_TRANSFORMATION -> if (value >= 0) "más Transformación que Estabilidad" else "más Estabilidad que Transformación"
    SynastryEnergyAxis.CALM_MOVEMENT -> if (value >= 0) "más Movimiento que Calma" else "más Calma que Movimiento"
}

private fun SynastryDimension.humanLabel(): String = when (this) {
    SynastryDimension.EMOTIONAL -> "resonancia emocional"
    SynastryDimension.COMMUNICATION -> "comunicación"
    SynastryDimension.ATTRACTION -> "atracción"
    SynastryDimension.GROWTH -> "crecimiento"
}

private fun SynastryDimension.humanLabelWithArticle(): String = when (this) {
    SynastryDimension.EMOTIONAL -> "la resonancia emocional"
    SynastryDimension.COMMUNICATION -> "la comunicación"
    SynastryDimension.ATTRACTION -> "la atracción"
    SynastryDimension.GROWTH -> "el crecimiento"
}

private fun SynastryDimension.strengthFallbackCopy(): String = when (this) {
    SynastryDimension.EMOTIONAL -> "una sensibilidad compartida"
    SynastryDimension.COMMUNICATION -> "una forma de diálogo que puede ordenarse rápido"
    SynastryDimension.ATTRACTION -> "una química presente desde el inicio"
    SynastryDimension.GROWTH -> "la apertura para evolucionar sin estancarse"
}

private fun SynastryDimension.guidanceFallbackCopy(): String = when (this) {
    SynastryDimension.EMOTIONAL -> "Conviene crear espacios seguros para nombrar lo que sienten con honestidad."
    SynastryDimension.COMMUNICATION -> "Conviene revisar tiempos y formas de diálogo para evitar malentendidos."
    SynastryDimension.ATTRACTION -> "Conviene cuidar el ritmo del vínculo para que la intensidad no opaque la escucha."
    SynastryDimension.GROWTH -> "Conviene mantener conversaciones abiertas sobre expectativas y dirección común."
}

private fun SynastrySignal.humanLabel(): String = when (this) {
    SynastrySignal.STRONG_EMOTIONAL_RESONANCE -> "una resonancia emocional genuina"
    SynastrySignal.DIFFERENT_EMOTIONAL_RHYTHMS -> "ritmos emocionales distintos"
    SynastrySignal.NATURAL_SPARK -> "una chispa espontánea"
    SynastrySignal.COMMUNICATION_FLOW -> "una comunicación que fluye"
    SynastrySignal.STABILITY_POTENTIAL -> "una base estable para sostener el vínculo"
    SynastrySignal.GROWTH_THROUGH_DIFFERENCE -> "la capacidad de crecer a través de la diferencia"
    SynastrySignal.HIGH_INTENSITY -> "una intensidad alta en ciertos momentos"
    SynastrySignal.NEED_FOR_PATIENCE -> "una necesidad de más paciencia entre ambos"
    SynastrySignal.GROUNDING_BOND -> "un lazo que aporta calma y aterrizaje"
    SynastrySignal.MENTAL_STIMULATION -> "una estimulación mental constante"
    SynastrySignal.CREATE_SHARED_RHYTHM -> "crear un ritmo compartido"
    SynastrySignal.USE_DIFFERENCE_AS_GROWTH -> "usar la diferencia como camino de crecimiento"
    SynastrySignal.PROTECT_THE_SOFTNESS -> "proteger la parte más sensible de la relación"
    SynastrySignal.SLOW_DOWN_REACTIVITY -> "bajar la reactividad antes de responder"
}

private fun SynastryReadingDepth.closingCopy(): String = when (this) {
    SynastryReadingDepth.BASIC -> "Con la Luna y el Ascendente de ambas cartas, esta lectura podría revelar matices más profundos."
    SynastryReadingDepth.PARTIAL -> "Con los datos faltantes, la lectura puede ganar precisión y mostrar matices más finos."
    SynastryReadingDepth.COMPLETE -> "Con la información disponible, el mapa del vínculo se percibe sólido y bien delineado."
}

private fun ZodiacSign.humanLabel(): String = when (this) {
    ZodiacSign.aries -> "Aries"
    ZodiacSign.taurus -> "Tauro"
    ZodiacSign.gemini -> "Géminis"
    ZodiacSign.cancer -> "Cáncer"
    ZodiacSign.leo -> "Leo"
    ZodiacSign.virgo -> "Virgo"
    ZodiacSign.libra -> "Libra"
    ZodiacSign.scorpio -> "Escorpio"
    ZodiacSign.sagittarius -> "Sagitario"
    ZodiacSign.capricorn -> "Capricornio"
    ZodiacSign.aquarius -> "Acuario"
    ZodiacSign.pisces -> "Piscis"
}

private val DAILY_GUIDANCES = listOf(
    "Prioricen una conversación breve y clara antes de reaccionar.",
    "Hoy suma más escuchar el ritmo del otro que intentar imponer el propio.",
    "Canalicen la intensidad hacia acuerdos concretos y alcanzables.",
    "Una pequeña muestra de cuidado puede cambiar todo el tono del día.",
    "Revisen expectativas y nombren una intención compartida para hoy.",
    "Eviten suposiciones rápidas: verifiquen lo que cada uno quiso decir.",
)
