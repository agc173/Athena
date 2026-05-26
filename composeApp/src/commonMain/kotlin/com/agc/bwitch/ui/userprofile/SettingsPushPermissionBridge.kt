package com.agc.bwitch.ui.userprofile

import androidx.compose.runtime.Composable
import com.agc.bwitch.presentation.userprofile.SettingsViewModel

@Composable
expect fun rememberHandlePushPermissionRequest(viewModel: SettingsViewModel): suspend () -> Unit
