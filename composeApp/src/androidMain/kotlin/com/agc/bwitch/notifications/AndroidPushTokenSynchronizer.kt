package com.agc.bwitch.notifications

import android.content.Context
import android.content.SharedPreferences
import com.agc.bwitch.domain.auth.AuthRepository
import com.agc.bwitch.domain.notifications.PushPlatform
import com.agc.bwitch.domain.notifications.PushRegistrationRepository
import com.agc.bwitch.domain.notifications.PushTokenRegistration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale
import java.util.TimeZone

class AndroidPushTokenSynchronizer(
    private val context: Context,
    private val pushManager: AndroidPushNotificationManager,
    private val pushRegistrationRepository: PushRegistrationRepository,
    private val authRepository: AuthRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private var started = false

    fun start() {
        if (started) return
        started = true

        scope.launch {
            authRepository.authState
                .distinctUntilChangedBy { it?.uid }
                .collect { user ->
                    if (user?.uid.isNullOrBlank()) return@collect
                    syncPendingUnregisterForUser(user.uid)
                    syncCurrentTokenIfPossible(reason = "auth_ready")
                }
        }
    }

    fun onNewToken(token: String) {
        if (token.isBlank()) return
        prefs.edit().putString(KEY_PENDING_REGISTER_TOKEN, token).apply()
        scope.launch { syncTokenIfPossible(token = token, reason = "on_new_token") }
    }

    fun syncCurrentTokenInBackground(reason: String) {
        scope.launch { syncCurrentTokenIfPossible(reason) }
    }

    suspend fun syncCurrentTokenIfPossible(reason: String) {
        if (!pushManager.hasNotificationPermission()) return
        val pendingToken = prefs.getString(KEY_PENDING_REGISTER_TOKEN, null)
        val token = pendingToken?.takeIf { it.isNotBlank() } ?: pushManager.getCurrentToken()
        if (token.isNullOrBlank()) return
        syncTokenIfPossible(token = token, reason = reason)
    }

    suspend fun unregisterCurrentTokenBeforeSignOut() {
        val user = authRepository.authState.first()
        val uid = user?.uid?.takeIf { it.isNotBlank() } ?: return
        val token = pushManager.getCurrentToken()?.takeIf { it.isNotBlank() } ?: return
        unregisterTokenForCurrentUserOrPend(uid = uid, token = token, reason = "logout")
    }

    suspend fun unregisterCurrentTokenBecausePermissionRevoked() {
        val user = authRepository.authState.first()
        val uid = user?.uid?.takeIf { it.isNotBlank() } ?: return
        val token = pushManager.getCurrentToken()?.takeIf { it.isNotBlank() } ?: return
        unregisterTokenForCurrentUserOrPend(uid = uid, token = token, reason = "permission_revoked")
    }

    private suspend fun syncTokenIfPossible(token: String, reason: String) {
        val user = authRepository.authState.first()
        if (user?.uid.isNullOrBlank()) {
            prefs.edit().putString(KEY_PENDING_REGISTER_TOKEN, token).apply()
            return
        }
        if (!pushManager.hasNotificationPermission()) {
            prefs.edit().putString(KEY_PENDING_REGISTER_TOKEN, token).apply()
            return
        }

        runCatching {
            pushRegistrationRepository.registerToken(
                PushTokenRegistration(
                    token = token,
                    platform = PushPlatform.ANDROID,
                    appVersion = resolveAppVersion(),
                    locale = Locale.getDefault().toLanguageTag(),
                    timezone = TimeZone.getDefault().id,
                    notificationsPermissionGranted = true,
                ),
            )
        }.onSuccess {
            if (prefs.getString(KEY_PENDING_REGISTER_TOKEN, null) == token) {
                prefs.edit().remove(KEY_PENDING_REGISTER_TOKEN).apply()
            }
            println("BWITCH_PUSH_SYNC register_ok reason=$reason")
        }.onFailure { error ->
            prefs.edit().putString(KEY_PENDING_REGISTER_TOKEN, token).apply()
            println("BWITCH_PUSH_SYNC register_failed reason=$reason message=${error.message}")
        }
    }

    private suspend fun syncPendingUnregisterForUser(uid: String) {
        val pendingUid = prefs.getString(KEY_PENDING_UNREGISTER_UID, null)
        val pendingToken = prefs.getString(KEY_PENDING_UNREGISTER_TOKEN, null)
        if (pendingUid != uid || pendingToken.isNullOrBlank()) return

        runCatching { pushRegistrationRepository.unregisterToken(pendingToken, PushPlatform.ANDROID) }
            .onSuccess {
                prefs.edit()
                    .remove(KEY_PENDING_UNREGISTER_UID)
                    .remove(KEY_PENDING_UNREGISTER_TOKEN)
                    .apply()
                println("BWITCH_PUSH_SYNC pending_unregister_ok")
            }
            .onFailure { error ->
                println("BWITCH_PUSH_SYNC pending_unregister_failed message=${error.message}")
            }
    }

    private suspend fun unregisterTokenForCurrentUserOrPend(uid: String, token: String, reason: String) {
        runCatching { pushRegistrationRepository.unregisterToken(token, PushPlatform.ANDROID) }
            .onSuccess {
                prefs.edit()
                    .remove(KEY_PENDING_UNREGISTER_UID)
                    .remove(KEY_PENDING_UNREGISTER_TOKEN)
                    .apply()
                println("BWITCH_PUSH_SYNC unregister_ok reason=$reason")
            }
            .onFailure { error ->
                prefs.edit()
                    .putString(KEY_PENDING_UNREGISTER_UID, uid)
                    .putString(KEY_PENDING_UNREGISTER_TOKEN, token)
                    .apply()
                println("BWITCH_PUSH_SYNC unregister_pending reason=$reason message=${error.message}")
            }
    }

    private fun resolveAppVersion(): String? = runCatching {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        val versionName = info.versionName?.takeIf { it.isNotBlank() }
        val versionCode = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            info.longVersionCode.toString()
        } else {
            @Suppress("DEPRECATION")
            info.versionCode.toString()
        }
        listOfNotNull(versionName, versionCode).joinToString("+").takeIf { it.isNotBlank() }
    }.getOrNull()

    private companion object {
        const val PREFS_NAME = "bwitch_push_sync"
        const val KEY_PENDING_REGISTER_TOKEN = "pending_register_token"
        const val KEY_PENDING_UNREGISTER_UID = "pending_unregister_uid"
        const val KEY_PENDING_UNREGISTER_TOKEN = "pending_unregister_token"
    }
}
