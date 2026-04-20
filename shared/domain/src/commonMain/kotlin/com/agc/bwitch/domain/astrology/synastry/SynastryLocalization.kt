package com.agc.bwitch.domain.astrology.synastry

import com.agc.bwitch.domain.astrology.horoscope.ZodiacSign

enum class SynastryLang {
    ES, EN, PT, RU, FR, IT, DE
}

fun synastryLang(languageCode: String): SynastryLang = when (languageCode.lowercase().substringBefore('-')) {
    "en" -> SynastryLang.EN
    "pt" -> SynastryLang.PT
    "ru" -> SynastryLang.RU
    "fr" -> SynastryLang.FR
    "it" -> SynastryLang.IT
    "de" -> SynastryLang.DE
    else -> SynastryLang.ES
}

fun ZodiacSign.localizedLabel(languageCode: String): String = when (synastryLang(languageCode)) {
    SynastryLang.ES -> when (this) {
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
    SynastryLang.EN -> when (this) {
        ZodiacSign.aries -> "Aries"
        ZodiacSign.taurus -> "Taurus"
        ZodiacSign.gemini -> "Gemini"
        ZodiacSign.cancer -> "Cancer"
        ZodiacSign.leo -> "Leo"
        ZodiacSign.virgo -> "Virgo"
        ZodiacSign.libra -> "Libra"
        ZodiacSign.scorpio -> "Scorpio"
        ZodiacSign.sagittarius -> "Sagittarius"
        ZodiacSign.capricorn -> "Capricorn"
        ZodiacSign.aquarius -> "Aquarius"
        ZodiacSign.pisces -> "Pisces"
    }
    SynastryLang.PT -> when (this) {
        ZodiacSign.aries -> "Áries"
        ZodiacSign.taurus -> "Touro"
        ZodiacSign.gemini -> "Gêmeos"
        ZodiacSign.cancer -> "Câncer"
        ZodiacSign.leo -> "Leão"
        ZodiacSign.virgo -> "Virgem"
        ZodiacSign.libra -> "Libra"
        ZodiacSign.scorpio -> "Escorpião"
        ZodiacSign.sagittarius -> "Sagitário"
        ZodiacSign.capricorn -> "Capricórnio"
        ZodiacSign.aquarius -> "Aquário"
        ZodiacSign.pisces -> "Peixes"
    }
    SynastryLang.RU -> when (this) {
        ZodiacSign.aries -> "Овен"
        ZodiacSign.taurus -> "Телец"
        ZodiacSign.gemini -> "Близнецы"
        ZodiacSign.cancer -> "Рак"
        ZodiacSign.leo -> "Лев"
        ZodiacSign.virgo -> "Дева"
        ZodiacSign.libra -> "Весы"
        ZodiacSign.scorpio -> "Скорпион"
        ZodiacSign.sagittarius -> "Стрелец"
        ZodiacSign.capricorn -> "Козерог"
        ZodiacSign.aquarius -> "Водолей"
        ZodiacSign.pisces -> "Рыбы"
    }
    SynastryLang.FR -> when (this) {
        ZodiacSign.aries -> "Bélier"
        ZodiacSign.taurus -> "Taureau"
        ZodiacSign.gemini -> "Gémeaux"
        ZodiacSign.cancer -> "Cancer"
        ZodiacSign.leo -> "Lion"
        ZodiacSign.virgo -> "Vierge"
        ZodiacSign.libra -> "Balance"
        ZodiacSign.scorpio -> "Scorpion"
        ZodiacSign.sagittarius -> "Sagittaire"
        ZodiacSign.capricorn -> "Capricorne"
        ZodiacSign.aquarius -> "Verseau"
        ZodiacSign.pisces -> "Poissons"
    }
    SynastryLang.IT -> when (this) {
        ZodiacSign.aries -> "Ariete"
        ZodiacSign.taurus -> "Toro"
        ZodiacSign.gemini -> "Gemelli"
        ZodiacSign.cancer -> "Cancro"
        ZodiacSign.leo -> "Leone"
        ZodiacSign.virgo -> "Vergine"
        ZodiacSign.libra -> "Bilancia"
        ZodiacSign.scorpio -> "Scorpione"
        ZodiacSign.sagittarius -> "Sagittario"
        ZodiacSign.capricorn -> "Capricorno"
        ZodiacSign.aquarius -> "Acquario"
        ZodiacSign.pisces -> "Pesci"
    }
    SynastryLang.DE -> when (this) {
        ZodiacSign.aries -> "Widder"
        ZodiacSign.taurus -> "Stier"
        ZodiacSign.gemini -> "Zwillinge"
        ZodiacSign.cancer -> "Krebs"
        ZodiacSign.leo -> "Löwe"
        ZodiacSign.virgo -> "Jungfrau"
        ZodiacSign.libra -> "Waage"
        ZodiacSign.scorpio -> "Skorpion"
        ZodiacSign.sagittarius -> "Schütze"
        ZodiacSign.capricorn -> "Steinbock"
        ZodiacSign.aquarius -> "Wassermann"
        ZodiacSign.pisces -> "Fische"
    }
}

fun SynastryDimension.localizedLabel(languageCode: String): String = when (synastryLang(languageCode)) {
    SynastryLang.ES -> when (this) {
        SynastryDimension.ATTRACTION -> "Atracción"
        SynastryDimension.EMOTIONAL -> "Sintonía emocional"
        SynastryDimension.COMMUNICATION -> "Comunicación"
        SynastryDimension.GROWTH -> "Potencial de crecimiento"
    }
    SynastryLang.EN -> when (this) {
        SynastryDimension.ATTRACTION -> "Attraction"
        SynastryDimension.EMOTIONAL -> "Emotional resonance"
        SynastryDimension.COMMUNICATION -> "Communication"
        SynastryDimension.GROWTH -> "Growth potential"
    }
    SynastryLang.PT -> when (this) {
        SynastryDimension.ATTRACTION -> "Atração"
        SynastryDimension.EMOTIONAL -> "Sintonia emocional"
        SynastryDimension.COMMUNICATION -> "Comunicação"
        SynastryDimension.GROWTH -> "Potencial de crescimento"
    }
    SynastryLang.RU -> when (this) {
        SynastryDimension.ATTRACTION -> "Притяжение"
        SynastryDimension.EMOTIONAL -> "Эмоциональный резонанс"
        SynastryDimension.COMMUNICATION -> "Коммуникация"
        SynastryDimension.GROWTH -> "Потенциал роста"
    }
    SynastryLang.FR -> when (this) {
        SynastryDimension.ATTRACTION -> "Attraction"
        SynastryDimension.EMOTIONAL -> "Résonance émotionnelle"
        SynastryDimension.COMMUNICATION -> "Communication"
        SynastryDimension.GROWTH -> "Potentiel de croissance"
    }
    SynastryLang.IT -> when (this) {
        SynastryDimension.ATTRACTION -> "Attrazione"
        SynastryDimension.EMOTIONAL -> "Sintonia emotiva"
        SynastryDimension.COMMUNICATION -> "Comunicazione"
        SynastryDimension.GROWTH -> "Potenziale di crescita"
    }
    SynastryLang.DE -> when (this) {
        SynastryDimension.ATTRACTION -> "Anziehung"
        SynastryDimension.EMOTIONAL -> "Emotionale Resonanz"
        SynastryDimension.COMMUNICATION -> "Kommunikation"
        SynastryDimension.GROWTH -> "Wachstumspotenzial"
    }
}

fun SynastryReadingDepth.localizedLabel(languageCode: String): String = when (synastryLang(languageCode)) {
    SynastryLang.ES -> when (this) {
        SynastryReadingDepth.BASIC -> "Lectura esencial"
        SynastryReadingDepth.PARTIAL -> "Lectura ampliada"
        SynastryReadingDepth.COMPLETE -> "Lectura completa"
    }
    SynastryLang.EN -> when (this) {
        SynastryReadingDepth.BASIC -> "Essential reading"
        SynastryReadingDepth.PARTIAL -> "Expanded reading"
        SynastryReadingDepth.COMPLETE -> "Complete reading"
    }
    SynastryLang.PT -> when (this) {
        SynastryReadingDepth.BASIC -> "Leitura essencial"
        SynastryReadingDepth.PARTIAL -> "Leitura ampliada"
        SynastryReadingDepth.COMPLETE -> "Leitura completa"
    }
    SynastryLang.RU -> when (this) {
        SynastryReadingDepth.BASIC -> "Базовое чтение"
        SynastryReadingDepth.PARTIAL -> "Расширенное чтение"
        SynastryReadingDepth.COMPLETE -> "Полное чтение"
    }
    SynastryLang.FR -> when (this) {
        SynastryReadingDepth.BASIC -> "Lecture essentielle"
        SynastryReadingDepth.PARTIAL -> "Lecture élargie"
        SynastryReadingDepth.COMPLETE -> "Lecture complète"
    }
    SynastryLang.IT -> when (this) {
        SynastryReadingDepth.BASIC -> "Lettura essenziale"
        SynastryReadingDepth.PARTIAL -> "Lettura ampliata"
        SynastryReadingDepth.COMPLETE -> "Lettura completa"
    }
    SynastryLang.DE -> when (this) {
        SynastryReadingDepth.BASIC -> "Essenzielle Deutung"
        SynastryReadingDepth.PARTIAL -> "Erweiterte Deutung"
        SynastryReadingDepth.COMPLETE -> "Vollständige Deutung"
    }
}

fun SynastryReading.primaryStrengthCopy(languageCode: String): String = primaryCopy(
    signals = structured.strengths,
    axis = dailyOverlay?.axes?.maxByOrNull { kotlin.math.abs(it.value) },
    seedBase = structured.overallScore.value + personA.sunSign.ordinal + personB.sunSign.ordinal,
    fallback = mapOf(
        SynastryLang.ES to "No se detectó una fortaleza dominante en esta lectura.",
        SynastryLang.EN to "No dominant strength was detected in this reading.",
        SynastryLang.PT to "Nenhuma força dominante foi detectada nesta leitura.",
        SynastryLang.RU to "В этом чтении не обнаружено доминирующей сильной стороны.",
        SynastryLang.FR to "Aucune force dominante n'a été détectée dans cette lecture.",
        SynastryLang.IT to "In questa lettura non è stata rilevata una forza dominante.",
        SynastryLang.DE to "In dieser Deutung wurde keine dominante Stärke erkannt.",
    ),
    languageCode = languageCode,
)

fun SynastryReading.primaryTensionCopy(languageCode: String): String = primaryCopy(
    signals = structured.tensions,
    axis = dailyOverlay?.axes?.maxByOrNull { kotlin.math.abs(it.value) },
    seedBase = structured.overallScore.value + (personA.sunSign.ordinal * 7) + personB.sunSign.ordinal,
    fallback = mapOf(
        SynastryLang.ES to "No se detectó una tensión dominante en esta lectura.",
        SynastryLang.EN to "No dominant tension was detected in this reading.",
        SynastryLang.PT to "Nenhuma tensão dominante foi detectada nesta leitura.",
        SynastryLang.RU to "В этом чтении не обнаружено доминирующего напряжения.",
        SynastryLang.FR to "Aucune tension dominante n'a été détectée dans cette lecture.",
        SynastryLang.IT to "In questa lettura non è stata rilevata una tensione dominante.",
        SynastryLang.DE to "In dieser Deutung wurde keine dominante Spannung erkannt.",
    ),
    languageCode = languageCode,
)

fun SynastryReading.primaryGuidanceCopy(languageCode: String): String = primaryCopy(
    signals = structured.guidance,
    axis = dailyOverlay?.axes?.firstOrNull(),
    seedBase = structured.overallScore.value + (personA.sunSign.ordinal * 17) + personB.sunSign.ordinal,
    fallback = mapOf(
        SynastryLang.ES to "Mantengan una comunicación presente y honesta.",
        SynastryLang.EN to "Keep communication present and honest.",
        SynastryLang.PT to "Mantenham uma comunicação presente e honesta.",
        SynastryLang.RU to "Сохраняйте честную и осознанную коммуникацию.",
        SynastryLang.FR to "Gardez une communication présente et honnête.",
        SynastryLang.IT to "Mantenete una comunicazione presente e onesta.",
        SynastryLang.DE to "Bewahrt eine präsente und ehrliche Kommunikation.",
    ),
    languageCode = languageCode,
)

fun SynastrySignal.copyVariants(languageCode: String, axis: SynastryDailyAxisState?): List<String> {
    val lang = synastryLang(languageCode)
    return when (this) {
        SynastrySignal.STRONG_EMOTIONAL_RESONANCE -> listOf(
            when (lang) {
                SynastryLang.ES -> "Hay una sintonía emocional que facilita comprenderse sin demasiadas explicaciones."
                SynastryLang.EN -> "There is emotional attunement that helps you understand each other with little explanation."
                SynastryLang.PT -> "Há uma sintonia emocional que facilita a compreensão mútua sem muitas explicações."
                SynastryLang.RU -> "Есть эмоциональная сонастройка, которая помогает понимать друг друга почти без объяснений."
                SynastryLang.FR -> "Il y a une résonance émotionnelle qui facilite la compréhension mutuelle sans trop d'explications."
                SynastryLang.IT -> "C'è una sintonia emotiva che facilita la comprensione reciproca senza troppe spiegazioni."
                SynastryLang.DE -> "Es gibt eine emotionale Resonanz, die euch hilft, euch ohne viele Erklärungen zu verstehen."
            }
        )
        SynastrySignal.DIFFERENT_EMOTIONAL_RHYTHMS -> listOf(
            when (lang) {
                SynastryLang.ES -> "Los tiempos emocionales no siempre coinciden, y eso puede generar cruces si no se nombran."
                SynastryLang.EN -> "Emotional timing does not always match, so naming needs clearly will reduce friction."
                SynastryLang.PT -> "Os ritmos emocionais nem sempre coincidem; nomear isso reduz atritos."
                SynastryLang.RU -> "Эмоциональные ритмы не всегда совпадают, поэтому важно прямо проговаривать потребности."
                SynastryLang.FR -> "Les rythmes émotionnels ne coïncident pas toujours; les nommer clairement réduit les frictions."
                SynastryLang.IT -> "I ritmi emotivi non sempre coincidono; esplicitarli riduce gli attriti."
                SynastryLang.DE -> "Emotionale Rhythmen passen nicht immer zusammen; sie klar zu benennen reduziert Reibung."
            }
        )
        SynastrySignal.NATURAL_SPARK -> listOf(
            when (lang) {
                SynastryLang.ES -> "La atracción aparece de forma natural y mantiene vivo el interés entre ambos."
                SynastryLang.EN -> "Attraction appears naturally and keeps the connection alive."
                SynastryLang.PT -> "A atração surge de forma natural e mantém o vínculo vivo."
                SynastryLang.RU -> "Притяжение возникает естественно и поддерживает живой интерес между вами."
                SynastryLang.FR -> "L'attraction apparaît naturellement et maintient le lien vivant."
                SynastryLang.IT -> "L'attrazione nasce in modo naturale e mantiene vivo il legame."
                SynastryLang.DE -> "Die Anziehung entsteht natürlich und hält die Verbindung lebendig."
            }
        )
        SynastrySignal.COMMUNICATION_FLOW -> listOf(
            when (lang) {
                SynastryLang.ES -> "El diálogo tiende a fluir y eso vuelve más simples los ajustes cotidianos."
                SynastryLang.EN -> "Dialogue tends to flow, making daily adjustments easier."
                SynastryLang.PT -> "O diálogo tende a fluir, facilitando ajustes do dia a dia."
                SynastryLang.RU -> "Диалог обычно течёт легко, и это упрощает ежедневные договорённости."
                SynastryLang.FR -> "Le dialogue circule naturellement, ce qui facilite les ajustements du quotidien."
                SynastryLang.IT -> "Il dialogo tende a scorrere, rendendo più semplici gli aggiustamenti quotidiani."
                SynastryLang.DE -> "Der Dialog fließt meist gut und macht alltägliche Abstimmungen einfacher."
            }
        )
        SynastrySignal.STABILITY_POTENTIAL -> listOf(
            when (lang) {
                SynastryLang.ES -> "Se percibe potencial para construir una base estable sin perder cercanía."
                SynastryLang.EN -> "There is potential to build a stable base without losing closeness."
                SynastryLang.PT -> "Há potencial para construir uma base estável sem perder proximidade."
                SynastryLang.RU -> "Есть потенциал выстроить устойчивую основу, не теряя близости."
                SynastryLang.FR -> "Il existe un potentiel pour construire une base stable sans perdre la proximité."
                SynastryLang.IT -> "C'è potenziale per costruire una base stabile senza perdere vicinanza."
                SynastryLang.DE -> "Es gibt Potenzial, eine stabile Basis aufzubauen, ohne Nähe zu verlieren."
            }
        )
        SynastrySignal.GROWTH_THROUGH_DIFFERENCE -> listOf(
            when (lang) {
                SynastryLang.ES -> "Las diferencias pueden convertirse en evolución si se usan como aprendizaje mutuo."
                SynastryLang.EN -> "Differences can become growth when used as mutual learning."
                SynastryLang.PT -> "As diferenças podem virar evolução quando usadas como aprendizado mútuo."
                SynastryLang.RU -> "Различия могут стать ростом, если использовать их как взаимное обучение."
                SynastryLang.FR -> "Les différences peuvent devenir un levier d'évolution si elles servent d'apprentissage mutuel."
                SynastryLang.IT -> "Le differenze possono diventare crescita se usate come apprendimento reciproco."
                SynastryLang.DE -> "Unterschiede können zu Wachstum werden, wenn ihr sie als gegenseitiges Lernen nutzt."
            }
        )
        SynastrySignal.HIGH_INTENSITY -> listOf(
            when (lang) {
                SynastryLang.ES -> "La intensidad es alta y puede desbordar si no acompañan con pausas conscientes."
                SynastryLang.EN -> "Intensity is high and may overflow without conscious pauses."
                SynastryLang.PT -> "A intensidade está alta e pode transbordar sem pausas conscientes."
                SynastryLang.RU -> "Интенсивность высока и может захлестнуть без осознанных пауз."
                SynastryLang.FR -> "L'intensité est forte et peut déborder sans pauses conscientes."
                SynastryLang.IT -> "L'intensità è alta e può traboccare senza pause consapevoli."
                SynastryLang.DE -> "Die Intensität ist hoch und kann ohne bewusste Pausen überlaufen."
            }
        )
        SynastrySignal.NEED_FOR_PATIENCE -> listOf(
            when (lang) {
                SynastryLang.ES -> "La comunicación pide más paciencia para evitar interpretaciones rápidas."
                SynastryLang.EN -> "Communication asks for more patience to avoid quick interpretations."
                SynastryLang.PT -> "A comunicação pede mais paciência para evitar interpretações apressadas."
                SynastryLang.RU -> "Коммуникации нужно больше терпения, чтобы избежать поспешных выводов."
                SynastryLang.FR -> "La communication demande plus de patience pour éviter les interprétations rapides."
                SynastryLang.IT -> "La comunicazione richiede più pazienza per evitare interpretazioni affrettate."
                SynastryLang.DE -> "Die Kommunikation braucht mehr Geduld, um vorschnelle Deutungen zu vermeiden."
            }
        )
        SynastrySignal.GROUNDING_BOND -> listOf(
            when (lang) {
                SynastryLang.ES -> "Predomina una energía de anclaje que ayuda a bajar ruido y enfocarse en lo importante."
                SynastryLang.EN -> "A grounding energy helps reduce noise and focus on what matters."
                SynastryLang.PT -> "Predomina uma energia de ancoragem que ajuda a reduzir ruído e focar no essencial."
                SynastryLang.RU -> "Преобладает заземляющая энергия, которая помогает снизить шум и сфокусироваться на главном."
                SynastryLang.FR -> "Une énergie d'ancrage domine et aide à réduire le bruit pour se concentrer sur l'essentiel."
                SynastryLang.IT -> "Predomina un'energia di radicamento che aiuta a ridurre il rumore e a concentrarsi sull'essenziale."
                SynastryLang.DE -> "Eine erdende Energie dominiert und hilft, Störrauschen zu senken und das Wesentliche zu sehen."
            }
        )
        SynastrySignal.MENTAL_STIMULATION -> listOf(
            when (lang) {
                SynastryLang.ES -> "Hay estímulo mental mutuo y eso mantiene viva la curiosidad entre ambos."
                SynastryLang.EN -> "Mutual mental stimulation keeps curiosity alive."
                SynastryLang.PT -> "Há estímulo mental mútuo e isso mantém a curiosidade viva entre vocês."
                SynastryLang.RU -> "Есть взаимная интеллектуальная стимуляция, и это поддерживает живое любопытство."
                SynastryLang.FR -> "Il y a une stimulation mentale mutuelle qui garde la curiosité bien vivante."
                SynastryLang.IT -> "C'è una stimolazione mentale reciproca che mantiene viva la curiosità."
                SynastryLang.DE -> "Es gibt gegenseitige mentale Stimulation, die eure Neugier lebendig hält."
            }
        )
        SynastrySignal.CREATE_SHARED_RHYTHM -> listOf(
            when (lang) {
                SynastryLang.ES -> "La clave es crear un ritmo compartido para no entrar en sincronías forzadas."
                SynastryLang.EN -> "Create a shared rhythm to avoid forced synchronization."
                SynastryLang.PT -> "A chave é criar um ritmo compartilhado para evitar sincronias forçadas."
                SynastryLang.RU -> "Ключ в том, чтобы создать общий ритм и не входить в принудительную синхронизацию."
                SynastryLang.FR -> "La clé est de créer un rythme commun pour éviter les synchronisations forcées."
                SynastryLang.IT -> "La chiave è creare un ritmo condiviso per evitare sincronie forzate."
                SynastryLang.DE -> "Der Schlüssel ist, einen gemeinsamen Rhythmus zu schaffen, um erzwungene Synchronisation zu vermeiden."
            }
        )
        SynastrySignal.USE_DIFFERENCE_AS_GROWTH -> listOf(
            when (lang) {
                SynastryLang.ES -> "La guía principal es usar la diferencia como recurso de crecimiento."
                SynastryLang.EN -> "Use differences as a growth path."
                SynastryLang.PT -> "A orientação principal é usar a diferença como caminho de crescimento."
                SynastryLang.RU -> "Главный ориентир — использовать различия как путь к росту."
                SynastryLang.FR -> "L'orientation principale est d'utiliser la différence comme chemin de croissance."
                SynastryLang.IT -> "La guida principale è usare la differenza come via di crescita."
                SynastryLang.DE -> "Die zentrale Orientierung ist, Unterschiede als Wachstumsweg zu nutzen."
            }
        )
        SynastrySignal.PROTECT_THE_SOFTNESS -> listOf(
            when (lang) {
                SynastryLang.ES -> "Conviene cuidar la parte sensible del vínculo antes de discutir formas."
                SynastryLang.EN -> "Protect the sensitive part of the bond before discussing form."
                SynastryLang.PT -> "Convém proteger a parte sensível do vínculo antes de discutir formas."
                SynastryLang.RU -> "Стоит сначала защитить чувствительную часть связи, а уже потом обсуждать форму."
                SynastryLang.FR -> "Mieux vaut protéger la partie sensible du lien avant de discuter des formes."
                SynastryLang.IT -> "Conviene proteggere la parte sensibile del legame prima di discutere le forme."
                SynastryLang.DE -> "Schützt zuerst den sensiblen Teil eurer Verbindung, bevor ihr über Formen diskutiert."
            }
        )
        SynastrySignal.SLOW_DOWN_REACTIVITY -> listOf(
            when (lang) {
                SynastryLang.ES -> if ((axis?.value ?: 0) >= 0) "La guía es bajar un punto la aceleración para responder con más conciencia." else "Conviene pausar antes de reaccionar para que el diálogo no se tense de más."
                SynastryLang.EN -> if ((axis?.value ?: 0) >= 0) "Lower the pace before responding." else "Pause before reacting to avoid escalation."
                SynastryLang.PT -> if ((axis?.value ?: 0) >= 0) "A orientação é reduzir o ritmo antes de responder." else "Vale pausar antes de reagir para evitar escalada."
                SynastryLang.RU -> if ((axis?.value ?: 0) >= 0) "Рекомендация — немного снизить темп перед ответом." else "Лучше сделать паузу перед реакцией, чтобы не усиливать напряжение."
                SynastryLang.FR -> if ((axis?.value ?: 0) >= 0) "Le conseil est de ralentir légèrement avant de répondre." else "Mieux vaut faire une pause avant de réagir pour éviter l'escalade."
                SynastryLang.IT -> if ((axis?.value ?: 0) >= 0) "La guida è rallentare un momento prima di rispondere." else "Conviene fare una pausa prima di reagire per evitare escalation."
                SynastryLang.DE -> if ((axis?.value ?: 0) >= 0) "Der Hinweis ist, vor der Antwort kurz Tempo herauszunehmen." else "Macht besser eine Pause, bevor ihr reagiert, um Eskalation zu vermeiden."
            }
        )
    }
}

private fun SynastryReading.primaryCopy(
    signals: List<SynastrySignal>,
    axis: SynastryDailyAxisState?,
    seedBase: Int,
    fallback: Map<SynastryLang, String>,
    languageCode: String,
): String {
    val lang = synastryLang(languageCode)
    val primarySignal = signals.firstOrNull() ?: return fallback.getValue(lang)
    val variants = primarySignal.copyVariants(languageCode, axis)
    val index = ((seedBase * 31) + primarySignal.ordinal + (axis?.value ?: 0)).absoluteValue() % variants.size
    return variants[index]
}

private fun Int.absoluteValue(): Int = kotlin.math.abs(this)
