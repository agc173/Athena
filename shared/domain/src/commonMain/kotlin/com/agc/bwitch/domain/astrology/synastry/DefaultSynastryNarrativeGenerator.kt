package com.agc.bwitch.domain.astrology.synastry

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
        val personAName = input.personA.displayName ?: "Persona A"
        val personBName = input.personB.displayName ?: "Persona B"
        val orderedDimensions = structured.scores.entries.sortedByDescending { it.value.value }
        val strongestDimension = orderedDimensions.first().key
        val challengeDimension = orderedDimensions.last().key
        val primaryStrength = structured.strengths.firstOrNull()
        val primaryTension = structured.tensions.firstOrNull()
        val primaryGuidance = structured.guidance.firstOrNull()

        val paragraphOne = buildString {
            append("$personAName y $personBName sostienen una energía ${structured.archetype.humanLabel()}. ")
            append(structured.archetype.openingInsight())
            append(" ")
            append("La conexión se siente especialmente viva en ${strongestDimension.humanLabelWithArticle()}.")
        }

        val paragraphTwo = buildString {
            append("Entre sus fortalezas destaca ${primaryStrength?.humanLabel() ?: strongestDimension.strengthFallbackCopy()}. ")
            append("También hay un potencial claro en ${strongestDimension.humanLabel()} para construir acuerdos y sostener el vínculo con más presencia.")
        }

        val paragraphThree = buildString {
            val tensionCopy = primaryTension?.humanLabel()
                ?: "el vínculo pide atención consciente en ${challengeDimension.humanLabel()}"
            append("Cuando aparece fricción, suele notarse en $tensionCopy. ")
            append(
                primaryGuidance?.let { "La clave está en ${it.humanLabel()}." }
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

private fun SynastryBondArchetype.humanLabel(): String = when (this) {
    SynastryBondArchetype.MAGNETIC -> "magnética"
    SynastryBondArchetype.MIRROR -> "espejo"
    SynastryBondArchetype.ALCHEMICAL -> "alquímica"
    SynastryBondArchetype.ANCHOR -> "ancla"
    SynastryBondArchetype.STORM -> "tormentosa"
    SynastryBondArchetype.DEVOTIONAL -> "devocional"
    SynastryBondArchetype.ELECTRIC -> "eléctrica"
    SynastryBondArchetype.COSMIC_DANCE -> "de danza cósmica"
}

private fun SynastryBondArchetype.openingInsight(): String = when (this) {
    SynastryBondArchetype.MAGNETIC -> "Hay atracción natural y una sensación de encuentro significativo."
    SynastryBondArchetype.MIRROR -> "Se reflejan mutuamente y eso facilita comprenderse con profundidad."
    SynastryBondArchetype.ALCHEMICAL -> "La relación invita a transformarse y a crecer a través de las diferencias."
    SynastryBondArchetype.ANCHOR -> "Predomina una sensación de sostén, calma y construcción a largo plazo."
    SynastryBondArchetype.STORM -> "La intensidad es alta y pide madurez emocional para canalizarla bien."
    SynastryBondArchetype.DEVOTIONAL -> "Hay ternura, compromiso y un deseo genuino de cuidarse."
    SynastryBondArchetype.ELECTRIC -> "La chispa mental y el dinamismo mantienen la conexión en movimiento."
    SynastryBondArchetype.COSMIC_DANCE -> "El intercambio fluye con matices, combinando armonía y aprendizaje."
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
    SynastryDimension.EMOTIONAL -> "la facilidad para conectar desde lo sensible"
    SynastryDimension.COMMUNICATION -> "la forma en que dialogan y se entienden"
    SynastryDimension.ATTRACTION -> "la química que aparece entre ambos"
    SynastryDimension.STABILITY -> "la capacidad de sostenerse con constancia"
    SynastryDimension.GROWTH -> "la apertura para evolucionar juntos"
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
    SynastryReadingDepth.BASIC -> "Con la Luna y el Ascendente de ambas personas, esta lectura podría revelar matices más profundos."
    SynastryReadingDepth.PARTIAL -> "Con los datos faltantes, la lectura puede ganar precisión y mostrar matices más finos."
    SynastryReadingDepth.COMPLETE -> "Con la información disponible, el mapa del vínculo se percibe sólido y bien delineado."
}
