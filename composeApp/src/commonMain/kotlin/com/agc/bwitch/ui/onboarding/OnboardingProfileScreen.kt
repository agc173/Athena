package com.agc.bwitch.ui.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.agc.bwitch.domain.userprofile.UsernameRules
import com.agc.bwitch.localization.appStrings
import com.agc.bwitch.presentation.localization.AppLanguageViewModel
import com.agc.bwitch.presentation.userprofile.ONBOARDING_AVATAR_UPDATED_MESSAGE_KEY
import com.agc.bwitch.presentation.userprofile.ONBOARDING_AVATAR_UPLOAD_ERROR_KEY
import com.agc.bwitch.presentation.userprofile.ONBOARDING_BIRTH_DATE_INVALID_ERROR_KEY
import com.agc.bwitch.presentation.userprofile.ONBOARDING_PROFILE_LOAD_ERROR_KEY
import com.agc.bwitch.presentation.userprofile.ONBOARDING_PROFILE_SAVE_ERROR_KEY
import com.agc.bwitch.presentation.userprofile.ONBOARDING_USERNAME_INVALID_ERROR_KEY
import com.agc.bwitch.presentation.userprofile.ONBOARDING_USERNAME_REQUIRED_ERROR_KEY
import com.agc.bwitch.presentation.userprofile.OnboardingProfileViewModel
import com.agc.bwitch.ui.common.BirthDateSelector
import com.agc.bwitch.ui.localization.LanguageSelectorSection
import com.agc.bwitch.ui.userprofile.AvatarPickerButton
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import kotlinx.datetime.LocalDate
import org.koin.compose.koinInject

@Composable
fun OnboardingProfileScreen(contentPadding: PaddingValues) {
    val vm: OnboardingProfileViewModel = koinInject()
    val state by vm.uiState.collectAsState()
    val strings = appStrings.onboarding
    val appLanguageVm: AppLanguageViewModel = koinInject()
    val appLanguageState by appLanguageVm.uiState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var username by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf<LocalDate?>(null) }
    var touched by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        vm.snackbarEvents.collect { message ->
            snackbarHostState.showSnackbar(message.toOnboardingUiText(strings))
        }
    }

    LaunchedEffect(state.profile) {
        if (!touched) {
            username = state.profile?.username.orEmpty()
            birthDate = state.profile?.birthDate
        }
    }

    val normalizedUsername = UsernameRules.normalize(username).orEmpty()
    val usernameValid = normalizedUsername.isNotBlank() && UsernameRules.isValid(normalizedUsername)
    val birthDateValid = birthDate != null

    val canContinue = !state.isBusy && !state.isInitialLoading && usernameValid && birthDateValid
    val photoUrl = state.profile?.photoUrl

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = strings.profileTitle,
            style = MaterialTheme.typography.headlineSmall,
        )
        Text(
            text = strings.profileSubtitle,
            style = MaterialTheme.typography.bodyMedium
        )

        if (!state.authDisplayName.isNullOrBlank() || !state.authEmail.isNullOrBlank()) {
            Text(
                text = listOfNotNull(state.authDisplayName, state.authEmail).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall
            )
        }

        LanguageSelectorSection(
            currentLanguage = appLanguageState.currentLanguage,
            supportedLanguages = appLanguageState.supportedLanguages,
            onLanguageSelected = appLanguageVm::onLanguageSelected,
            enabled = !state.isBusy,
        )

        if (!photoUrl.isNullOrBlank()) {
            KamelImage(
                resource = asyncPainterResource(photoUrl),
                contentDescription = strings.profileAvatarContentDescription,
                modifier = Modifier
                    .size(96.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        }

        OutlinedTextField(
            value = username,
            onValueChange = {
                touched = true
                username = it
            },
            enabled = !state.isBusy,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(strings.usernameLabel) },
            isError = touched && !usernameValid,
            supportingText = {
                if (touched && !usernameValid) {
                    Text(strings.usernameError)
                }
            }
        )

        BirthDateSelector(
            selectedDate = birthDate,
            onDateSelected = {
                touched = true
                birthDate = it
            },
            label = strings.birthDateLabel,
            enabled = !state.isBusy,
            modifier = Modifier.fillMaxWidth(),
            isError = touched && !birthDateValid,
            supportingText = if (touched && !birthDateValid) strings.birthDateFormatError else null,
        )

        AvatarPickerButton(
            enabled = !state.isBusy,
            buttonText = strings.selectAvatarButton,
        ) { uriString, mimeType ->
            vm.uploadAvatarAndSave(uriString, mimeType)
        }

        Button(
            onClick = {
                touched = true
                vm.completeOnboarding(usernameText = normalizedUsername, birthDateText = birthDate?.toString().orEmpty())
            },
            enabled = canContinue,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.isSaving) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
                Text(strings.continueButton)
            }
        }

        state.error?.let {
            Text(text = it.toOnboardingUiText(strings), color = MaterialTheme.colorScheme.error)
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

private fun String.toOnboardingUiText(strings: com.agc.bwitch.localization.OnboardingStrings): String {
    return when (this) {
        ONBOARDING_USERNAME_REQUIRED_ERROR_KEY -> strings.usernameRequiredError
        ONBOARDING_USERNAME_INVALID_ERROR_KEY -> strings.usernameError
        ONBOARDING_BIRTH_DATE_INVALID_ERROR_KEY -> strings.birthDateFormatError
        ONBOARDING_PROFILE_LOAD_ERROR_KEY -> strings.profileLoadError
        ONBOARDING_PROFILE_SAVE_ERROR_KEY -> strings.profileSaveError
        ONBOARDING_AVATAR_UPDATED_MESSAGE_KEY -> strings.avatarUpdatedMessage
        ONBOARDING_AVATAR_UPLOAD_ERROR_KEY -> strings.avatarUploadError
        else -> this
    }
}
