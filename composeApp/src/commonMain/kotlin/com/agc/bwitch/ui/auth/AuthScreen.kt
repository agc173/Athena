package com.agc.bwitch.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.agc.bwitch.localization.appStrings
import com.agc.bwitch.platform.rememberPlatformContext
import com.agc.bwitch.presentation.auth.GoogleIdTokenProvider
import com.agc.bwitch.presentation.auth.SessionViewModel
import com.agc.bwitch.ui.common.designsystem.BWitchPrimaryButton
import com.agc.bwitch.ui.common.designsystem.BWitchSecondaryButton
import com.agc.bwitch.ui.common.designsystem.BWitchTextField
import com.agc.bwitch.ui.theme.BWitchThemeTokens
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

@Composable
fun AuthScreen(
    modifier: Modifier = Modifier,
    viewModel: SessionViewModel = koinInject()
) {
    val dimens = BWitchThemeTokens.dimens
    val extras = BWitchThemeTokens.extras
    val state by viewModel.uiState.collectAsState()
    val strings = appStrings.auth
    val commonStrings = appStrings.common

    val context = rememberPlatformContext()
    val googleProvider: GoogleIdTokenProvider = koinInject { parametersOf(context) }
    val scope = rememberCoroutineScope()

    var localError by remember { mutableStateOf<String?>(null) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    LaunchedEffect(state.isLoggedIn) {
        if (state.isLoggedIn) isSubmitting = false
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(dimens.spacingMd),
        verticalArrangement = Arrangement.spacedBy(dimens.spacingSm + dimens.spacingXs)
    ) {
        Text(commonStrings.appName, style = MaterialTheme.typography.headlineMedium)
        Text(
            strings.subtitle,
            color = extras.textSecondary,
            style = MaterialTheme.typography.bodyMedium
        )

        BWitchSecondaryButton(
            onClick = {
                isSubmitting = true
                localError = null
                scope.launch {
                    runCatching { googleProvider.getIdToken() }
                        .onSuccess { token -> viewModel.signInWithGoogle(token) }
                        .onFailure { _ ->
                            isSubmitting = false
                            localError = strings.googleSignInErrorFallback
                        }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading && !isSubmitting
        ) {
            Text(strings.continueWithGoogle)
        }

        localError?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        BWitchTextField(
            value = email,
            onValueChange = {
                email = it
                isSubmitting = false
                localError = null
            },
            label = { Text(strings.emailLabel) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        BWitchTextField(
            value = password,
            onValueChange = {
                password = it
                isSubmitting = false
                localError = null
            },
            label = { Text(strings.passwordLabel) },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dimens.spacingSm + dimens.spacingXs)
        ) {
            BWitchPrimaryButton(
                onClick = {
                    isSubmitting = true
                    localError = null
                    viewModel.signInWithEmail(email.trim(), password)
                },
                enabled = !state.isLoading && !isSubmitting && email.isNotBlank() && password.isNotBlank(),
                modifier = Modifier.weight(1f)
            ) {
                Text(strings.signIn)
            }

            BWitchSecondaryButton(
                onClick = {
                    isSubmitting = true
                    localError = null
                    viewModel.signUpWithEmail(email.trim(), password)
                },
                enabled = !state.isLoading && !isSubmitting && email.isNotBlank() && password.isNotBlank(),
                modifier = Modifier.weight(1f)
            ) {
                Text(strings.signUp)
            }
        }

        if (state.isLoading || isSubmitting) {
            Row(horizontalArrangement = Arrangement.spacedBy(dimens.spacingSm)) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                Text(strings.connecting, color = extras.textSecondary)
            }
        }

        state.error?.let {
            Text(strings.authErrorFallback, color = MaterialTheme.colorScheme.error)
            LaunchedEffect(it) { isSubmitting = false }
        }
    }
}
