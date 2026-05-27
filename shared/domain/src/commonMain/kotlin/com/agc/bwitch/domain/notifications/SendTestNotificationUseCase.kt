package com.agc.bwitch.domain.notifications

class SendTestNotificationUseCase(
    private val repository: PushTestNotificationRepository,
) {
    suspend operator fun invoke() = repository.sendTestNotification()
}
