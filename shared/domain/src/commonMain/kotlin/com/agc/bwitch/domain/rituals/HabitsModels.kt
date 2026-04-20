package com.agc.bwitch.domain.rituals

data class HabitIntention(
    val id: String,
    val titleKey: String,
    val actionTextKey: String,
)

data class HabitsProgress(
    val currentCyclePoints: Int,
    val cycleTarget: Int = 60,
    val completedCycles: Int,
)

data class DailyHabitsState(
    val date: String,
    val selectedIntentionIds: List<String>,
    val completedIntentionIds: Set<String>,
)
