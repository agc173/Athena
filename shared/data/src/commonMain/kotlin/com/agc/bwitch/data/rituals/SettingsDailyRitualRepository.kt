package com.agc.bwitch.data.rituals

import com.agc.bwitch.data.rituals.local.localDailyRitualTemplates
import com.agc.bwitch.data.storage.SettingsFactory
import com.agc.bwitch.domain.rituals.DailyRitualRepository
import com.agc.bwitch.domain.rituals.DailyRitualTemplate
import com.agc.bwitch.domain.rituals.DailyRitualTheme
import com.russhwolf.settings.Settings
import com.russhwolf.settings.get
import com.russhwolf.settings.set
import kotlinx.datetime.LocalDate
import kotlinx.datetime.daysUntil

class SettingsDailyRitualRepository(
    settingsFactory: SettingsFactory,
) : DailyRitualRepository {

    private val settings: Settings = settingsFactory.create("daily_ritual")

    private val KEY_SELECTED_DATE_ISO = "selected_date_iso"
    private val KEY_SELECTED_TEMPLATE_ID = "selected_template_id"
    private val KEY_SELECTED_THEME = "selected_theme"
    private val KEY_LAST_COMPLETED_DATE_ISO = "last_completed_date_iso"
    private val KEY_STREAK_COUNT = "streak_count"

    override fun getTemplateForDate(date: LocalDate): DailyRitualTemplate {
        val dateIso = date.toString()
        val selectedDate = settings[KEY_SELECTED_DATE_ISO]
        val selectedTemplateId = settings[KEY_SELECTED_TEMPLATE_ID]

        if (selectedDate == dateIso && !selectedTemplateId.isNullOrBlank()) {
            findTemplateById(selectedTemplateId)?.let { return it }
        }

        val previousTemplateId = selectedTemplateId
        val previousTheme = settings[KEY_SELECTED_THEME]?.let { raw ->
            runCatching { DailyRitualTheme.valueOf(raw) }.getOrNull()
        }

        val chosen = pickTemplate(date, previousTemplateId, previousTheme)

        settings[KEY_SELECTED_DATE_ISO] = dateIso
        settings[KEY_SELECTED_TEMPLATE_ID] = chosen.id
        settings[KEY_SELECTED_THEME] = chosen.theme.name

        return chosen
    }

    override fun isCompletedOn(date: LocalDate): Boolean =
        settings[KEY_LAST_COMPLETED_DATE_ISO] == date.toString()

    override fun getStreakForDate(date: LocalDate): Int {
        val storedStreak = settings.getInt(KEY_STREAK_COUNT, defaultValue = 0)
        val lastCompleted = settings[KEY_LAST_COMPLETED_DATE_ISO]?.let(::safeDate)
        return adjustStreakForMissedDays(storedStreak, lastCompleted, date)
    }

    override fun completeOn(date: LocalDate): Int {
        val dateIso = date.toString()
        if (settings[KEY_LAST_COMPLETED_DATE_ISO] == dateIso) {
            return settings.getInt(KEY_STREAK_COUNT, defaultValue = 0)
        }

        val storedStreak = settings.getInt(KEY_STREAK_COUNT, defaultValue = 0)
        val lastCompleted = settings[KEY_LAST_COMPLETED_DATE_ISO]?.let(::safeDate)
        val adjusted = adjustStreakForMissedDays(storedStreak, lastCompleted, date)
        val newStreak = adjusted + 1

        settings[KEY_STREAK_COUNT] = newStreak
        settings[KEY_LAST_COMPLETED_DATE_ISO] = dateIso

        return newStreak
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
        if (diffDays <= 1) return streak

        val missedDays = diffDays - 1
        return (streak - missedDays).coerceAtLeast(0)
    }

    private fun findTemplateById(id: String): DailyRitualTemplate? =
        localDailyRitualTemplates.firstOrNull { template -> template.id == id }

    private fun safeDate(value: String): LocalDate? = runCatching { LocalDate.parse(value) }.getOrNull()
}
