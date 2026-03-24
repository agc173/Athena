package com.agc.bwitch.domain.astrology.synastry

import com.agc.bwitch.domain.astrology.horoscope.ZodiacSign

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
        val challengeDimension = orderedDimensions.last().key
        val primaryStrength = structured.strengths.firstOrNull()
        val primaryTension = structured.tensions.firstOrNull()
        val primaryGuidance = structured.guidance.firstOrNull()

        val paragraphOne = buildString {
            append("$pairLabel muestran una dinámica con foco en ${strongestDimension.humanLabelWithArticle()}. ")
            append("El tono general combina química, aprendizaje y margen real de construcción.")
        }

        val paragraphTwo = buildString {
            append("Entre las fortalezas aparece ${primaryStrength?.humanLabel() ?: strongestDimension.strengthFallbackCopy()}. ")
            append("Cuando esta área se cuida, el vínculo gana continuidad y dirección compartida.")
        }

        val paragraphThree = buildString {
            val tensionCopy = primaryTension?.humanLabel()
                ?: "retos visibles en ${challengeDimension.humanLabel()}"
            append("La fricción tiende a notarse en $tensionCopy. ")
            append(
                primaryGuidance?.let { "La guía más útil es ${it.humanLabel()}." }
                    ?: challengeDimension.guidanceFallbackCopy()
            )
            append(" ")
            append(structured.depthInfo.depth.closingCopy())
        }

        return listOf(paragraphOne, paragraphTwo, paragraphThree).joinToString("\n\n")
    }
}

class SynastryReadingGenerator(
    private val compatibilityResolver: SynastryCompatibilityResolver = DefaultSynastryCompatibilityResolver(),
    private val narrativeGenerator: SynastryNarrativeGenerator = DefaultSynastryNarrativeGenerator(),
) {
    operator fun invoke(input: SynastryInput): SynastryReading {
        val structured = compatibilityResolver.resolve(input)
        return SynastryReading(
            personA = input.personA,
            personB = input.personB,
            structured = structured,
            narrative = narrativeGenerator.generate(input, structured),
        )
    }
}

private fun SynastryDimension.humanLabel(): String = when (this) {
    SynastryDimension.EMOTIONAL -> "resonancia emocional"
    SynastryDimension.COMMUNICATION -> "comunicación"
    SynastryDimension.ATTRACTION -> "atracción"
    SynastryDimension.STABILITY -> "estabilidad"
    SynastryDimension.GROWTH -> "crecimiento"
}

private fun SynastryDimension.humanLabelWithArticle(): String = when (this) {
    SynastryDimension.EMOTIONAL -> "la resonancia emocional"
    SynastryDimension.COMMUNICATION -> "la comunicación"
    SynastryDimension.ATTRACTION -> "la atracción"
    SynastryDimension.STABILITY -> "la estabilidad"
    SynastryDimension.GROWTH -> "el crecimiento"
}

private fun SynastryDimension.strengthFallbackCopy(): String = when (this) {
    SynastryDimension.EMOTIONAL -> "una sensibilidad compartida"
    SynastryDimension.COMMUNICATION -> "una forma de diálogo que puede ordenarse rápido"
    SynastryDimension.ATTRACTION -> "una química presente desde el inicio"
    SynastryDimension.STABILITY -> "la capacidad de sostener acuerdos concretos"
    SynastryDimension.GROWTH -> "la apertura para evolucionar sin estancarse"
}

private fun SynastryDimension.guidanceFallbackCopy(): String = when (this) {
    SynastryDimension.EMOTIONAL -> "Conviene crear espacios seguros para nombrar lo que sienten con honestidad."
    SynastryDimension.COMMUNICATION -> "Conviene revisar tiempos y formas de diálogo para evitar malentendidos."
    SynastryDimension.ATTRACTION -> "Conviene cuidar el ritmo del vínculo para que la intensidad no opaque la escucha."
    SynastryDimension.STABILITY -> "Conviene construir pequeñas rutinas compartidas que den más sostén."
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
