package com.agc.bwitch.ui.userprofile

import androidx.compose.runtime.Composable
import com.agc.bwitch.domain.session.ClearLocalUserDataUseCase
import com.agc.bwitch.notifications.AndroidPushNotificationManager
import com.agc.bwitch.notifications.AndroidPushTokenSynchronizer
import com.agc.bwitch.presentation.auth.SessionViewModel
import com.agc.bwitch.presentation.userprofile.SettingsViewModel
import kotlinx.datetime.TimeZone
import org.koin.compose.koinInject

@Composable
actual fun rememberSyncPushPermissionState(viewModel: SettingsViewModel): suspend () -> Unit {
    val pushManager: AndroidPushNotificationManager = koinInject()
    val synchronizer: AndroidPushTokenSynchronizer = koinInject()

    return {
        val timezone = TimeZone.currentSystemDefault().id
        if (pushManager.hasNotificationPermission()) {
            synchronizer.syncCurrentTokenIfPossible(reason = "settings_open")
        } else {
            synchronizer.unregisterCurrentTokenBecausePermissionRevoked()
            viewModel.onPushPermissionAndTokenResolved(
                permissionGranted = false,
                token = null,
                timezone = timezone,
            )
        }
    }
}

@Composable
actual fun rememberHandleSecureSignOut(
    sessionViewModel: SessionViewModel,
    clearLocalUserData: ClearLocalUserDataUseCase,
): suspend () -> Unit {
    val synchronizer: AndroidPushTokenSynchronizer = koinInject()

    return {
        synchronizer.unregisterCurrentTokenBeforeSignOut()
        sessionViewModel.signOut()
        clearLocalUserData()
    }
}
