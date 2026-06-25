package com.agc.bwitch.domain.notifications

interface PushRegistrationRepository {
    suspend fun getPreferences(): PushNotificationPreferences?
    suspend fun registerToken(payload: PushTokenRegistration)
    suspend fun unregisterToken(token: String, platform: PushPlatform)
    suspend fun updatePreferences(preferences: PushNotificationPreferences)
}
