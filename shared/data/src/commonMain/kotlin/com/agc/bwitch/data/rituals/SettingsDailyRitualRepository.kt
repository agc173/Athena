package com.agc.bwitch.data.rituals

import com.agc.bwitch.data.rituals.local.localDailyRitualTemplates
import com.agc.bwitch.data.storage.SettingsFactory
import com.agc.bwitch.domain.rituals.DailyRitualRepository
import com.agc.bwitch.domain.rituals.DailyRitualTemplate
import com.agc.bwitch.domain.rituals.DailyRitualTheme
import com.russhwolf.settings.Settings
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil

class SettingsDailyRitualRepository(
    settingsFactory: SettingsFactory,
) : DailyRitualRepository {

    private val settings: Settings = settingsFactory.create("daily_ritual")

    private val KEY_SELECTED_DATE_ISO = "selected_date_iso"
    private val KEY_SELECTED_TEMPLATE_ID = "selected_template_id"
    private val KEY_SELECTED_THEME = "selected_theme"
    private val KEY_DAILY_COMPLETION_DATE_ISO = "daily_completion_date_iso"
    private val KEY_DAILY_COMPLETED = "daily_completed"
    private val KEY_LAST_COMPLETED_DATE_ISO = "last_completed_date_iso"
    private val KEY_STREAK_COUNT = "streak_count"
    private val KEY_UPDATED_AT_EPOCH_MILLIS = "updated_at_epoch_millis"

    override fun getTemplateForDate(date: LocalDate): DailyRitualTemplate {
        val dateIso = date.toString()
        val selectedDate = settings.getString(KEY_SELECTED_DATE_ISO, defaultValue = "")
            .takeIf { it.isNotEmpty() }
        val selectedTemplateId = settings.getString(KEY_SELECTED_TEMPLATE_ID, defaultValue = "")
            .takeIf { it.isNotEmpty() }

        if (selectedDate == dateIso && !selectedTemplateId.isNullOrBlank()) {
            ensureDailyCompletionState(dateIso)
            findTemplateById(selectedTemplateId)?.let { return it }
        }

        val previousTemplateId = selectedTemplateId
        val previousTheme = settings.getString(KEY_SELECTED_THEME, defaultValue = "")
            .takeIf { it.isNotEmpty() }
            ?.let { raw ->
                runCatching { DailyRitualTheme.valueOf(raw) }.getOrNull()
            }

        val chosen = pickTemplate(date, previousTemplateId, previousTheme)

        settings.putString(KEY_SELECTED_DATE_ISO, dateIso)
        settings.putString(KEY_SELECTED_TEMPLATE_ID, chosen.id)
        settings.putString(KEY_SELECTED_THEME, chosen.theme.name)
        settings.putString(KEY_DAILY_COMPLETION_DATE_ISO, dateIso)
        settings.putBoolean(KEY_DAILY_COMPLETED, false)
        touchUpdatedAt()

        return chosen
    }

    override fun isCompletedOn(date: LocalDate): Boolean {
        val dateIso = date.toString()
        ensureDailyCompletionState(dateIso)
        return settings.getBoolean(KEY_DAILY_COMPLETED, defaultValue = false)
    }

    override fun getStreakForDate(date: LocalDate): Int {
        val storedStreak = settings.getInt(KEY_STREAK_COUNT, defaultValue = 0)
        val lastCompleted = settings.getString(KEY_LAST_COMPLETED_DATE_ISO, defaultValue = "")
            .takeIf { it.isNotEmpty() }
            ?.let(::safeDate)
        return adjustStreakForMissedDays(storedStreak, lastCompleted, date)
    }

    override fun completeOn(date: LocalDate): Int {
        val dateIso = date.toString()
        if (isCompletedOn(date)) {
            return getStreakForDate(date)
        }

        val storedStreak = settings.getInt(KEY_STREAK_COUNT, defaultValue = 0)
        val lastCompleted = settings.getString(KEY_LAST_COMPLETED_DATE_ISO, defaultValue = "")
            .takeIf { it.isNotEmpty() }
            ?.let(::safeDate)
        val adjusted = adjustStreakForMissedDays(storedStreak, lastCompleted, date)
        val newStreak = adjusted + 1

        settings.putInt(KEY_STREAK_COUNT, newStreak)
        settings.putString(KEY_LAST_COMPLETED_DATE_ISO, dateIso)
        settings.putString(KEY_DAILY_COMPLETION_DATE_ISO, dateIso)
        settings.putBoolean(KEY_DAILY_COMPLETED, true)
        touchUpdatedAt()

        return newStreak
    }

    internal fun getStateSnapshot(): DailyRitualLocalState {
        return DailyRitualLocalState(
            selectedDateIso = settings.getString(KEY_SELECTED_DATE_ISO, defaultValue = "").ifBlank { null },
            selectedTemplateId = settings.getString(KEY_SELECTED_TEMPLATE_ID, defaultValue = "").ifBlank { null },
            selectedTheme = settings.getString(KEY_SELECTED_THEME, defaultValue = "")
                .ifBlank { null }
                ?.let { raw -> runCatching { DailyRitualTheme.valueOf(raw) }.getOrNull() },
            dailyCompletionDateIso = settings.getString(KEY_DAILY_COMPLETION_DATE_ISO, defaultValue = "").ifBlank { null },
            dailyCompleted = settings.getBoolean(KEY_DAILY_COMPLETED, defaultValue = false),
            lastCompletedDateIso = settings.getString(KEY_LAST_COMPLETED_DATE_ISO, defaultValue = "").ifBlank { null },
            streakCount = settings.getInt(KEY_STREAK_COUNT, defaultValue = 0),
            updatedAtEpochMillis = settings.getLong(KEY_UPDATED_AT_EPOCH_MILLIS, 0L)
        )
    }

    internal fun saveStateSnapshot(state: DailyRitualLocalState) {
        putNullableString(KEY_SELECTED_DATE_ISO, state.selectedDateIso)
        putNullableString(KEY_SELECTED_TEMPLATE_ID, state.selectedTemplateId)
        putNullableString(KEY_SELECTED_THEME, state.selectedTheme?.name)
        putNullableString(KEY_DAILY_COMPLETION_DATE_ISO, state.dailyCompletionDateIso)
        settings.putBoolean(KEY_DAILY_COMPLETED, state.dailyCompleted)
        putNullableString(KEY_LAST_COMPLETED_DATE_ISO, state.lastCompletedDateIso)
        settings.putInt(KEY_STREAK_COUNT, state.streakCount)
        settings.putLong(KEY_UPDATED_AT_EPOCH_MILLIS, state.updatedAtEpochMillis)
    }

    internal fun getLocalUpdatedAtEpochMillisOrNull(): Long? {
        val value = settings.getLong(KEY_UPDATED_AT_EPOCH_MILLIS, 0L)
        return value.takeIf { it > 0L }
    }

    private fun ensureDailyCompletionState(dateIso: String) {
        val completionDateIso = settings.getString(KEY_DAILY_COMPLETION_DATE_ISO, defaultValue = "")
        if (completionDateIso != dateIso) {
            settings.putString(KEY_DAILY_COMPLETION_DATE_ISO, dateIso)
            settings.putBoolean(KEY_DAILY_COMPLETED, false)
            touchUpdatedAt()
        }
    }

    private fun touchUpdatedAt() {
        settings.putLong(KEY_UPDATED_AT_EPOCH_MILLIS, Clock.System.now().toEpochMilliseconds())
    }

    private fun putNullableString(key: String, value: String?) {
        if (value.isNullOrBlank()) settings.remove(key) else settings.putString(key, value)
    }

    private fun pickTemplate(
        date: LocalDate,
        previousTemplateId: String?,
        previousTheme: DailyRitualTheme?,
    ): DailyRitualTemplate {
        val withoutPrevious = localDailyRitualTemplates.filter { template ->
            template.id != previousTemplateId
        }.ifEmpty { localDailyRitualTemplates }

        val differentTheme = withoutPrevious.filter { template ->
            previousTheme == null || template.theme != previousTheme
        }

        val pool = if (differentTheme.isNotEmpty()) differentTheme else withoutPrevious
        val index = stableIndexForDate(date, pool.size)
        return pool[index]
    }

    private fun stableIndexForDate(date: LocalDate, size: Int): Int {
        if (size <= 1) return 0
        val seed = date.hashCode().toLong().let { if (it < 0) -it else it }
        return (seed % size).toInt()
    }

    private fun adjustStreakForMissedDays(
        streak: Int,
        lastCompleted: LocalDate?,
        today: LocalDate,
    ): Int {
        if (streak <= 0 || lastCompleted == null) return 0

        val diffDays = lastCompleted.daysUntil(today)
        return if (diffDays <= 1) streak else 0
    }

    private fun findTemplateById(id: String): DailyRitualTemplate? =
        localDailyRitualTemplates.firstOrNull { template -> template.id == id }

    private fun safeDate(value: String): LocalDate? = runCatching { LocalDate.parse(value) }.getOrNull()
}

internal data class DailyRitualLocalState(
    val selectedDateIso: String? = null,
    val selectedTemplateId: String? = null,
    val selectedTheme: DailyRitualTheme? = null,
    val dailyCompletionDateIso: String? = null,
    val dailyCompleted: Boolean = false,
    val lastCompletedDateIso: String? = null,
    val streakCount: Int = 0,
    val updatedAtEpochMillis: Long = 0L,
)
