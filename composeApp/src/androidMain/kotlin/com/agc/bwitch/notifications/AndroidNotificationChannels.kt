package com.agc.bwitch.notifications

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.util.Log

object AndroidNotificationChannels {
    const val CHANNEL_DAILY = "bwitch_daily"
    const val DEFAULT_CHANNEL_ID = CHANNEL_DAILY
    const val CHANNEL_REWARDS = "bwitch_rewards"
    const val CHANNEL_SPIRITUAL = "bwitch_spiritual"

    fun create(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            Log.i(TAG, "Notification channels skipped: sdk=${Build.VERSION.SDK_INT}")
            return
        }

        val manager = context.getSystemService(NotificationManager::class.java) ?: run {
            Log.w(TAG, "Notification channels skipped: notification_manager_unavailable")
            return
        }
        val channels = listOf(
            NotificationChannel(
                CHANNEL_DAILY,
                "Daily guidance",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Daily horoscope and ritual reminders"
            },
            NotificationChannel(
                CHANNEL_REWARDS,
                "Rewards",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Rewards and moon pack activity"
            },
            NotificationChannel(
                CHANNEL_SPIRITUAL,
                "Spiritual updates",
                NotificationManager.IMPORTANCE_DEFAULT,
            ).apply {
                description = "Spiritual content and feature updates"
            },
        )

        manager.createNotificationChannels(channels)
        Log.i(TAG, "Notification channels created: ids=${channels.joinToString { it.id }}")
    }

    private const val TAG = "BwitchNotifChannels"
}
