package com.agc.bwitch.domain.settings

import kotlinx.coroutines.flow.Flow

interface NotificationSettingsRepository {
    suspend fun get(): NotificationSettings
    fun observe(): Flow<NotificationSettings>
    suspend fun update(settings: NotificationSettings)
}
