package com.agc.bwitch.ui.userprofile

import androidx.compose.runtime.Composable
import com.agc.bwitch.presentation.userprofile.SettingsViewModel

@Composable
actual fun rememberHandlePushPermissionRequest(viewModel: SettingsViewModel): suspend () -> Unit = {
    viewModel.onPushPermissionAndTokenResolved(permissionGranted = false, token = null)
}
