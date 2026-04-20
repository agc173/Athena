package com.agc.bwitch.data.settings

import com.agc.bwitch.data.storage.SettingsFactory
import com.agc.bwitch.domain.settings.NotificationSettings
import com.agc.bwitch.domain.settings.NotificationSettingsRepository
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged

class SettingsNotificationSettingsRepository(
    settingsFactory: SettingsFactory,
) : NotificationSettingsRepository {

    private val settings: Settings = settingsFactory.create("bwitch_notifications")

    private val state = MutableStateFlow(readSettings())

    override suspend fun get(): NotificationSettings = state.value

    override fun observe(): Flow<NotificationSettings> = state.distinctUntilChanged()

    override suspend fun update(settings: NotificationSettings) {
        persist(settings)
        state.value = settings
    }

    private fun readSettings(): NotificationSettings = NotificationSettings(
        globalEnabled = settings.getBoolean(GLOBAL_ENABLED_KEY, false),
        dailyHoroscopeEnabled = settings.getBoolean(DAILY_HOROSCOPE_ENABLED_KEY, false),
        ritualOfDayEnabled = settings.getBoolean(RITUAL_OF_DAY_ENABLED_KEY, false),
        habitsEnabled = settings.getBoolean(HABITS_ENABLED_KEY, false),
    )

    private fun persist(state: NotificationSettings) {
        settings.putBoolean(GLOBAL_ENABLED_KEY, state.globalEnabled)
        settings.putBoolean(DAILY_HOROSCOPE_ENABLED_KEY, state.dailyHoroscopeEnabled)
        settings.putBoolean(RITUAL_OF_DAY_ENABLED_KEY, state.ritualOfDayEnabled)
        settings.putBoolean(HABITS_ENABLED_KEY, state.habitsEnabled)
    }

    private companion object {
        private const val GLOBAL_ENABLED_KEY = "notifications_global_enabled"
        private const val DAILY_HOROSCOPE_ENABLED_KEY = "notifications_daily_horoscope_enabled"
        private const val RITUAL_OF_DAY_ENABLED_KEY = "notifications_ritual_of_day_enabled"
        private const val HABITS_ENABLED_KEY = "notifications_habits_enabled"
    }
}
