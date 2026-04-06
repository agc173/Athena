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
        title = "Ritual para atraer el amor",
        subtitle = "Abre tu energía a vínculos conscientes y dulces.",
        intention = "Abrir espacio interno para atraer un amor recíproco y consciente.",
        materials = listOf("Un espejo pequeño", "Vela blanca", "Cuaderno"),
        preparation = "Busca un lugar en calma y enciende la vela.",
        action = "Mírate al espejo y di tres cualidades que honras en ti.",
        closing = "Escribe una frase de amor propio y agradece en voz baja.",
        optionalNote = "Puedes repetirlo antes de una cita o conversación importante.",
    ),
    RitualDetail(
        id = "love_rose_bath",
        category = RitualCategoryType.Love,
        title = "Ritual para sanar un corazón roto",
        subtitle = "Suelta lo que duele y recupera tu paz emocional.",
        intention = "Sanar el apego y soltar vínculos que ya cumplieron su ciclo.",
        materials = listOf("Agua tibia", "Pétalos o esencia de rosa", "Toalla"),
        action = "Sumerge manos o pies y respira lento durante dos minutos.",
        closing = "Repite: 'Estoy a salvo para amar y recibir amor'.",
    ),
    RitualDetail(
        id = "love_letter_heart",
        category = RitualCategoryType.Love,
        title = "Ritual para fortalecer tu relación",
        subtitle = "Nutre el vínculo con presencia, escucha y ternura.",
        intention = "Fortalecer una relación desde la empatía y la comunicación consciente.",
        materials = listOf("Hoja", "Bolígrafo", "Sobre"),
        preparation = "Respira profundo durante un minuto antes de escribir.",
        action = "Escribe una carta corta comenzando con 'Querido corazón, hoy necesito...'.",
        closing = "Guarda la carta y agradece tu valentía emocional.",
    ),

    // Prosperidad (3)
    RitualDetail(
        id = "prosperity_coin_focus",
        category = RitualCategoryType.Prosperity,
        title = "Ritual para atraer dinero",
        subtitle = "Enfoca tu energía en decisiones que generan abundancia.",
        intention = "Activar hábitos concretos para atraer más dinero a tu vida.",
        materials = listOf("Una moneda", "Papel", "Bolígrafo"),
        preparation = "Define una meta simple para esta semana.",
        action = "Escribe una acción de hoy y guarda la moneda junto al papel.",
        closing = "Empieza esa acción dentro de los próximos cinco minutos.",
    ),
    RitualDetail(
        id = "prosperity_desk_reset",
        category = RitualCategoryType.Prosperity,
        title = "Ritual para encontrar trabajo",
        subtitle = "Activa claridad, constancia y apertura a nuevas oportunidades.",
        intention = "Potenciar tu búsqueda laboral con claridad y acción sostenida.",
        materials = listOf("Paño", "Recipiente pequeño", "Incienso opcional"),
        action = "Limpia tu mesa y deja a la vista solo lo esencial para tu objetivo.",
        closing = "Define la primera tarea y ejecútala sin distracciones.",
    ),
    RitualDetail(
        id = "prosperity_wallet_blessing",
        category = RitualCategoryType.Prosperity,
        title = "Ritual para impulsar tu negocio",
        subtitle = "Ordena tu energía para crecer con dirección y propósito.",
        intention = "Mejorar resultados de tu negocio enfocando recursos y prioridades.",
        materials = listOf("Cartera", "Billete o moneda", "Canela en polvo"),
        preparation = "Vacía tu cartera y retira papeles que ya no uses.",
        action = "Coloca una pizca de canela y repite una afirmación de abundancia consciente.",
        closing = "Vuelve a guardar solo lo necesario y ordenado.",
    ),

    // Protección (3)
    RitualDetail(
        id = "protection_salt_circle",
        category = RitualCategoryType.Protection,
        title = "Ritual para proteger tu energía personal",
        subtitle = "Crea un resguardo interno antes de compartirte con otros.",
        intention = "Proteger tu energía personal frente a entornos exigentes.",
        materials = listOf("Sal gruesa", "Vaso con agua"),
        preparation = "Marca un pequeño perímetro simbólico con sal.",
        action = "Respira un minuto y repite tu afirmación de protección.",
        closing = "Recoge la sal con gratitud y desecha fuera de casa.",
    ),
    RitualDetail(
        id = "protection_hand_seal",
        category = RitualCategoryType.Protection,
        title = "Ritual para cortar influencias negativas",
        subtitle = "Desvincula tu campo de cargas ajenas con firmeza serena.",
        intention = "Cortar la influencia de energías densas o vínculos invasivos.",
        materials = listOf("Tus manos", "Un lugar tranquilo"),
        action = "Frota tus manos, colócalas en pecho y abdomen y respira profundo.",
        closing = "Visualiza una luz envolvente y afirma: 'Yo decido qué permito'.",
    ),
    RitualDetail(
        id = "protection_doorway_intent",
        category = RitualCategoryType.Protection,
        title = "Ritual para reforzar límites",
        subtitle = "Marca con claridad qué entra y qué no en tu espacio.",
        intention = "Reforzar límites sanos en tu hogar y en tus relaciones.",
        materials = listOf("Agua con sal", "Paño limpio", "Aceite esencial opcional"),
        preparation = "Humedece el paño con agua y una pizca de sal.",
        action = "Limpia marco y picaporte con intención de protección y calma.",
        closing = "Di: 'Aquí habitan paz, respeto y claridad'.",
    ),

    // Limpieza (3)
    RitualDetail(
        id = "cleansing_smoke_release",
        category = RitualCategoryType.Cleansing,
        title = "Ritual para limpiar la energía del hogar",
        subtitle = "Renueva tu espacio y devuelve armonía al ambiente.",
        intention = "Limpiar la energía del hogar para restaurar calma y orden.",
        materials = listOf("Incienso o sahumerio", "Ventana abierta"),
        preparation = "Ventila el espacio durante un minuto.",
        action = "Pasa el humo alrededor de tu cuerpo con intención de liberar carga.",
        closing = "Nombra lo que sueltas hoy y agradece el cierre.",
    ),
    RitualDetail(
        id = "cleansing_water_prayer",
        category = RitualCategoryType.Cleansing,
        title = "Ritual para soltar emociones",
        subtitle = "Deja ir la carga del día y respira con ligereza.",
        intention = "Soltar emociones acumuladas y volver al equilibrio interno.",
        materials = listOf("Vaso transparente", "Agua"),
        action = "Bebe el agua lentamente mientras repites una frase de limpieza.",
        closing = "Finaliza con: 'Me libero y descanso en paz'.",
        optionalNote = "Úsalo también antes de meditar o dormir.",
    ),
    RitualDetail(
        id = "cleansing_sound_reset",
        category = RitualCategoryType.Cleansing,
        title = "Ritual para un reinicio personal",
        subtitle = "Corta la inercia y vuelve a ti con energía nueva.",
        intention = "Marcar un reinicio personal para avanzar con mayor claridad.",
        materials = listOf("Campanilla o cuenco", "Ventana abierta", "Respiración consciente"),
        preparation = "Abre una ventana y ubícate en el centro del espacio.",
        action = "Haz sonar la campanilla en cada esquina con respiración lenta.",
        closing = "Cierra con silencio de 30 segundos y nota el cambio en tu cuerpo.",
    ),
)
