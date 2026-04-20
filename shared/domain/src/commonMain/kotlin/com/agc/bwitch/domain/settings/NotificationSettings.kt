package com.agc.bwitch.domain.settings

data class NotificationSettings(
    val globalEnabled: Boolean = false,
    val dailyHoroscopeEnabled: Boolean = false,
    val ritualOfDayEnabled: Boolean = false,
    val habitsEnabled: Boolean = false,
)
