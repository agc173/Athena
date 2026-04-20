package com.agc.bwitch.domain.settings

class GetNotificationSettingsUseCase(
    private val repository: NotificationSettingsRepository,
) {
    suspend operator fun invoke(): NotificationSettings = repository.get()
}
