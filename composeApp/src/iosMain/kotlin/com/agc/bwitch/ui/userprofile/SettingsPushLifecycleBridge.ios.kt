package com.agc.bwitch.ui.userprofile

import androidx.compose.runtime.Composable
import com.agc.bwitch.domain.session.ClearLocalUserDataUseCase
import com.agc.bwitch.presentation.auth.SessionViewModel
import com.agc.bwitch.presentation.userprofile.SettingsViewModel

@Composable
actual fun rememberSyncPushPermissionState(viewModel: SettingsViewModel): suspend () -> Unit = {
    // iOS push is intentionally out of scope for the Android-first beta hardening.
}

@Composable
actual fun rememberHandleSecureSignOut(
    sessionViewModel: SessionViewModel,
    clearLocalUserData: ClearLocalUserDataUseCase,
): suspend () -> Unit = {
    sessionViewModel.signOut().join()
    clearLocalUserData()
}
