package com.agc.bwitch.domain.notifications

class UpdatePushNotificationPreferencesUseCase(
    private val repository: PushRegistrationRepository,
) {
    suspend operator fun invoke(preferences: PushNotificationPreferences) =
        repository.updatePreferences(preferences)
}
