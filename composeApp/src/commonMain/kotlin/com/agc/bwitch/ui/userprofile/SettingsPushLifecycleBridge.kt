package com.agc.bwitch.ui.userprofile

import androidx.compose.runtime.Composable
import com.agc.bwitch.domain.session.ClearLocalUserDataUseCase
import com.agc.bwitch.presentation.auth.SessionViewModel
import com.agc.bwitch.presentation.userprofile.SettingsViewModel

@Composable
expect fun rememberSyncPushPermissionState(viewModel: SettingsViewModel): suspend () -> Unit

@Composable
expect fun rememberHandleSecureSignOut(
    sessionViewModel: SessionViewModel,
    clearLocalUserData: ClearLocalUserDataUseCase,
): suspend () -> Unit
