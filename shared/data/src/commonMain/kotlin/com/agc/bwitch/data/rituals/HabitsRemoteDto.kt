package com.agc.bwitch.data.rituals

import kotlinx.serialization.Serializable

@Serializable
data class HabitsRemoteDto(
    val todayDateIso: String = "",
    val selectedIntentionIds: List<String> = emptyList(),
    val completedIntentionIds: List<String> = emptyList(),
    val progressPoints: Int = 0,
    val completedCycles: Int = 0,
    val updatedAtEpochMillis: Long = 0L,
) {
    internal fun toLocalState(): HabitsLocalState = HabitsLocalState(
        todayDateIso = todayDateIso,
        selectedIntentionIds = selectedIntentionIds,
        completedIntentionIds = completedIntentionIds.toSet(),
        progressPoints = progressPoints,
        completedCycles = completedCycles,
        updatedAtEpochMillis = updatedAtEpochMillis,
    )

    companion object {
        internal fun fromLocalState(state: HabitsLocalState): HabitsRemoteDto = HabitsRemoteDto(
            todayDateIso = state.todayDateIso,
            selectedIntentionIds = state.selectedIntentionIds,
            completedIntentionIds = state.completedIntentionIds.toList(),
            progressPoints = state.progressPoints,
            completedCycles = state.completedCycles,
            updatedAtEpochMillis = state.updatedAtEpochMillis,
        )
    }
}
