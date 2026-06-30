package com.agc.bwitch.notifications

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
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
        val notificationsEnabled = areNotificationsEnabled()
        val runtimeGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS,
            ) == PackageManager.PERMISSION_GRANTED
        val granted = notificationsEnabled && runtimeGranted
        Log.i(TAG, "Push permission status: sdk=${Build.VERSION.SDK_INT} notificationsEnabled=$notificationsEnabled runtimeGranted=$runtimeGranted granted=$granted")
        return granted
    }

    fun createNotificationChannels() {
        AndroidNotificationChannels.create(context)
    }

    suspend fun getCurrentToken(): String? = runCatching { firebaseMessaging.token.await() }
        .onSuccess { token -> Log.i(TAG, "FCM token retrieved: prefix=${token.take(6)}... length=${token.length}") }
        .onFailure { error -> Log.w(TAG, "FCM token retrieval failed", error) }
        .getOrNull()

    companion object {
        private const val TAG = "AndroidPushManager"
    }

    suspend fun deleteCurrentToken() {
        runCatching { firebaseMessaging.deleteToken().await() }
    }
}
