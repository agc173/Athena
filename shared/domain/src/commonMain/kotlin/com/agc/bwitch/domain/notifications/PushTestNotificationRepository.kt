package com.agc.bwitch.domain.notifications

interface PushTestNotificationRepository {
    suspend fun sendTestNotification()
}
