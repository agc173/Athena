package com.agc.bwitch.domain.settings

import kotlinx.coroutines.flow.Flow

class ObserveNotificationSettingsUseCase(
    private val repository: NotificationSettingsRepository,
) {
    operator fun invoke(): Flow<NotificationSettings> = repository.observe()
}
