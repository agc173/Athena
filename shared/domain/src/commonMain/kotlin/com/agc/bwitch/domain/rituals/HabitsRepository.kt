package com.agc.bwitch.domain.rituals

interface HabitsRepository {
    fun getTodayIntentions(): List<HabitIntention>
    fun getTodayState(): DailyHabitsState
    fun getProgress(): HabitsProgress
    fun markCompleted(intentionId: String)
    fun unmarkCompleted(intentionId: String)
}
