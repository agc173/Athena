package com.agc.bwitch.presentation.rituals

data class HabitsUiState(
    val isLoading: Boolean = true,
    val progressPoints: Int = 0,
    val cycleTarget: Int = 60,
    val completedCycles: Int = 0,
    val glowLevel: HabitsGlowLevel = HabitsGlowLevel.Base,
    val intentions: List<HabitIntentionUiModel> = emptyList(),
)

data class HabitIntentionUiModel(
    val id: String,
    val title: String,
    val actionText: String,
    val isCompleted: Boolean,
)
