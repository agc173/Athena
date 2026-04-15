package com.agc.bwitch.domain.rituals.i18n

import com.agc.bwitch.domain.localization.AppLanguage

object DailyRitualContentRepository {

    fun resolve(language: AppLanguage, key: String): String {
        return localizedCatalog[language]?.get(key)
            ?: spanishCatalog[key]
            ?: key
    }

    private val spanishCatalog: Map<String, String> = mapOf(
        "daily_ritual.cta.continue" to "Continuar",

        "daily_ritual.theme.calm" to "Calma",
        "daily_ritual.theme.clarity" to "Claridad",
        "daily_ritual.theme.release" to "Soltar",
        "daily_ritual.theme.energy" to "Energía",

        "daily_ritual.template.calm_breath_anchor.title" to "Respira y vuelve a ti",
        "daily_ritual.template.calm_breath_anchor.intro" to "Un ritual breve para aflojar el cuerpo y bajar el ruido mental.",
        "daily_ritual.template.calm_breath_anchor.completion" to "Ya creaste un pequeño espacio de calma para tu día.",

        "daily_ritual.template.calm_soft_body_scan.title" to "Escaneo suave",
        "daily_ritual.template.calm_soft_body_scan.intro" to "Una pausa corta para escuchar tu cuerpo antes de seguir.",
        "daily_ritual.template.calm_soft_body_scan.completion" to "Escucharte también es avanzar.",

        "daily_ritual.template.clarity_focus_one.title" to "Una cosa importante",
        "daily_ritual.template.clarity_focus_one.intro" to "Enfoca tu energía en una sola prioridad para hoy.",
        "daily_ritual.template.clarity_focus_one.completion" to "Tu mente está enfocada: paso pequeño, impacto grande.",

        "daily_ritual.template.clarity_boundary.title" to "Límite claro",
        "daily_ritual.template.clarity_boundary.intro" to "Un micro ritual para proteger tu atención.",
        "daily_ritual.template.clarity_boundary.completion" to "Hoy avanzas con claridad y dirección.",

        "daily_ritual.template.release_small_let_go.title" to "Soltar en pequeño",
        "daily_ritual.template.release_small_let_go.intro" to "Explora si hoy puedes liberar una carga concreta.",
        "daily_ritual.template.release_small_let_go.completion" to "Soltar también incluye respetar tu propio ritmo.",

        "daily_ritual.template.release_close_cycle.title" to "Cerrar ciclo breve",
        "daily_ritual.template.release_close_cycle.intro" to "Un momento para dejar atrás lo que ya cumplió su función.",
        "daily_ritual.template.release_close_cycle.completion" to "Has liberado espacio para lo nuevo.",

        "daily_ritual.template.energy_activate_body.title" to "Activar energía",
        "daily_ritual.template.energy_activate_body.intro" to "Despierta tu energía con un gesto corporal simple.",
        "daily_ritual.template.energy_activate_body.completion" to "Tu energía está disponible: úsala a tu favor.",

        "daily_ritual.template.energy_spark_start.title" to "Encender el inicio",
        "daily_ritual.template.energy_spark_start.intro" to "Rompe la inercia con un ritual corto y directo.",
        "daily_ritual.template.energy_spark_start.completion" to "Inercia rota. Ya estás en movimiento.",

        "daily_ritual.step.c1.text" to "Suelta los hombros y lleva tu atención a la respiración.",
        "daily_ritual.step.c2.text" to "Haz 3 respiraciones lentas, inhalando por nariz y exhalando por boca.",
        "daily_ritual.step.c2.cta" to "Ya lo hice",
        "daily_ritual.step.c3.text" to "Nombra en una frase cómo te gustaría sentirte hoy.",
        "daily_ritual.step.c3.cta" to "Guardar intención",

        "daily_ritual.step.c4.text" to "Cierra los ojos un instante y recorre tu cuerpo de cabeza a pies.",
        "daily_ritual.step.c5.text" to "¿Dónde notas más tensión ahora?",
        "daily_ritual.option.c5.neck" to "Cuello",
        "daily_ritual.option.c5.chest" to "Pecho",
        "daily_ritual.option.c5.jaw" to "Mandíbula",
        "daily_ritual.option.c5.stomach" to "Estómago",
        "daily_ritual.step.c6.text" to "Lleva la mano a esa zona y exhala largo dos veces.",

        "daily_ritual.step.cl1.text" to "¿Cuál es la acción más importante de tu día?",
        "daily_ritual.step.cl1.cta" to "Definir",
        "daily_ritual.step.cl2.text" to "¿Qué lo bloquea más?",
        "daily_ritual.option.cl2.distraction" to "Distracción",
        "daily_ritual.option.cl2.fear_of_failure" to "Miedo a fallar",
        "daily_ritual.option.cl2.lack_of_time" to "Falta de tiempo",
        "daily_ritual.option.cl2.perfectionism" to "Perfeccionismo",
        "daily_ritual.step.cl3.text" to "Comprométete a dedicarle 15 minutos sin interrupciones.",
        "daily_ritual.step.cl3.cta" to "Me comprometo",

        "daily_ritual.step.cl4.text" to "Recuerda: decir no también es cuidar tu energía.",
        "daily_ritual.step.cl5.text" to "Escribe un límite que necesitas marcar hoy.",
        "daily_ritual.step.cl5.cta" to "Guardar límite",
        "daily_ritual.step.cl6.text" to "Repite en voz baja: 'Mi enfoque también merece espacio'.",
        "daily_ritual.step.cl6.cta" to "Listo",

        "daily_ritual.step.r1.text" to "¿Qué te está pesando hoy?",
        "daily_ritual.step.r1.cta" to "Nombrarlo",
        "daily_ritual.step.r2.text" to "¿Puedes soltarlo por hoy?",
        "daily_ritual.option.r2.yes" to "Sí",
        "daily_ritual.option.r2.not_yet" to "Aún no",

        "daily_ritual.step.r2_yes_1.text" to "Di: 'Elijo soltar esto por hoy'.",
        "daily_ritual.step.r2_yes_1.cta" to "Hecho",
        "daily_ritual.step.r2_yes_2.text" to "Escribe una acción ligera para honrar ese cierre.",
        "daily_ritual.step.r2_yes_3.text" to "Respira profundo y continúa tu día con amabilidad.",
        "daily_ritual.step.r2_yes_3.cta" to "Cerrar ritual",

        "daily_ritual.step.r2_no_1.text" to "Está bien. Primero nos damos contención.",
        "daily_ritual.step.r2_no_2.text" to "¿Qué apoyo mínimo necesitas para sostenerte hoy?",
        "daily_ritual.step.r2_no_3.text" to "Respira profundo y continúa tu día con amabilidad.",
        "daily_ritual.step.r2_no_3.cta" to "Cerrar ritual",

        "daily_ritual.step.r4.text" to "Piensa en algo que hoy puedas dejar de cargar.",
        "daily_ritual.step.r5.text" to "Escribe una frase de cierre.",
        "daily_ritual.step.r5.cta" to "Guardar",
        "daily_ritual.step.r6.text" to "Exhala y termina esta frase: 'Gracias, ahora te dejo ir'.",
        "daily_ritual.step.r6.cta" to "Terminar",

        "daily_ritual.step.e1.text" to "¿Qué necesitas activar hoy?",
        "daily_ritual.option.e1.focus" to "Enfoque",
        "daily_ritual.option.e1.motivation" to "Motivación",
        "daily_ritual.option.e1.courage" to "Valentía",
        "daily_ritual.option.e1.joy" to "Alegría",
        "daily_ritual.step.e2.text" to "Haz 20 segundos de movimiento: estirarte, sacudir brazos o caminar.",
        "daily_ritual.step.e2.cta" to "Ya lo hice",
        "daily_ritual.step.e3.text" to "Define una microacción para aprovechar este impulso.",

        "daily_ritual.step.e4.text" to "Cuenta regresiva 3-2-1 y ponte de pie.",
        "daily_ritual.step.e5.text" to "¿Quieres empezar suave o intenso?",
        "daily_ritual.option.e5.soft" to "Suave",
        "daily_ritual.option.e5.intense" to "Intenso",
        "daily_ritual.step.e5.cta" to "Elegir",
        "daily_ritual.step.e6.text" to "Sostén el ritmo elegido durante 30 segundos.",
        "daily_ritual.step.e6.cta" to "Hecho",
    )

    private val englishCatalog: Map<String, String> = mapOf(
        "daily_ritual.cta.continue" to "Continue",

        "daily_ritual.theme.calm" to "Calm",
        "daily_ritual.theme.clarity" to "Clarity",
        "daily_ritual.theme.release" to "Release",
        "daily_ritual.theme.energy" to "Energy",

        "daily_ritual.template.calm_breath_anchor.title" to "Breathe and return to yourself",
        "daily_ritual.template.calm_breath_anchor.intro" to "A short ritual to loosen the body and quiet mental noise.",
        "daily_ritual.template.calm_breath_anchor.completion" to "You already created a small space of calm for your day.",

        "daily_ritual.template.calm_soft_body_scan.title" to "Gentle body scan",
        "daily_ritual.template.calm_soft_body_scan.intro" to "A brief pause to listen to your body before moving on.",
        "daily_ritual.template.calm_soft_body_scan.completion" to "Listening to yourself is also progress.",

        "daily_ritual.template.clarity_focus_one.title" to "One important thing",
        "daily_ritual.template.clarity_focus_one.intro" to "Focus your energy on one priority today.",
        "daily_ritual.template.clarity_focus_one.completion" to "Your mind is focused: small step, big impact.",

        "daily_ritual.template.clarity_boundary.title" to "Clear boundary",
        "daily_ritual.template.clarity_boundary.intro" to "A micro ritual to protect your attention.",
        "daily_ritual.template.clarity_boundary.completion" to "Today you move forward with clarity and direction.",

        "daily_ritual.template.release_small_let_go.title" to "Let go in small ways",
        "daily_ritual.template.release_small_let_go.intro" to "Explore whether you can release one concrete burden today.",
        "daily_ritual.template.release_small_let_go.completion" to "Letting go also means honoring your own rhythm.",

        "daily_ritual.template.release_close_cycle.title" to "Close a short cycle",
        "daily_ritual.template.release_close_cycle.intro" to "A moment to leave behind what has already served its purpose.",
        "daily_ritual.template.release_close_cycle.completion" to "You created space for what comes next.",

        "daily_ritual.template.energy_activate_body.title" to "Activate energy",
        "daily_ritual.template.energy_activate_body.intro" to "Wake up your energy with a simple body gesture.",
        "daily_ritual.template.energy_activate_body.completion" to "Your energy is available: use it in your favor.",

        "daily_ritual.template.energy_spark_start.title" to "Ignite your start",
        "daily_ritual.template.energy_spark_start.intro" to "Break inertia with a short and direct ritual.",
        "daily_ritual.template.energy_spark_start.completion" to "Inertia broken. You are already in motion.",

        "daily_ritual.step.c1.text" to "Drop your shoulders and bring your attention to your breath.",
        "daily_ritual.step.c2.text" to "Take 3 slow breaths, inhale through your nose and exhale through your mouth.",
        "daily_ritual.step.c2.cta" to "Done",
        "daily_ritual.step.c3.text" to "In one sentence, name how you want to feel today.",
        "daily_ritual.step.c3.cta" to "Save intention",

        "daily_ritual.step.c4.text" to "Close your eyes for a moment and scan your body from head to toe.",
        "daily_ritual.step.c5.text" to "Where do you feel the most tension right now?",
        "daily_ritual.option.c5.neck" to "Neck",
        "daily_ritual.option.c5.chest" to "Chest",
        "daily_ritual.option.c5.jaw" to "Jaw",
        "daily_ritual.option.c5.stomach" to "Stomach",
        "daily_ritual.step.c6.text" to "Place your hand on that area and take two long exhales.",

        "daily_ritual.step.cl1.text" to "What is the most important action of your day?",
        "daily_ritual.step.cl1.cta" to "Define",
        "daily_ritual.step.cl2.text" to "What blocks it the most?",
        "daily_ritual.option.cl2.distraction" to "Distraction",
        "daily_ritual.option.cl2.fear_of_failure" to "Fear of failure",
        "daily_ritual.option.cl2.lack_of_time" to "Lack of time",
        "daily_ritual.option.cl2.perfectionism" to "Perfectionism",
        "daily_ritual.step.cl3.text" to "Commit to giving it 15 uninterrupted minutes.",
        "daily_ritual.step.cl3.cta" to "I commit",

        "daily_ritual.step.cl4.text" to "Remember: saying no is also protecting your energy.",
        "daily_ritual.step.cl5.text" to "Write one boundary you need to set today.",
        "daily_ritual.step.cl5.cta" to "Save boundary",
        "daily_ritual.step.cl6.text" to "Repeat softly: 'My focus also deserves space'.",
        "daily_ritual.step.cl6.cta" to "Ready",

        "daily_ritual.step.r1.text" to "What feels heavy for you today?",
        "daily_ritual.step.r1.cta" to "Name it",
        "daily_ritual.step.r2.text" to "Can you let it go for today?",
        "daily_ritual.option.r2.yes" to "Yes",
        "daily_ritual.option.r2.not_yet" to "Not yet",

        "daily_ritual.step.r2_yes_1.text" to "Say: 'I choose to release this for today'.",
        "daily_ritual.step.r2_yes_1.cta" to "Done",
        "daily_ritual.step.r2_yes_2.text" to "Write one light action to honor that closure.",
        "daily_ritual.step.r2_yes_3.text" to "Take a deep breath and continue your day with kindness.",
        "daily_ritual.step.r2_yes_3.cta" to "Close ritual",

        "daily_ritual.step.r2_no_1.text" to "That's okay. First, we hold ourselves with care.",
        "daily_ritual.step.r2_no_2.text" to "What minimum support do you need to sustain yourself today?",
        "daily_ritual.step.r2_no_3.text" to "Take a deep breath and continue your day with kindness.",
        "daily_ritual.step.r2_no_3.cta" to "Close ritual",

        "daily_ritual.step.r4.text" to "Think of something you can stop carrying today.",
        "daily_ritual.step.r5.text" to "Write one closing sentence.",
        "daily_ritual.step.r5.cta" to "Save",
        "daily_ritual.step.r6.text" to "Exhale and finish this sentence: 'Thank you, now I let you go'.",
        "daily_ritual.step.r6.cta" to "Finish",

        "daily_ritual.step.e1.text" to "What do you need to activate today?",
        "daily_ritual.option.e1.focus" to "Focus",
        "daily_ritual.option.e1.motivation" to "Motivation",
        "daily_ritual.option.e1.courage" to "Courage",
        "daily_ritual.option.e1.joy" to "Joy",
        "daily_ritual.step.e2.text" to "Do 20 seconds of movement: stretch, shake your arms, or walk.",
        "daily_ritual.step.e2.cta" to "Done",
        "daily_ritual.step.e3.text" to "Define one micro-action to use this momentum.",

        "daily_ritual.step.e4.text" to "Count down 3-2-1 and stand up.",
        "daily_ritual.step.e5.text" to "Do you want to start soft or intense?",
        "daily_ritual.option.e5.soft" to "Soft",
        "daily_ritual.option.e5.intense" to "Intense",
        "daily_ritual.step.e5.cta" to "Choose",
        "daily_ritual.step.e6.text" to "Keep your chosen rhythm for 30 seconds.",
        "daily_ritual.step.e6.cta" to "Done",
    )

    private val localizedCatalog: Map<AppLanguage, Map<String, String>> = mapOf(
        AppLanguage.Spanish to spanishCatalog,
        AppLanguage.English to englishCatalog,
    )
}
