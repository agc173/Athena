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
        val state = getStateSnapshot()
        return state.selectedIntentionIds.mapNotNull { id -> intentionsPool.find { it.id == id } }
    }

    override fun getTodayState(): DailyHabitsState = getStateSnapshot().toDailyHabitsState()

    override fun getProgress(): HabitsProgress = getStateSnapshot().toHabitsProgress()

    override fun markCompleted(intentionId: String) {
        val state = getStateSnapshot()
        if (intentionId !in state.selectedIntentionIds) return
        if (intentionId in state.completedIntentionIds) return

        val updatedCompleted = (state.completedIntentionIds + intentionId).take(MAX_DAILY_INTENTIONS).toSet()
        val updatedProgress = progressAfterDelta(
            currentPoints = state.progressPoints,
            currentCycles = state.completedCycles,
            delta = 1,
        )
        val updated = state.copy(
            completedIntentionIds = updatedCompleted,
            progressPoints = updatedProgress.first,
            completedCycles = updatedProgress.second,
            updatedAtEpochMillis = nowEpochMillis(),
        )
        saveStateSnapshot(updated)
    }

    override fun unmarkCompleted(intentionId: String) {
        val state = getStateSnapshot()
        if (intentionId !in state.completedIntentionIds) return

        val updatedCompleted = state.completedIntentionIds - intentionId
        val updatedProgress = progressAfterDelta(
            currentPoints = state.progressPoints,
            currentCycles = state.completedCycles,
            delta = -1,
        )
        val updated = state.copy(
            completedIntentionIds = updatedCompleted,
            progressPoints = updatedProgress.first,
            completedCycles = updatedProgress.second,
            updatedAtEpochMillis = nowEpochMillis(),
        )
        saveStateSnapshot(updated)
    }

    internal fun getStateSnapshot(): HabitsLocalState {
        val dailyState = ensureTodayState()
        return HabitsLocalState(
            todayDateIso = dailyState.date,
            selectedIntentionIds = dailyState.selectedIntentionIds,
            completedIntentionIds = dailyState.completedIntentionIds,
            progressPoints = settings.getInt(PROGRESS_POINTS_KEY, 0).coerceAtLeast(0),
            completedCycles = settings.getInt(COMPLETED_CYCLES_KEY, 0).coerceAtLeast(0),
            updatedAtEpochMillis = settings.getLong(UPDATED_AT_EPOCH_MILLIS_KEY, 0L),
        )
    }

    internal fun saveStateSnapshot(state: HabitsLocalState) {
        settings.putString(TODAY_DATE_KEY, state.todayDateIso)
        settings.putString(TODAY_SELECTED_IDS_KEY, state.selectedIntentionIds.joinToString(SEPARATOR))
        settings.putString(TODAY_COMPLETED_IDS_KEY, state.completedIntentionIds.joinToString(SEPARATOR))
        settings.putInt(PROGRESS_POINTS_KEY, state.progressPoints.coerceAtLeast(0))
        settings.putInt(COMPLETED_CYCLES_KEY, state.completedCycles.coerceAtLeast(0))
        settings.putLong(UPDATED_AT_EPOCH_MILLIS_KEY, state.updatedAtEpochMillis.coerceAtLeast(0L))
    }

    internal fun getLocalUpdatedAtEpochMillisOrNull(): Long? {
        val value = settings.getLong(UPDATED_AT_EPOCH_MILLIS_KEY, 0L)
        return if (value > 0L) value else null
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
        saveStateSnapshot(
            state = HabitsLocalState(
                todayDateIso = newState.date,
                selectedIntentionIds = newState.selectedIntentionIds,
                completedIntentionIds = newState.completedIntentionIds,
                progressPoints = settings.getInt(PROGRESS_POINTS_KEY, 0).coerceAtLeast(0),
                completedCycles = settings.getInt(COMPLETED_CYCLES_KEY, 0).coerceAtLeast(0),
                updatedAtEpochMillis = nowEpochMillis(),
            )
        )
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

    private fun progressAfterDelta(currentPoints: Int, currentCycles: Int, delta: Int): Pair<Int, Int> {
        if (delta > 0) {
            val total = currentPoints + delta
            val earnedCycles = total / CYCLE_TARGET
            val nextPoints = total % CYCLE_TARGET
            return nextPoints to (currentCycles + earnedCycles)
        }

        if (delta < 0) {
            if (currentPoints > 0) {
                return (currentPoints - 1) to currentCycles
            }
            if (currentCycles > 0) {
                return (CYCLE_TARGET - 1) to (currentCycles - 1)
            }
        }
        return currentPoints to currentCycles
    }

    private fun readIds(key: String): List<String> =
        settings.getStringOrNull(key)
            ?.split(SEPARATOR)
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            .orEmpty()

    private fun todayIsoDate(): String =
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date.toString()

    private fun nowEpochMillis(): Long = Clock.System.now().toEpochMilliseconds()

    private companion object {
        private const val CYCLE_TARGET = 60
        private const val MAX_DAILY_INTENTIONS = 3
        private const val SEPARATOR = ","

        private const val TODAY_DATE_KEY = "habits_today_date"
        private const val TODAY_SELECTED_IDS_KEY = "habits_today_selected_ids"
        private const val TODAY_COMPLETED_IDS_KEY = "habits_today_completed_ids"
        private const val PROGRESS_POINTS_KEY = "habits_progress_points"
        private const val COMPLETED_CYCLES_KEY = "habits_completed_cycles"
        private const val UPDATED_AT_EPOCH_MILLIS_KEY = "habits_updated_at_epoch_millis"

        private val intentionsPool = listOf(
            intention(id = "conexion"),
            intention(id = "gratitud"),
            intention(id = "calma"),
            intention(id = "presencia"),
            intention(id = "limpieza"),
            intention(id = "cuidado"),
            intention(id = "silencio"),
            intention(id = "orden"),
            intention(id = "movimiento"),
            intention(id = "introspeccion"),
            intention(id = "descanso"),
            intention(id = "apertura"),
        )

        private fun intention(id: String): HabitIntention = HabitIntention(
            id = id,
            titleKey = "habits.intention.$id.title",
            actionTextKey = "habits.intention.$id.action",
        )
    }
}

internal data class HabitsLocalState(
    val todayDateIso: String,
    val selectedIntentionIds: List<String>,
    val completedIntentionIds: Set<String>,
    val progressPoints: Int,
    val completedCycles: Int,
    val updatedAtEpochMillis: Long = 0L,
) {
    fun toDailyHabitsState(): DailyHabitsState = DailyHabitsState(
        date = todayDateIso,
        selectedIntentionIds = selectedIntentionIds,
        completedIntentionIds = completedIntentionIds,
    )

    fun toHabitsProgress(): HabitsProgress = HabitsProgress(
        currentCyclePoints = progressPoints,
        cycleTarget = 60,
        completedCycles = completedCycles,
    )
}
