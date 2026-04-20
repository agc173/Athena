package com.agc.bwitch.domain.settings

class UpdateNotificationSettingsUseCase(
    private val repository: NotificationSettingsRepository,
) {
    suspend operator fun invoke(settings: NotificationSettings) = repository.update(settings)
}
