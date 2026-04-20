package com.agc.bwitch.domain.rituals.i18n

import com.agc.bwitch.domain.localization.AppLanguage

object HabitsContentRepository {

    private const val KEY_PREFIX = "habits.intention."

    fun resolve(language: AppLanguage, key: String): String {
        return localizedCatalog[language]?.get(key)
            ?: spanishCatalog[key]
            ?: key
    }

    fun resolveCompat(language: AppLanguage, value: String): String {
        return if (value.isHabitsContentKey()) resolve(language = language, key = value) else value
    }

    fun String.isHabitsContentKey(): Boolean = startsWith(KEY_PREFIX)

    private val spanishCatalog: Map<String, String> = mapOf(
        "habits.intention.conexion.title" to "Conexión",
        "habits.intention.conexion.action" to "Escribe a alguien con quien hace tiempo no hablas",
        "habits.intention.gratitud.title" to "Gratitud",
        "habits.intention.gratitud.action" to "Escribe tres cosas por las que te sientes agradecida hoy",
        "habits.intention.calma.title" to "Calma",
        "habits.intention.calma.action" to "Respira durante 3 minutos sin distracciones",
        "habits.intention.presencia.title" to "Presencia",
        "habits.intention.presencia.action" to "Bebe un vaso de agua con atención plena",
        "habits.intention.limpieza.title" to "Limpieza",
        "habits.intention.limpieza.action" to "Ordena un pequeño rincón de tu casa",
        "habits.intention.cuidado.title" to "Cuidado",
        "habits.intention.cuidado.action" to "Dedica 10 minutos a algo que te haga bien",
        "habits.intention.silencio.title" to "Silencio",
        "habits.intention.silencio.action" to "Regálate unos minutos sin móvil ni ruido",
        "habits.intention.orden.title" to "Orden",
        "habits.intention.orden.action" to "Guarda o limpia un objeto que uses a diario",
        "habits.intention.movimiento.title" to "Movimiento",
        "habits.intention.movimiento.action" to "Da un paseo breve sin mirar el móvil",
        "habits.intention.introspeccion.title" to "Introspección",
        "habits.intention.introspeccion.action" to "Escribe una frase sobre cómo quieres sentirte hoy",
        "habits.intention.descanso.title" to "Descanso",
        "habits.intention.descanso.action" to "Baja el ritmo durante cinco minutos y respira",
        "habits.intention.apertura.title" to "Apertura",
        "habits.intention.apertura.action" to "Abre una ventana y renueva el aire de tu espacio",
    )

    private val englishCatalog: Map<String, String> = mapOf(
        "habits.intention.conexion.title" to "Connection",
        "habits.intention.conexion.action" to "Message someone you have not spoken to in a while",
        "habits.intention.gratitud.title" to "Gratitude",
        "habits.intention.gratitud.action" to "Write three things you feel grateful for today",
        "habits.intention.calma.title" to "Calm",
        "habits.intention.calma.action" to "Breathe for 3 minutes without distractions",
        "habits.intention.presencia.title" to "Presence",
        "habits.intention.presencia.action" to "Drink a glass of water with full attention",
        "habits.intention.limpieza.title" to "Cleansing",
        "habits.intention.limpieza.action" to "Tidy up a small corner of your home",
        "habits.intention.cuidado.title" to "Self-care",
        "habits.intention.cuidado.action" to "Spend 10 minutes on something that makes you feel good",
        "habits.intention.silencio.title" to "Silence",
        "habits.intention.silencio.action" to "Give yourself a few minutes without phone or noise",
        "habits.intention.orden.title" to "Order",
        "habits.intention.orden.action" to "Put away or clean one object you use every day",
        "habits.intention.movimiento.title" to "Movement",
        "habits.intention.movimiento.action" to "Take a short walk without looking at your phone",
        "habits.intention.introspeccion.title" to "Introspection",
        "habits.intention.introspeccion.action" to "Write one sentence about how you want to feel today",
        "habits.intention.descanso.title" to "Rest",
        "habits.intention.descanso.action" to "Slow down for five minutes and breathe",
        "habits.intention.apertura.title" to "Openness",
        "habits.intention.apertura.action" to "Open a window and refresh the air in your space",
    )

    private val portugueseCatalog: Map<String, String> = mapOf(
        "habits.intention.conexion.title" to "Conexão",
        "habits.intention.conexion.action" to "Envie uma mensagem para alguém com quem você não fala há algum tempo",
        "habits.intention.gratitud.title" to "Gratidão",
        "habits.intention.gratitud.action" to "Escreva três coisas pelas quais você se sente grata hoje",
        "habits.intention.calma.title" to "Calma",
        "habits.intention.calma.action" to "Respire por 3 minutos sem distrações",
        "habits.intention.presencia.title" to "Presença",
        "habits.intention.presencia.action" to "Beba um copo de água com atenção plena",
        "habits.intention.limpieza.title" to "Limpeza",
        "habits.intention.limpieza.action" to "Organize um pequeno canto da sua casa",
        "habits.intention.cuidado.title" to "Autocuidado",
        "habits.intention.cuidado.action" to "Dedique 10 minutos a algo que te faça bem",
        "habits.intention.silencio.title" to "Silêncio",
        "habits.intention.silencio.action" to "Dê a si mesma alguns minutos sem celular nem barulho",
        "habits.intention.orden.title" to "Ordem",
        "habits.intention.orden.action" to "Guarde ou limpe um objeto que você usa todos os dias",
        "habits.intention.movimiento.title" to "Movimento",
        "habits.intention.movimiento.action" to "Faça uma caminhada curta sem olhar o celular",
        "habits.intention.introspeccion.title" to "Introspecção",
        "habits.intention.introspeccion.action" to "Escreva uma frase sobre como você quer se sentir hoje",
        "habits.intention.descanso.title" to "Descanso",
        "habits.intention.descanso.action" to "Diminua o ritmo por cinco minutos e respire",
        "habits.intention.apertura.title" to "Abertura",
        "habits.intention.apertura.action" to "Abra uma janela e renove o ar do seu espaço",
    )

    private val russianCatalog: Map<String, String> = mapOf(
        "habits.intention.conexion.title" to "Связь",
        "habits.intention.conexion.action" to "Напиши тому, с кем давно не общалась",
        "habits.intention.gratitud.title" to "Благодарность",
        "habits.intention.gratitud.action" to "Запиши три вещи, за которые ты благодарна сегодня",
        "habits.intention.calma.title" to "Спокойствие",
        "habits.intention.calma.action" to "Дыши 3 минуты без отвлечений",
        "habits.intention.presencia.title" to "Присутствие",
        "habits.intention.presencia.action" to "Выпей стакан воды осознанно",
        "habits.intention.limpieza.title" to "Очищение",
        "habits.intention.limpieza.action" to "Приведи в порядок небольшой уголок дома",
        "habits.intention.cuidado.title" to "Забота о себе",
        "habits.intention.cuidado.action" to "Посвяти 10 минут тому, что поддерживает тебя",
        "habits.intention.silencio.title" to "Тишина",
        "habits.intention.silencio.action" to "Подари себе несколько минут без телефона и шума",
        "habits.intention.orden.title" to "Порядок",
        "habits.intention.orden.action" to "Убери или почисти предмет, которым пользуешься каждый день",
        "habits.intention.movimiento.title" to "Движение",
        "habits.intention.movimiento.action" to "Сделай короткую прогулку, не глядя в телефон",
        "habits.intention.introspeccion.title" to "Саморефлексия",
        "habits.intention.introspeccion.action" to "Напиши одну фразу о том, как ты хочешь себя чувствовать сегодня",
        "habits.intention.descanso.title" to "Отдых",
        "habits.intention.descanso.action" to "Сбавь темп на пять минут и подыши",
        "habits.intention.apertura.title" to "Открытость",
        "habits.intention.apertura.action" to "Открой окно и обнови воздух в своем пространстве",
    )

    private val frenchCatalog: Map<String, String> = mapOf(
        "habits.intention.conexion.title" to "Connexion",
        "habits.intention.conexion.action" to "Écris à quelqu'un avec qui tu n'as pas parlé depuis longtemps",
        "habits.intention.gratitud.title" to "Gratitude",
        "habits.intention.gratitud.action" to "Écris trois choses pour lesquelles tu te sens reconnaissante aujourd'hui",
        "habits.intention.calma.title" to "Calme",
        "habits.intention.calma.action" to "Respire pendant 3 minutes sans distractions",
        "habits.intention.presencia.title" to "Présence",
        "habits.intention.presencia.action" to "Bois un verre d'eau en pleine conscience",
        "habits.intention.limpieza.title" to "Nettoyage",
        "habits.intention.limpieza.action" to "Range un petit coin de ta maison",
        "habits.intention.cuidado.title" to "Soin de soi",
        "habits.intention.cuidado.action" to "Consacre 10 minutes à quelque chose qui te fait du bien",
        "habits.intention.silencio.title" to "Silence",
        "habits.intention.silencio.action" to "Offre-toi quelques minutes sans téléphone ni bruit",
        "habits.intention.orden.title" to "Ordre",
        "habits.intention.orden.action" to "Range ou nettoie un objet que tu utilises chaque jour",
        "habits.intention.movimiento.title" to "Mouvement",
        "habits.intention.movimiento.action" to "Fais une courte marche sans regarder ton téléphone",
        "habits.intention.introspeccion.title" to "Introspection",
        "habits.intention.introspeccion.action" to "Écris une phrase sur la manière dont tu veux te sentir aujourd'hui",
        "habits.intention.descanso.title" to "Repos",
        "habits.intention.descanso.action" to "Ralentis pendant cinq minutes et respire",
        "habits.intention.apertura.title" to "Ouverture",
        "habits.intention.apertura.action" to "Ouvre une fenêtre et renouvelle l'air de ton espace",
    )

    private val italianCatalog: Map<String, String> = mapOf(
        "habits.intention.conexion.title" to "Connessione",
        "habits.intention.conexion.action" to "Scrivi a qualcuno con cui non parli da tempo",
        "habits.intention.gratitud.title" to "Gratitudine",
        "habits.intention.gratitud.action" to "Scrivi tre cose per cui ti senti grata oggi",
        "habits.intention.calma.title" to "Calma",
        "habits.intention.calma.action" to "Respira per 3 minuti senza distrazioni",
        "habits.intention.presencia.title" to "Presenza",
        "habits.intention.presencia.action" to "Bevi un bicchiere d'acqua con piena attenzione",
        "habits.intention.limpieza.title" to "Pulizia",
        "habits.intention.limpieza.action" to "Riordina un piccolo angolo di casa",
        "habits.intention.cuidado.title" to "Cura di sé",
        "habits.intention.cuidado.action" to "Dedica 10 minuti a qualcosa che ti faccia stare bene",
        "habits.intention.silencio.title" to "Silenzio",
        "habits.intention.silencio.action" to "Concediti qualche minuto senza telefono né rumore",
        "habits.intention.orden.title" to "Ordine",
        "habits.intention.orden.action" to "Metti a posto o pulisci un oggetto che usi ogni giorno",
        "habits.intention.movimiento.title" to "Movimento",
        "habits.intention.movimiento.action" to "Fai una breve passeggiata senza guardare il telefono",
        "habits.intention.introspeccion.title" to "Introspezione",
        "habits.intention.introspeccion.action" to "Scrivi una frase su come vuoi sentirti oggi",
        "habits.intention.descanso.title" to "Riposo",
        "habits.intention.descanso.action" to "Rallenta per cinque minuti e respira",
        "habits.intention.apertura.title" to "Apertura",
        "habits.intention.apertura.action" to "Apri una finestra e rinnova l'aria del tuo spazio",
    )

    private val germanCatalog: Map<String, String> = mapOf(
        "habits.intention.conexion.title" to "Verbindung",
        "habits.intention.conexion.action" to "Schreibe jemandem, mit dem du lange nicht gesprochen hast",
        "habits.intention.gratitud.title" to "Dankbarkeit",
        "habits.intention.gratitud.action" to "Schreibe drei Dinge auf, für die du heute dankbar bist",
        "habits.intention.calma.title" to "Ruhe",
        "habits.intention.calma.action" to "Atme 3 Minuten lang ohne Ablenkungen",
        "habits.intention.presencia.title" to "Präsenz",
        "habits.intention.presencia.action" to "Trinke ein Glas Wasser in voller Achtsamkeit",
        "habits.intention.limpieza.title" to "Reinigung",
        "habits.intention.limpieza.action" to "Räume eine kleine Ecke deines Zuhauses auf",
        "habits.intention.cuidado.title" to "Selbstfürsorge",
        "habits.intention.cuidado.action" to "Nimm dir 10 Minuten für etwas, das dir gut tut",
        "habits.intention.silencio.title" to "Stille",
        "habits.intention.silencio.action" to "Schenke dir ein paar Minuten ohne Handy und ohne Lärm",
        "habits.intention.orden.title" to "Ordnung",
        "habits.intention.orden.action" to "Räume einen Gegenstand weg oder reinige ihn, den du täglich benutzt",
        "habits.intention.movimiento.title" to "Bewegung",
        "habits.intention.movimiento.action" to "Mach einen kurzen Spaziergang, ohne auf dein Handy zu schauen",
        "habits.intention.introspeccion.title" to "Selbstreflexion",
        "habits.intention.introspeccion.action" to "Schreibe einen Satz darüber, wie du dich heute fühlen möchtest",
        "habits.intention.descanso.title" to "Erholung",
        "habits.intention.descanso.action" to "Drossle fünf Minuten lang dein Tempo und atme",
        "habits.intention.apertura.title" to "Offenheit",
        "habits.intention.apertura.action" to "Öffne ein Fenster und erneuere die Luft in deinem Raum",
    )

    private val localizedCatalog: Map<AppLanguage, Map<String, String>> = mapOf(
        AppLanguage.Spanish to spanishCatalog,
        AppLanguage.English to englishCatalog,
        AppLanguage.Portuguese to portugueseCatalog,
        AppLanguage.Russian to russianCatalog,
        AppLanguage.French to frenchCatalog,
        AppLanguage.Italian to italianCatalog,
        AppLanguage.German to germanCatalog,
    )
}
