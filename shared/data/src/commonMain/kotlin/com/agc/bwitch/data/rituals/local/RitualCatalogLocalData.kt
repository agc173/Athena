package com.agc.bwitch.data.rituals.local

import com.agc.bwitch.domain.rituals.RitualCategory
import com.agc.bwitch.domain.rituals.RitualCategoryType
import com.agc.bwitch.domain.rituals.RitualDetail

internal val localRitualCategories = listOf(
    RitualCategory(
        type = RitualCategoryType.Love,
        title = "Amor",
        subtitle = "Conecta contigo y abre espacio para vínculos conscientes.",
    ),
    RitualCategory(
        type = RitualCategoryType.Prosperity,
        title = "Prosperidad",
        subtitle = "Ordena tu energía para atraer abundancia y enfoque.",
    ),
    RitualCategory(
        type = RitualCategoryType.Protection,
        title = "Protección",
        subtitle = "Fortalece tus límites energéticos con intención clara.",
    ),
    RitualCategory(
        type = RitualCategoryType.Cleansing,
        title = "Limpieza",
        subtitle = "Libera cargas y renueva tu campo antes de avanzar.",
    ),
)

internal val localRitualDetails = listOf(
    // Amor (3)
    RitualDetail(
        id = "love_mirror_intention",
        category = RitualCategoryType.Love,
        title = "Espejo de intención amorosa",
        subtitle = "Un momento breve para elegirte primero.",
        intention = "Cultivar amor propio antes de compartirlo con otras personas.",
        materials = listOf("Un espejo pequeño", "Vela blanca", "Cuaderno"),
        preparation = "Busca un lugar en calma y enciende la vela.",
        action = "Mírate al espejo y di tres cualidades que honras en ti.",
        closing = "Escribe una frase de amor propio y agradece en voz baja.",
        optionalNote = "Puedes repetirlo antes de una cita o conversación importante.",
    ),
    RitualDetail(
        id = "love_rose_bath",
        category = RitualCategoryType.Love,
        title = "Baño de rosa y calma",
        subtitle = "Suaviza la emoción y vuelve al centro.",
        intention = "Abrir espacio para la ternura y la receptividad.",
        materials = listOf("Agua tibia", "Pétalos o esencia de rosa", "Toalla"),
        action = "Sumerge manos o pies y respira lento durante dos minutos.",
        closing = "Repite: 'Estoy a salvo para amar y recibir amor'.",
    ),
    RitualDetail(
        id = "love_letter_heart",
        category = RitualCategoryType.Love,
        title = "Carta al corazón",
        subtitle = "Escucha tu necesidad emocional del presente.",
        intention = "Fortalecer el vínculo interno con compasión y honestidad.",
        materials = listOf("Hoja", "Bolígrafo", "Sobre"),
        preparation = "Respira profundo durante un minuto antes de escribir.",
        action = "Escribe una carta corta comenzando con 'Querido corazón, hoy necesito...'.",
        closing = "Guarda la carta y agradece tu valentía emocional.",
    ),

    // Prosperidad (3)
    RitualDetail(
        id = "prosperity_coin_focus",
        category = RitualCategoryType.Prosperity,
        title = "Moneda de enfoque",
        subtitle = "Convierte intención en acción concreta.",
        intention = "Alinear tus decisiones con una meta de prosperidad.",
        materials = listOf("Una moneda", "Papel", "Bolígrafo"),
        preparation = "Define una meta simple para esta semana.",
        action = "Escribe una acción de hoy y guarda la moneda junto al papel.",
        closing = "Empieza esa acción dentro de los próximos cinco minutos.",
    ),
    RitualDetail(
        id = "prosperity_desk_reset",
        category = RitualCategoryType.Prosperity,
        title = "Reset de escritorio abundante",
        subtitle = "Orden externo para claridad mental.",
        intention = "Crear un entorno que favorezca foco y productividad.",
        materials = listOf("Paño", "Recipiente pequeño", "Incienso opcional"),
        action = "Limpia tu mesa y deja a la vista solo lo esencial para tu objetivo.",
        closing = "Define la primera tarea y ejecútala sin distracciones.",
    ),
    RitualDetail(
        id = "prosperity_wallet_blessing",
        category = RitualCategoryType.Prosperity,
        title = "Bendición de cartera",
        subtitle = "Ordena tu relación energética con el dinero.",
        intention = "Transformar escasez mental en gratitud y responsabilidad.",
        materials = listOf("Cartera", "Billete o moneda", "Canela en polvo"),
        preparation = "Vacía tu cartera y retira papeles que ya no uses.",
        action = "Coloca una pizca de canela y repite una afirmación de abundancia consciente.",
        closing = "Vuelve a guardar solo lo necesario y ordenado.",
    ),

    // Protección (3)
    RitualDetail(
        id = "protection_salt_circle",
        category = RitualCategoryType.Protection,
        title = "Círculo de sal consciente",
        subtitle = "Refuerza tus límites antes de exponerte.",
        intention = "Sostener tu energía en interacciones demandantes.",
        materials = listOf("Sal gruesa", "Vaso con agua"),
        preparation = "Marca un pequeño perímetro simbólico con sal.",
        action = "Respira un minuto y repite tu afirmación de protección.",
        closing = "Recoge la sal con gratitud y desecha fuera de casa.",
    ),
    RitualDetail(
        id = "protection_hand_seal",
        category = RitualCategoryType.Protection,
        title = "Sello de manos",
        subtitle = "Ancla corporal para sentir seguridad.",
        intention = "Activar presencia y límites personales claros.",
        materials = listOf("Tus manos", "Un lugar tranquilo"),
        action = "Frota tus manos, colócalas en pecho y abdomen y respira profundo.",
        closing = "Visualiza una luz envolvente y afirma: 'Yo decido qué permito'.",
    ),
    RitualDetail(
        id = "protection_doorway_intent",
        category = RitualCategoryType.Protection,
        title = "Umbral protegido",
        subtitle = "Sella energéticamente la entrada de tu espacio.",
        intention = "Cuidar tu hogar como lugar de descanso y recuperación.",
        materials = listOf("Agua con sal", "Paño limpio", "Aceite esencial opcional"),
        preparation = "Humedece el paño con agua y una pizca de sal.",
        action = "Limpia marco y picaporte con intención de protección y calma.",
        closing = "Di: 'Aquí habitan paz, respeto y claridad'.",
    ),

    // Limpieza (3)
    RitualDetail(
        id = "cleansing_smoke_release",
        category = RitualCategoryType.Cleansing,
        title = "Humo de liberación",
        subtitle = "Limpieza breve al final del día.",
        intention = "Soltar residuos emocionales y recuperar claridad.",
        materials = listOf("Incienso o sahumerio", "Ventana abierta"),
        preparation = "Ventila el espacio durante un minuto.",
        action = "Pasa el humo alrededor de tu cuerpo con intención de liberar carga.",
        closing = "Nombra lo que sueltas hoy y agradece el cierre.",
    ),
    RitualDetail(
        id = "cleansing_water_prayer",
        category = RitualCategoryType.Cleansing,
        title = "Agua y oración breve",
        subtitle = "Renovación simple antes de descansar.",
        intention = "Calmar la mente y cerrar el día con ligereza.",
        materials = listOf("Vaso transparente", "Agua"),
        action = "Bebe el agua lentamente mientras repites una frase de limpieza.",
        closing = "Finaliza con: 'Me libero y descanso en paz'.",
        optionalNote = "Úsalo también antes de meditar o dormir.",
    ),
    RitualDetail(
        id = "cleansing_sound_reset",
        category = RitualCategoryType.Cleansing,
        title = "Reseteo sonoro",
        subtitle = "Despeja el ambiente usando vibración.",
        intention = "Renovar la atmósfera emocional de forma rápida y consciente.",
        materials = listOf("Campanilla o cuenco", "Ventana abierta", "Respiración consciente"),
        preparation = "Abre una ventana y ubícate en el centro del espacio.",
        action = "Haz sonar la campanilla en cada esquina con respiración lenta.",
        closing = "Cierra con silencio de 30 segundos y nota el cambio en tu cuerpo.",
    ),
)
