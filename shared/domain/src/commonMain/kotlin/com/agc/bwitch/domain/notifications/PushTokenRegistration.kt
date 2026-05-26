package com.agc.bwitch.domain.notifications

data class PushTokenRegistration(
    val token: String,
    val platform: PushPlatform,
    val appVersion: String?,
    val locale: String?,
    val timezone: String?,
    val notificationsPermissionGranted: Boolean,
)
