package com.agc.bwitch.notifications

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.agc.bwitch.R
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage

class BwitchFirebaseMessagingService : FirebaseMessagingService() {
    override fun onNewToken(token: String) {
        super.onNewToken(token)

        val preview = token.take(6)
        Log.i(TAG, "onNewToken received (prefix=$preview..., length=${token.length})")
    }

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.data["title"] ?: message.notification?.title
        val body = message.data["body"] ?: message.notification?.body
        if (title.isNullOrBlank() || body.isNullOrBlank()) {
            Log.i(TAG, "Push message ignored (missing title/body)")
            return
        }

        if (!canPostNotifications()) {
            Log.i(TAG, "Push message received but notifications are disabled/no permission")
            return
        }

        val channelId = when (message.data["type"]?.lowercase()) {
            "rewards" -> AndroidNotificationChannels.CHANNEL_REWARDS
            "spiritual" -> AndroidNotificationChannels.CHANNEL_SPIRITUAL
            else -> AndroidNotificationChannels.CHANNEL_DAILY
        }

        // TODO: Replace with dedicated monochrome notification icon when asset is available.
        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        val notificationId = message.messageId?.hashCode() ?: System.currentTimeMillis().toInt()
        NotificationManagerCompat.from(this).notify(notificationId, notification)
    }

    private fun canPostNotifications(): Boolean {
        if (!NotificationManagerCompat.from(this).areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true

        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    companion object {
        private const val TAG = "BwitchFcmService"
    }
}
