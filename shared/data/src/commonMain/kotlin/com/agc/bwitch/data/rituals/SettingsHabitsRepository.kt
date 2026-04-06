package com.agc.bwitch.data.rituals

import com.agc.bwitch.data.storage.SettingsFactory
import com.agc.bwitch.domain.rituals.DailyHabitsState
import com.agc.bwitch.domain.rituals.HabitIntention
import com.agc.bwitch.domain.rituals.HabitsProgress
import com.agc.bwitch.domain.rituals.HabitsRepository
import com.russhwolf.settings.Settings
import kotlin.random.Random
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class SettingsHabitsRepository(
    settingsFactory: SettingsFactory,
) : HabitsRepository {

    private val settings: Settings = settingsFactory.create("bwitch_habits")

    override fun getTodayIntentions(): List<HabitIntention> {
        val state = ensureTodayState()
        return state.selectedIntentionIds.mapNotNull { id -> intentionsPool.find { it.id == id } }
    }

    override fun getTodayState(): DailyHabitsState = ensureTodayState()

    override fun getProgress(): HabitsProgress = HabitsProgress(
        currentCyclePoints = settings.getInt(PROGRESS_POINTS_KEY, 0).coerceAtLeast(0),
        cycleTarget = CYCLE_TARGET,
        completedCycles = settings.getInt(COMPLETED_CYCLES_KEY, 0).coerceAtLeast(0),
    )

    override fun markCompleted(intentionId: String) {
        val state = ensureTodayState()
        if (intentionId !in state.selectedIntentionIds) return
        if (intentionId in state.completedIntentionIds) return

        val updatedCompleted = (state.completedIntentionIds + intentionId).take(MAX_DAILY_INTENTIONS).toSet()
        saveTodayState(state.copy(completedIntentionIds = updatedCompleted))

        updateProgress(delta = 1)
    }

    override fun unmarkCompleted(intentionId: String) {
        val state = ensureTodayState()
        if (intentionId !in state.completedIntentionIds) return

        val updatedCompleted = state.completedIntentionIds - intentionId
        saveTodayState(state.copy(completedIntentionIds = updatedCompleted))

        updateProgress(delta = -1)
    }

    private fun ensureTodayState(): DailyHabitsState {
        val today = todayIsoDate()
        val persistedDate = settings.getStringOrNull(TODAY_DATE_KEY)

        if (persistedDate == today) {
            return DailyHabitsState(
                date = today,
                selectedIntentionIds = readIds(TODAY_SELECTED_IDS_KEY),
                completedIntentionIds = readIds(TODAY_COMPLETED_IDS_KEY).toSet(),
            )
        }

        val previousIds = readIds(TODAY_SELECTED_IDS_KEY)
        val newIds = generateDailyIds(today = today, previousIds = previousIds)

        val newState = DailyHabitsState(
            date = today,
            selectedIntentionIds = newIds,
            completedIntentionIds = emptySet(),
        )
        saveTodayState(newState)
        return newState
    }

    private fun generateDailyIds(today: String, previousIds: List<String>): List<String> {
        if (intentionsPool.size <= MAX_DAILY_INTENTIONS) {
            return intentionsPool.map { it.id }
        }

        val todaySeed = today.hashCode().toLong()
        var attempts = 0
        var selected = shuffledIds(todaySeed)

        while (
            attempts < 4 &&
            previousIds.size == MAX_DAILY_INTENTIONS &&
            selected == previousIds
        ) {
            attempts += 1
            selected = shuffledIds(todaySeed + attempts)
        }

        return selected
    }

    private fun shuffledIds(seed: Long): List<String> {
        val random = Random(seed)
        return intentionsPool.shuffled(random).take(MAX_DAILY_INTENTIONS).map { it.id }
    }

    private fun updateProgress(delta: Int) {
        val currentPoints = settings.getInt(PROGRESS_POINTS_KEY, 0).coerceAtLeast(0)
        val currentCycles = settings.getInt(COMPLETED_CYCLES_KEY, 0).coerceAtLeast(0)

        if (delta > 0) {
            val total = currentPoints + delta
            val earnedCycles = total / CYCLE_TARGET
            val nextPoints = total % CYCLE_TARGET
            settings.putInt(PROGRESS_POINTS_KEY, nextPoints)
            settings.putInt(COMPLETED_CYCLES_KEY, currentCycles + earnedCycles)
            return
        }

        if (delta < 0) {
            if (currentPoints > 0) {
                settings.putInt(PROGRESS_POINTS_KEY, currentPoints - 1)
                return
            }
            if (currentCycles > 0) {
                settings.putInt(COMPLETED_CYCLES_KEY, currentCycles - 1)
                settings.putInt(PROGRESS_POINTS_KEY, CYCLE_TARGET - 1)
            }
        }
    }

    private fun saveTodayState(state: DailyHabitsState) {
        settings.putString(TODAY_DATE_KEY, state.date)
        settings.putString(TODAY_SELECTED_IDS_KEY, state.selectedIntentionIds.joinToString(SEPARATOR))
        settings.putString(TODAY_COMPLETED_IDS_KEY, state.completedIntentionIds.joinToString(SEPARATOR))
    }

    private fun readIds(key: String): List<String> =
        settings.getStringOrNull(key)
            ?.split(SEPARATOR)
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            .orEmpty()

    private fun todayIsoDate(): String =
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()

    private companion object {
        private const val CYCLE_TARGET = 60
        private const val MAX_DAILY_INTENTIONS = 3
        private const val SEPARATOR = ","

        private const val TODAY_DATE_KEY = "habits_today_date"
        private const val TODAY_SELECTED_IDS_KEY = "habits_today_selected_ids"
        private const val TODAY_COMPLETED_IDS_KEY = "habits_today_completed_ids"
        private const val PROGRESS_POINTS_KEY = "habits_progress_points"
        private const val COMPLETED_CYCLES_KEY = "habits_completed_cycles"

        private val intentionsPool = listOf(
            HabitIntention(
                id = "conexion",
                title = "Conexión",
                actionText = "Escribe a alguien con quien hace tiempo no hablas",
            ),
            HabitIntention(
                id = "gratitud",
                title = "Gratitud",
                actionText = "Escribe tres cosas por las que te sientes agradecido hoy",
            ),
            HabitIntention(
                id = "calma",
                title = "Calma",
                actionText = "Respira durante 3 minutos sin distracciones",
            ),
            HabitIntention(
                id = "presencia",
                title = "Presencia",
                actionText = "Bebe un vaso de agua con atención plena",
            ),
            HabitIntention(
                id = "limpieza",
                title = "Limpieza",
                actionText = "Ordena un pequeño rincón de tu casa",
            ),
            HabitIntention(
                id = "cuidado",
                title = "Cuidado",
                actionText = "Dedica 10 minutos a algo que te haga bien",
            ),
            HabitIntention(
                id = "silencio",
                title = "Silencio",
                actionText = "Regálate unos minutos sin móvil ni ruido",
            ),
            HabitIntention(
                id = "orden",
                title = "Orden",
                actionText = "Guarda o limpia un objeto que uses a diario",
            ),
            HabitIntention(
                id = "movimiento",
                title = "Movimiento",
                actionText = "Da un paseo breve sin mirar el móvil",
            ),
            HabitIntention(
                id = "introspeccion",
                title = "Introspección",
                actionText = "Escribe una frase sobre cómo quieres sentirte hoy",
            ),
            HabitIntention(
                id = "descanso",
                title = "Descanso",
                actionText = "Baja el ritmo durante cinco minutos y respira",
            ),
            HabitIntention(
                id = "apertura",
                title = "Apertura",
                actionText = "Abre una ventana y renueva el aire de tu espacio",
            ),
        )
    }
}
