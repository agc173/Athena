package com.agc.bwitch.ui.userprofile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.agc.bwitch.domain.session.ClearLocalUserDataUseCase
import com.agc.bwitch.localization.appStrings
import com.agc.bwitch.presentation.auth.SessionViewModel
import com.agc.bwitch.presentation.localization.AppLanguageViewModel
import com.agc.bwitch.presentation.userprofile.UserProfileViewModel
import com.agc.bwitch.ui.localization.LanguageSelectorSection
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun SettingsScreen(contentPadding: PaddingValues) {
    val vm: UserProfileViewModel = koinInject()
    val state by vm.uiState.collectAsState()
    val strings = appStrings
    val appLanguageVm: AppLanguageViewModel = koinInject()
    val appLanguageState by appLanguageVm.uiState.collectAsState()
    val sessionVm: SessionViewModel = koinInject()
    val clearLocalUserData: ClearLocalUserDataUseCase = koinInject()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        vm.snackbarEvents.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = strings.navigation.settings,
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = strings.settings.subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        LanguageSelectorSection(
            currentLanguage = appLanguageState.currentLanguage,
            supportedLanguages = appLanguageState.supportedLanguages,
            onLanguageSelected = appLanguageVm::onLanguageSelected,
            enabled = !state.isBusy,
        )

        AvatarPickerButton(enabled = !state.isBusy) { uriString, mimeType ->
            vm.uploadAvatarAndSave(uriString, mimeType)
        }

        Button(
            onClick = { vm.refresh() },
            enabled = !state.isBusy,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.isRefreshing) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
                Text(strings.settings.refreshProfile)
            }
        }

        Button(
            onClick = {
                scope.launch {
                    runCatching { sessionVm.signOut() }
                    runCatching { clearLocalUserData() }
                }
            },
            enabled = !state.isBusy,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(strings.settings.signOut)
        }

        state.error?.let {
            Text(text = it, color = MaterialTheme.colorScheme.error)
        }
    }

    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
    }
}
