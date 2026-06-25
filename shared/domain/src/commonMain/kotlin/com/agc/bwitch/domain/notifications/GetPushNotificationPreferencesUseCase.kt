package com.agc.bwitch.domain.notifications

class GetPushNotificationPreferencesUseCase(
    private val repository: PushRegistrationRepository,
) {
    suspend operator fun invoke(): PushNotificationPreferences? = repository.getPreferences()
}
