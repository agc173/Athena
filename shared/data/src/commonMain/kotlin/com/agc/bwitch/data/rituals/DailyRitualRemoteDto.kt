package com.agc.bwitch.data.rituals

import com.agc.bwitch.domain.rituals.DailyRitualTheme
import kotlinx.serialization.Serializable

@Serializable
data class DailyRitualRemoteDto(
    val selectedDateIso: String? = null,
    val selectedTemplateId: String? = null,
    val selectedTheme: String? = null,
    val dailyCompletionDateIso: String? = null,
    val dailyCompleted: Boolean = false,
    val lastCompletedDateIso: String? = null,
    val streakCount: Int = 0,
    val updatedAtEpochMillis: Long = 0L,
) {
    internal fun toLocalState(): DailyRitualLocalState =
        DailyRitualLocalState(
            selectedDateIso = selectedDateIso,
            selectedTemplateId = selectedTemplateId,
            selectedTheme = selectedTheme
                ?.let { raw -> runCatching { DailyRitualTheme.valueOf(raw) }.getOrNull() },
            dailyCompletionDateIso = dailyCompletionDateIso,
            dailyCompleted = dailyCompleted,
            lastCompletedDateIso = lastCompletedDateIso,
            streakCount = streakCount,
            updatedAtEpochMillis = updatedAtEpochMillis,
        )

    companion object {
        internal fun fromLocalState(state: DailyRitualLocalState): DailyRitualRemoteDto =
            DailyRitualRemoteDto(
                selectedDateIso = state.selectedDateIso,
                selectedTemplateId = state.selectedTemplateId,
                selectedTheme = state.selectedTheme?.name,
                dailyCompletionDateIso = state.dailyCompletionDateIso,
                dailyCompleted = state.dailyCompleted,
                lastCompletedDateIso = state.lastCompletedDateIso,
                streakCount = state.streakCount,
                updatedAtEpochMillis = state.updatedAtEpochMillis,
            )
    }
}
