package com.agc.bwitch.domain.notifications

data class PushNotificationPreferences(
    val globalEnabled: Boolean,
    val dailyHoroscopeEnabled: Boolean,
    val dailyRewardEnabled: Boolean,
    val tarotOracleReminderEnabled: Boolean,
    val ritualsEnabled: Boolean,
    val habitsEnabled: Boolean,
)
