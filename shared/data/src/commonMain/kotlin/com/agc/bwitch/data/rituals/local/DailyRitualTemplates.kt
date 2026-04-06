package com.agc.bwitch.data.rituals.local

import com.agc.bwitch.domain.rituals.DailyRitualStep
import com.agc.bwitch.domain.rituals.DailyRitualStepKind
import com.agc.bwitch.domain.rituals.DailyRitualTemplate
import com.agc.bwitch.domain.rituals.DailyRitualTheme
import com.agc.bwitch.domain.rituals.dailyRitualBranchKey

internal val localDailyRitualTemplates: List<DailyRitualTemplate> = listOf(
    DailyRitualTemplate(
        id = "calm_breath_anchor",
        theme = DailyRitualTheme.Calm,
        title = "Respira y vuelve a ti",
        subtitle = "Calma",
        intro = "Un ritual breve para aflojar el cuerpo y bajar el ruido mental.",
        estimatedMinutes = 3,
        steps = listOf(
            DailyRitualStep("c1", DailyRitualStepKind.Info, "Suelta los hombros y lleva tu atención a la respiración."),
            DailyRitualStep("c2", DailyRitualStepKind.Confirmation, "Haz 3 respiraciones lentas, inhalando por nariz y exhalando por boca.", ctaLabel = "Ya lo hice"),
            DailyRitualStep("c3", DailyRitualStepKind.TextInput, "Nombra en una frase cómo te gustaría sentirte hoy.", ctaLabel = "Guardar intención"),
        ),
        completionMessage = "Ya creaste un pequeño espacio de calma para tu día.",
    ),
    DailyRitualTemplate(
        id = "calm_soft_body_scan",
        theme = DailyRitualTheme.Calm,
        title = "Escaneo suave",
        subtitle = "Calma",
        intro = "Una pausa corta para escuchar tu cuerpo antes de seguir.",
        estimatedMinutes = 4,
        steps = listOf(
            DailyRitualStep("c4", DailyRitualStepKind.Info, "Cierra los ojos un instante y recorre tu cuerpo de cabeza a pies."),
            DailyRitualStep("c5", DailyRitualStepKind.SingleChoice, "¿Dónde notas más tensión ahora?", options = listOf("Cuello", "Pecho", "Mandíbula", "Estómago")),
            DailyRitualStep("c6", DailyRitualStepKind.Confirmation, "Lleva la mano a esa zona y exhala largo dos veces.", ctaLabel = "Continuar"),
        ),
        completionMessage = "Escucharte también es avanzar.",
    ),
    DailyRitualTemplate(
        id = "clarity_focus_one",
        theme = DailyRitualTheme.Clarity,
        title = "Una cosa importante",
        subtitle = "Claridad",
        intro = "Enfoca tu energía en una sola prioridad para hoy.",
        estimatedMinutes = 3,
        steps = listOf(
            DailyRitualStep("cl1", DailyRitualStepKind.TextInput, "¿Cuál es la acción más importante de tu día?", ctaLabel = "Definir"),
            DailyRitualStep("cl2", DailyRitualStepKind.SingleChoice, "¿Qué lo bloquea más?", options = listOf("Distracción", "Miedo a fallar", "Falta de tiempo", "Perfeccionismo")),
            DailyRitualStep("cl3", DailyRitualStepKind.Confirmation, "Comprométete a dedicarle 15 minutos sin interrupciones.", ctaLabel = "Me comprometo"),
        ),
        completionMessage = "Tu mente está enfocada: paso pequeño, impacto grande.",
    ),
    DailyRitualTemplate(
        id = "clarity_boundary",
        theme = DailyRitualTheme.Clarity,
        title = "Límite claro",
        subtitle = "Claridad",
        intro = "Un micro ritual para proteger tu atención.",
        estimatedMinutes = 4,
        steps = listOf(
            DailyRitualStep("cl4", DailyRitualStepKind.Info, "Recuerda: decir no también es cuidar tu energía."),
            DailyRitualStep("cl5", DailyRitualStepKind.TextInput, "Escribe un límite que necesitas marcar hoy.", ctaLabel = "Guardar límite"),
            DailyRitualStep("cl6", DailyRitualStepKind.Confirmation, "Repite en voz baja: 'Mi enfoque también merece espacio'.", ctaLabel = "Listo"),
        ),
        completionMessage = "Hoy avanzas con claridad y dirección.",
    ),
    DailyRitualTemplate(
        id = "release_small_let_go",
        theme = DailyRitualTheme.Release,
        title = "Soltar en pequeño",
        subtitle = "Soltar",
        intro = "Explora si hoy puedes liberar una carga concreta.",
        estimatedMinutes = 4,
        steps = listOf(
            DailyRitualStep("r1", DailyRitualStepKind.TextInput, "¿Qué te está pesando hoy?", ctaLabel = "Nombrarlo"),
            DailyRitualStep("r2", DailyRitualStepKind.BinaryChoice, "¿Puedes soltarlo por hoy?", options = listOf("Sí", "Aún no"), ctaLabel = "Continuar"),
        ),
        branches = mapOf(
            dailyRitualBranchKey("r2", "Sí") to listOf(
                DailyRitualStep("r2_yes_1", DailyRitualStepKind.Confirmation, "Di: 'Elijo soltar esto por hoy'.", ctaLabel = "Hecho"),
                DailyRitualStep("r2_yes_2", DailyRitualStepKind.TextInput, "Escribe una acción ligera para honrar ese cierre.", ctaLabel = "Continuar"),
                DailyRitualStep("r2_yes_3", DailyRitualStepKind.Confirmation, "Respira profundo y continúa tu día con amabilidad.", ctaLabel = "Cerrar ritual"),
            ),
            dailyRitualBranchKey("r2", "Aún no") to listOf(
                DailyRitualStep("r2_no_1", DailyRitualStepKind.Info, "Está bien. Primero nos damos contención."),
                DailyRitualStep("r2_no_2", DailyRitualStepKind.TextInput, "¿Qué apoyo mínimo necesitas para sostenerte hoy?", ctaLabel = "Continuar"),
                DailyRitualStep("r2_no_3", DailyRitualStepKind.Confirmation, "Respira profundo y continúa tu día con amabilidad.", ctaLabel = "Cerrar ritual"),
            ),
        ),
        completionMessage = "Soltar también incluye respetar tu propio ritmo.",
    ),
    DailyRitualTemplate(
        id = "release_close_cycle",
        theme = DailyRitualTheme.Release,
        title = "Cerrar ciclo breve",
        subtitle = "Soltar",
        intro = "Un momento para dejar atrás lo que ya cumplió su función.",
        estimatedMinutes = 3,
        steps = listOf(
            DailyRitualStep("r4", DailyRitualStepKind.Info, "Piensa en algo que hoy puedas dejar de cargar."),
            DailyRitualStep("r5", DailyRitualStepKind.TextInput, "Escribe una frase de cierre.", ctaLabel = "Guardar"),
            DailyRitualStep("r6", DailyRitualStepKind.Confirmation, "Exhala y termina esta frase: 'Gracias, ahora te dejo ir'.", ctaLabel = "Terminar"),
        ),
        completionMessage = "Has liberado espacio para lo nuevo.",
    ),
    DailyRitualTemplate(
        id = "energy_activate_body",
        theme = DailyRitualTheme.Energy,
        title = "Activar energía",
        subtitle = "Energía",
        intro = "Despierta tu energía con un gesto corporal simple.",
        estimatedMinutes = 3,
        steps = listOf(
            DailyRitualStep("e1", DailyRitualStepKind.SingleChoice, "¿Qué necesitas activar hoy?", options = listOf("Enfoque", "Motivación", "Valentía", "Alegría")),
            DailyRitualStep("e2", DailyRitualStepKind.Confirmation, "Haz 20 segundos de movimiento: estirarte, sacudir brazos o caminar.", ctaLabel = "Ya lo hice"),
            DailyRitualStep("e3", DailyRitualStepKind.TextInput, "Define una microacción para aprovechar este impulso.", ctaLabel = "Continuar"),
        ),
        completionMessage = "Tu energía está disponible: úsala a tu favor.",
    ),
    DailyRitualTemplate(
        id = "energy_spark_start",
        theme = DailyRitualTheme.Energy,
        title = "Encender el inicio",
        subtitle = "Energía",
        intro = "Rompe la inercia con un ritual corto y directo.",
        estimatedMinutes = 4,
        steps = listOf(
            DailyRitualStep("e4", DailyRitualStepKind.Info, "Cuenta regresiva 3-2-1 y ponte de pie."),
            DailyRitualStep("e5", DailyRitualStepKind.BinaryChoice, "¿Quieres empezar suave o intenso?", options = listOf("Suave", "Intenso"), ctaLabel = "Elegir"),
            DailyRitualStep("e6", DailyRitualStepKind.Confirmation, "Sostén el ritmo elegido durante 30 segundos.", ctaLabel = "Hecho"),
        ),
        completionMessage = "Inercia rota. Ya estás en movimiento.",
    ),
)
