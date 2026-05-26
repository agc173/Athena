package com.agc.bwitch.ui.userprofile

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import com.agc.bwitch.notifications.AndroidPushNotificationManager
import com.agc.bwitch.presentation.userprofile.SettingsViewModel
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
actual fun rememberHandlePushPermissionRequest(viewModel: SettingsViewModel): suspend () -> Unit {
    val pushManager: AndroidPushNotificationManager = koinInject()
    val scope = rememberCoroutineScope()
    var pendingAfterPermission by remember { mutableStateOf(false) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (!pendingAfterPermission) return@rememberLauncherForActivityResult
        pendingAfterPermission = false
        scope.launch {
            val token = if (granted && pushManager.areNotificationsEnabled()) pushManager.getCurrentToken() else null
            viewModel.onPushPermissionAndTokenResolved(permissionGranted = granted, token = token)
        }
    }

    return {
        if (pushManager.shouldRequestRuntimePermission()) {
            pendingAfterPermission = true
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else if (!pushManager.areNotificationsEnabled()) {
            viewModel.onPushPermissionAndTokenResolved(permissionGranted = false, token = null)
        } else {
            val token = pushManager.getCurrentToken()
            viewModel.onPushPermissionAndTokenResolved(permissionGranted = true, token = token)
        }
    }
}
