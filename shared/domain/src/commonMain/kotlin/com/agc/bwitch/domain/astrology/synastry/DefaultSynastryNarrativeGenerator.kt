package com.agc.bwitch.domain.astrology.synastry

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
        val lang = synastryLang(input.languageCode)
        val pairLabel = when (lang) {
            SynastryLang.EN -> "${input.personA.sunSign.localizedLabel(input.languageCode)} and ${input.personB.sunSign.localizedLabel(input.languageCode)}"
            SynastryLang.PT -> "${input.personA.sunSign.localizedLabel(input.languageCode)} e ${input.personB.sunSign.localizedLabel(input.languageCode)}"
            SynastryLang.RU -> "${input.personA.sunSign.localizedLabel(input.languageCode)} и ${input.personB.sunSign.localizedLabel(input.languageCode)}"
            SynastryLang.FR -> "${input.personA.sunSign.localizedLabel(input.languageCode)} et ${input.personB.sunSign.localizedLabel(input.languageCode)}"
            SynastryLang.IT -> "${input.personA.sunSign.localizedLabel(input.languageCode)} e ${input.personB.sunSign.localizedLabel(input.languageCode)}"
            SynastryLang.DE -> "${input.personA.sunSign.localizedLabel(input.languageCode)} und ${input.personB.sunSign.localizedLabel(input.languageCode)}"
            SynastryLang.ES -> "${input.personA.sunSign.localizedLabel(input.languageCode)} y ${input.personB.sunSign.localizedLabel(input.languageCode)}"
        }

        val orderedDimensions = structured.scores.entries.sortedByDescending { it.value.value }
        val strongestDimension = orderedDimensions.first().key.localizedLabel(input.languageCode).lowercase()
        val secondDimension = (orderedDimensions.getOrNull(1)?.key ?: orderedDimensions.first().key)
            .localizedLabel(input.languageCode)
            .lowercase()
        val challengeDimension = orderedDimensions.last().key.localizedLabel(input.languageCode).lowercase()

        val paragraphOne = when (lang) {
            SynastryLang.ES -> "$pairLabel forman una combinación de $strongestDimension y $secondDimension. El vínculo se percibe vivo cuando alternan impulso y escucha con un ritmo compartido."
            SynastryLang.EN -> "$pairLabel form a combination of $strongestDimension and $secondDimension. The bond feels alive when impulse and listening are balanced through a shared rhythm."
            SynastryLang.PT -> "$pairLabel formam uma combinação de $strongestDimension e $secondDimension. O vínculo ganha vida quando alternam impulso e escuta com ritmo compartilhado."
            SynastryLang.RU -> "$pairLabel образуют сочетание $strongestDimension и $secondDimension. Связь становится живой, когда импульс и внимательное слушание идут в общем ритме."
            SynastryLang.FR -> "$pairLabel forment une combinaison de $strongestDimension et $secondDimension. Le lien reste vivant quand l'élan et l'écoute s'équilibrent dans un rythme partagé."
            SynastryLang.IT -> "$pairLabel formano una combinazione di $strongestDimension e $secondDimension. Il legame resta vivo quando slancio e ascolto si alternano in un ritmo condiviso."
            SynastryLang.DE -> "$pairLabel bilden eine Kombination aus $strongestDimension und $secondDimension. Die Verbindung bleibt lebendig, wenn Impuls und Zuhören in einem gemeinsamen Rhythmus ausbalanciert sind."
        }

        val paragraphTwo = when (lang) {
            SynastryLang.ES -> "El equilibrio general mejora cuando las diferencias se usan para ajustar expectativas, en lugar de forzar respuestas rápidas. La zona que pide más conciencia es $challengeDimension, porque ahí suele definirse el tono de fondo de la relación."
            SynastryLang.EN -> "Overall balance improves when differences are used to adjust expectations instead of forcing quick responses. The area needing more awareness is $challengeDimension, because it usually defines the baseline tone of the relationship."
            SynastryLang.PT -> "O equilíbrio geral melhora quando as diferenças são usadas para ajustar expectativas, em vez de forçar respostas rápidas. A área que pede mais consciência é $challengeDimension, pois ali costuma se definir o tom de fundo da relação."
            SynastryLang.RU -> "Общий баланс улучшается, когда различия помогают согласовывать ожидания, а не подталкивают к быстрым реакциям. Зона, требующая большего внимания, — $challengeDimension, потому что именно там обычно задаётся фоновый тон отношений."
            SynastryLang.FR -> "L'équilibre global s'améliore lorsque les différences servent à ajuster les attentes plutôt qu'à forcer des réponses rapides. La zone qui demande le plus de conscience est $challengeDimension, car c'est souvent là que se définit le ton de fond de la relation."
            SynastryLang.IT -> "L'equilibrio generale migliora quando le differenze vengono usate per regolare le aspettative, invece di forzare risposte rapide. L'area che richiede più consapevolezza è $challengeDimension, perché lì si definisce spesso il tono di fondo della relazione."
            SynastryLang.DE -> "Die Gesamtbalance verbessert sich, wenn Unterschiede genutzt werden, um Erwartungen abzustimmen, statt schnelle Reaktionen zu erzwingen. Der Bereich mit dem größten Bewusstseinsbedarf ist $challengeDimension, weil dort häufig der Grundton der Beziehung entsteht."
        }

        val paragraphThree = when (lang) {
            SynastryLang.ES -> when (structured.depthInfo.depth) {
                SynastryReadingDepth.BASIC -> "Con la Luna y el Ascendente de ambas cartas, esta lectura podría revelar matices más profundos."
                SynastryReadingDepth.PARTIAL -> "Con los datos faltantes, la lectura puede ganar precisión y mostrar matices más finos."
                SynastryReadingDepth.COMPLETE -> "Con la información disponible, el mapa del vínculo se percibe sólido y bien delineado."
            }
            SynastryLang.EN -> when (structured.depthInfo.depth) {
                SynastryReadingDepth.BASIC -> "Adding Moon and Rising for both charts could reveal deeper nuances."
                SynastryReadingDepth.PARTIAL -> "With the missing data, the reading can gain precision and show finer details."
                SynastryReadingDepth.COMPLETE -> "With the current information, the bond map feels solid and well-defined."
            }
            SynastryLang.PT -> when (structured.depthInfo.depth) {
                SynastryReadingDepth.BASIC -> "Com Lua e Ascendente de ambos os mapas, esta leitura pode revelar nuances mais profundas."
                SynastryReadingDepth.PARTIAL -> "Com os dados faltantes, a leitura pode ganhar precisão e mostrar nuances mais finas."
                SynastryReadingDepth.COMPLETE -> "Com as informações disponíveis, o mapa do vínculo se mostra sólido e bem definido."
            }
            SynastryLang.RU -> when (structured.depthInfo.depth) {
                SynastryReadingDepth.BASIC -> "С Луной и Асцендентом обеих карт это чтение может показать более глубокие нюансы."
                SynastryReadingDepth.PARTIAL -> "С недостающими данными чтение станет точнее и покажет более тонкие детали."
                SynastryReadingDepth.COMPLETE -> "С доступной информацией карта связи выглядит цельной и хорошо очерченной."
            }
            SynastryLang.FR -> when (structured.depthInfo.depth) {
                SynastryReadingDepth.BASIC -> "Avec la Lune et l'Ascendant des deux cartes, cette lecture pourrait révéler des nuances plus profondes."
                SynastryReadingDepth.PARTIAL -> "Avec les données manquantes, la lecture peut gagner en précision et montrer des nuances plus fines."
                SynastryReadingDepth.COMPLETE -> "Avec les informations disponibles, la carte du lien paraît solide et bien définie."
            }
            SynastryLang.IT -> when (structured.depthInfo.depth) {
                SynastryReadingDepth.BASIC -> "Con Luna e Ascendente di entrambe le carte, questa lettura può rivelare sfumature più profonde."
                SynastryReadingDepth.PARTIAL -> "Con i dati mancanti, la lettura può guadagnare precisione e mostrare sfumature più sottili."
                SynastryReadingDepth.COMPLETE -> "Con le informazioni disponibili, la mappa del legame risulta solida e ben delineata."
            }
            SynastryLang.DE -> when (structured.depthInfo.depth) {
                SynastryReadingDepth.BASIC -> "Mit Mond und Aszendent beider Karten kann diese Deutung tiefere Nuancen sichtbar machen."
                SynastryReadingDepth.PARTIAL -> "Mit den fehlenden Daten kann die Deutung präziser werden und feinere Nuancen zeigen."
                SynastryReadingDepth.COMPLETE -> "Mit den verfügbaren Informationen wirkt die Beziehungslandkarte solide und klar konturiert."
            }
        }

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

        val lang = synastryLang(input.languageCode)
        val primaryAxis = axes.maxByOrNull { kotlin.math.abs(it.value) } ?: axes.first()
        val energyLabel = axisEnergyLabel(primaryAxis, lang)
        val guidanceCatalog = dailyGuidances(lang)
        val guidance = guidanceCatalog[(seed and Int.MAX_VALUE) % guidanceCatalog.size]
        val sentenceAxis = primaryAxis.toAxisSentence(lang)

        return SynastryDailyOverlay(
            dateIso = date.toString(),
            highlightedDimension = highlighted,
            sensitiveDimension = sensitive,
            dailyEnergyLabel = energyLabel,
            dailyGuidance = guidance,
            dailyNarrativeFragment = when (lang) {
                SynastryLang.ES -> "El clima de hoy se define por $sentenceAxis. Conviene ajustar el ritmo entre iniciativa y receptividad para sostener el equilibrio."
                SynastryLang.EN -> "Today's tone is defined by $sentenceAxis. Adjusting the rhythm between initiative and receptivity helps sustain balance."
                SynastryLang.PT -> "O clima de hoje é definido por $sentenceAxis. Ajustar o ritmo entre iniciativa e receptividade ajuda a sustentar o equilíbrio."
                SynastryLang.RU -> "Тон дня определяется через $sentenceAxis. Полезно согласовать ритм между инициативой и восприимчивостью, чтобы сохранить баланс."
                SynastryLang.FR -> "La tonalité du jour est définie par $sentenceAxis. Ajuster le rythme entre initiative et réceptivité aide à maintenir l'équilibre."
                SynastryLang.IT -> "Il tono di oggi è definito da $sentenceAxis. Regolare il ritmo tra iniziativa e ricettività aiuta a mantenere l'equilibrio."
                SynastryLang.DE -> "Der Ton des Tages wird durch $sentenceAxis bestimmt. Den Rhythmus zwischen Initiative und Aufnahmefähigkeit anzupassen hilft, das Gleichgewicht zu halten."
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

    private fun axisEnergyLabel(primaryAxis: SynastryDailyAxisState, lang: SynastryLang): String = when (lang) {
        SynastryLang.ES -> when (primaryAxis.axis) {
            SynastryEnergyAxis.HARMONY_INTENSITY -> if (primaryAxis.value >= 0) "Energía de intensidad creativa" else "Energía de armonización"
            SynastryEnergyAxis.STABILITY_TRANSFORMATION -> if (primaryAxis.value >= 0) "Energía de transformación" else "Energía de estabilidad"
            SynastryEnergyAxis.CALM_MOVEMENT -> if (primaryAxis.value >= 0) "Energía de movimiento" else "Energía de calma"
        }
        SynastryLang.EN -> when (primaryAxis.axis) {
            SynastryEnergyAxis.HARMONY_INTENSITY -> if (primaryAxis.value >= 0) "Creative intensity energy" else "Harmonizing energy"
            SynastryEnergyAxis.STABILITY_TRANSFORMATION -> if (primaryAxis.value >= 0) "Transformation energy" else "Stability energy"
            SynastryEnergyAxis.CALM_MOVEMENT -> if (primaryAxis.value >= 0) "Movement energy" else "Calm energy"
        }
        SynastryLang.PT -> when (primaryAxis.axis) {
            SynastryEnergyAxis.HARMONY_INTENSITY -> if (primaryAxis.value >= 0) "Energia de intensidade criativa" else "Energia de harmonização"
            SynastryEnergyAxis.STABILITY_TRANSFORMATION -> if (primaryAxis.value >= 0) "Energia de transformação" else "Energia de estabilidade"
            SynastryEnergyAxis.CALM_MOVEMENT -> if (primaryAxis.value >= 0) "Energia de movimento" else "Energia de calma"
        }
        SynastryLang.RU -> when (primaryAxis.axis) {
            SynastryEnergyAxis.HARMONY_INTENSITY -> if (primaryAxis.value >= 0) "Энергия творческой интенсивности" else "Энергия гармонизации"
            SynastryEnergyAxis.STABILITY_TRANSFORMATION -> if (primaryAxis.value >= 0) "Энергия трансформации" else "Энергия стабильности"
            SynastryEnergyAxis.CALM_MOVEMENT -> if (primaryAxis.value >= 0) "Энергия движения" else "Энергия спокойствия"
        }
        SynastryLang.FR -> when (primaryAxis.axis) {
            SynastryEnergyAxis.HARMONY_INTENSITY -> if (primaryAxis.value >= 0) "Énergie d'intensité créative" else "Énergie d'harmonisation"
            SynastryEnergyAxis.STABILITY_TRANSFORMATION -> if (primaryAxis.value >= 0) "Énergie de transformation" else "Énergie de stabilité"
            SynastryEnergyAxis.CALM_MOVEMENT -> if (primaryAxis.value >= 0) "Énergie de mouvement" else "Énergie de calme"
        }
        SynastryLang.IT -> when (primaryAxis.axis) {
            SynastryEnergyAxis.HARMONY_INTENSITY -> if (primaryAxis.value >= 0) "Energia di intensità creativa" else "Energia di armonizzazione"
            SynastryEnergyAxis.STABILITY_TRANSFORMATION -> if (primaryAxis.value >= 0) "Energia di trasformazione" else "Energia di stabilità"
            SynastryEnergyAxis.CALM_MOVEMENT -> if (primaryAxis.value >= 0) "Energia di movimento" else "Energia di calma"
        }
        SynastryLang.DE -> when (primaryAxis.axis) {
            SynastryEnergyAxis.HARMONY_INTENSITY -> if (primaryAxis.value >= 0) "Energie kreativer Intensität" else "Energie der Harmonisierung"
            SynastryEnergyAxis.STABILITY_TRANSFORMATION -> if (primaryAxis.value >= 0) "Energie der Transformation" else "Energie der Stabilität"
            SynastryEnergyAxis.CALM_MOVEMENT -> if (primaryAxis.value >= 0) "Energie der Bewegung" else "Energie der Ruhe"
        }
    }

    private fun SynastryDailyAxisState.toAxisSentence(lang: SynastryLang): String = when (lang) {
        SynastryLang.ES -> when (axis) {
            SynastryEnergyAxis.HARMONY_INTENSITY -> if (value >= 0) "más Intensidad que Armonía" else "más Armonía que Intensidad"
            SynastryEnergyAxis.STABILITY_TRANSFORMATION -> if (value >= 0) "más Transformación que Estabilidad" else "más Estabilidad que Transformación"
            SynastryEnergyAxis.CALM_MOVEMENT -> if (value >= 0) "más Movimiento que Calma" else "más Calma que Movimiento"
        }
        SynastryLang.EN -> when (axis) {
            SynastryEnergyAxis.HARMONY_INTENSITY -> if (value >= 0) "more Intensity than Harmony" else "more Harmony than Intensity"
            SynastryEnergyAxis.STABILITY_TRANSFORMATION -> if (value >= 0) "more Transformation than Stability" else "more Stability than Transformation"
            SynastryEnergyAxis.CALM_MOVEMENT -> if (value >= 0) "more Movement than Calm" else "more Calm than Movement"
        }
        SynastryLang.PT -> when (axis) {
            SynastryEnergyAxis.HARMONY_INTENSITY -> if (value >= 0) "mais Intensidade do que Harmonia" else "mais Harmonia do que Intensidade"
            SynastryEnergyAxis.STABILITY_TRANSFORMATION -> if (value >= 0) "mais Transformação do que Estabilidade" else "mais Estabilidade do que Transformação"
            SynastryEnergyAxis.CALM_MOVEMENT -> if (value >= 0) "mais Movimento do que Calma" else "mais Calma do que Movimento"
        }
        SynastryLang.RU -> when (axis) {
            SynastryEnergyAxis.HARMONY_INTENSITY -> if (value >= 0) "больше Интенсивности, чем Гармонии" else "больше Гармонии, чем Интенсивности"
            SynastryEnergyAxis.STABILITY_TRANSFORMATION -> if (value >= 0) "больше Трансформации, чем Стабильности" else "больше Стабильности, чем Трансформации"
            SynastryEnergyAxis.CALM_MOVEMENT -> if (value >= 0) "больше Движения, чем Спокойствия" else "больше Спокойствия, чем Движения"
        }
        SynastryLang.FR -> when (axis) {
            SynastryEnergyAxis.HARMONY_INTENSITY -> if (value >= 0) "plus d'Intensité que d'Harmonie" else "plus d'Harmonie que d'Intensité"
            SynastryEnergyAxis.STABILITY_TRANSFORMATION -> if (value >= 0) "plus de Transformation que de Stabilité" else "plus de Stabilité que de Transformation"
            SynastryEnergyAxis.CALM_MOVEMENT -> if (value >= 0) "plus de Mouvement que de Calme" else "plus de Calme que de Mouvement"
        }
        SynastryLang.IT -> when (axis) {
            SynastryEnergyAxis.HARMONY_INTENSITY -> if (value >= 0) "più Intensità che Armonia" else "più Armonia che Intensità"
            SynastryEnergyAxis.STABILITY_TRANSFORMATION -> if (value >= 0) "più Trasformazione che Stabilità" else "più Stabilità che Trasformazione"
            SynastryEnergyAxis.CALM_MOVEMENT -> if (value >= 0) "più Movimento che Calma" else "più Calma che Movimento"
        }
        SynastryLang.DE -> when (axis) {
            SynastryEnergyAxis.HARMONY_INTENSITY -> if (value >= 0) "mehr Intensität als Harmonie" else "mehr Harmonie als Intensität"
            SynastryEnergyAxis.STABILITY_TRANSFORMATION -> if (value >= 0) "mehr Transformation als Stabilität" else "mehr Stabilität als Transformation"
            SynastryEnergyAxis.CALM_MOVEMENT -> if (value >= 0) "mehr Bewegung als Ruhe" else "mehr Ruhe als Bewegung"
        }
    }

    private fun dailyGuidances(lang: SynastryLang): List<String> = when (lang) {
        SynastryLang.ES -> listOf(
            "Prioricen una conversación breve y clara antes de reaccionar.",
            "Hoy suma más escuchar el ritmo del otro que intentar imponer el propio.",
            "Canalicen la intensidad hacia acuerdos concretos y alcanzables.",
            "Una pequeña muestra de cuidado puede cambiar todo el tono del día.",
            "Revisen expectativas y nombren una intención compartida para hoy.",
            "Eviten suposiciones rápidas: verifiquen lo que cada uno quiso decir.",
        )
        SynastryLang.EN -> listOf(
            "Prioritize a short, clear conversation before reacting.",
            "Today it helps more to listen to each other's rhythm than to impose your own.",
            "Channel intensity into concrete and reachable agreements.",
            "A small act of care can shift the whole tone of the day.",
            "Review expectations and name a shared intention for today.",
            "Avoid quick assumptions: verify what each person meant.",
        )
        SynastryLang.PT -> listOf(
            "Priorizem uma conversa breve e clara antes de reagir.",
            "Hoje ajuda mais ouvir o ritmo do outro do que impor o próprio.",
            "Canalizem a intensidade para acordos concretos e possíveis.",
            "Um pequeno gesto de cuidado pode mudar todo o tom do dia.",
            "Revisem expectativas e definam uma intenção compartilhada para hoje.",
            "Evitem suposições rápidas: confirmem o que cada pessoa quis dizer.",
        )
        SynastryLang.RU -> listOf(
            "Сначала выберите короткий и ясный разговор, а уже потом реакцию.",
            "Сегодня важнее услышать ритм другого, чем навязывать свой.",
            "Направьте интенсивность в конкретные и достижимые договорённости.",
            "Небольшой жест заботы может изменить весь тон дня.",
            "Пересмотрите ожидания и обозначьте общее намерение на сегодня.",
            "Избегайте поспешных предположений: уточняйте, что имел в виду каждый.",
        )
        SynastryLang.FR -> listOf(
            "Privilégiez une conversation courte et claire avant de réagir.",
            "Aujourd'hui, écouter le rythme de l'autre aide plus que d'imposer le sien.",
            "Canalisez l'intensité vers des accords concrets et atteignables.",
            "Un petit geste d'attention peut changer toute la tonalité du jour.",
            "Revoyez vos attentes et nommez une intention partagée pour aujourd'hui.",
            "Évitez les suppositions rapides : vérifiez ce que chacun voulait dire.",
        )
        SynastryLang.IT -> listOf(
            "Date priorità a un dialogo breve e chiaro prima di reagire.",
            "Oggi aiuta di più ascoltare il ritmo dell'altro che imporre il proprio.",
            "Canalizzate l'intensità verso accordi concreti e realizzabili.",
            "Un piccolo gesto di cura può cambiare tutto il tono della giornata.",
            "Rivedete le aspettative e nominate un'intenzione condivisa per oggi.",
            "Evitate supposizioni rapide: verificate cosa intendeva dire ciascuno.",
        )
        SynastryLang.DE -> listOf(
            "Priorisiert ein kurzes, klares Gespräch, bevor ihr reagiert.",
            "Heute hilft es mehr, den Rhythmus des anderen zu hören, als den eigenen durchzusetzen.",
            "Lenkt die Intensität in konkrete und erreichbare Vereinbarungen.",
            "Eine kleine Geste der Fürsorge kann den ganzen Tageston verändern.",
            "Prüft Erwartungen und benennt eine gemeinsame Intention für heute.",
            "Vermeidet vorschnelle Annahmen: klärt, was jede Person wirklich meinte.",
        )
    }
}
