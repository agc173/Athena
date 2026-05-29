package com.agc.bwitch.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.tasks.await

class AndroidPushNotificationManager(
    private val context: Context,
    private val firebaseMessaging: FirebaseMessaging = FirebaseMessaging.getInstance(),
) {
    fun areNotificationsEnabled(): Boolean = NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun shouldRequestRuntimePermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return false

        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) != PackageManager.PERMISSION_GRANTED
    }

    fun hasNotificationPermission(): Boolean {
        if (!areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true

        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS,
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun createNotificationChannels() {
        AndroidNotificationChannels.create(context)
    }

    suspend fun getCurrentToken(): String? = runCatching { firebaseMessaging.token.await() }.getOrNull()

    suspend fun deleteCurrentToken() {
        runCatching { firebaseMessaging.deleteToken().await() }
    }
}
