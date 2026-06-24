package com.agc.bwitch.ui.auth

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.agc.bwitch.localization.appStrings
import com.agc.bwitch.domain.security.InputPolicy
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
    var localMessage by remember { mutableStateOf<String?>(null) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isSubmitting by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(state.isLoggedIn) {
        if (state.isLoggedIn) isSubmitting = false
    }

    fun clearLocalFeedback() {
        isSubmitting = false
        localError = null
        localMessage = null
    }

    fun validateEmail(): Boolean {
        val normalizedEmail = InputPolicy.normalizeSingleLineInput(email, InputPolicy.EMAIL_MAX_LENGTH)
        val isValid = normalizedEmail.isValidEmail() && InputPolicy.isEmailLengthValid(normalizedEmail)
        localError = if (isValid) null else strings.invalidEmailError
        return isValid
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
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
                localMessage = null
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
            Row(
                horizontalArrangement = Arrangement.spacedBy(dimens.spacingSm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                GoogleIcon(modifier = Modifier.size(18.dp))
                Text(strings.continueWithGoogle)
            }
        }

        BWitchSecondaryButton(
            onClick = {},
            modifier = Modifier.fillMaxWidth(),
            enabled = false,
        ) {
            Text("${strings.continueWithApple} · ${strings.appleComingSoon}")
        }

        Text(
            strings.emailSignInHeading,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
        )

        localError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        localMessage?.let { Text(it, color = MaterialTheme.colorScheme.primary) }

        BWitchTextField(
            value = email,
            onValueChange = {
                email = InputPolicy.normalizeSingleLineInput(it, InputPolicy.EMAIL_MAX_LENGTH)
                clearLocalFeedback()
            },
            label = { Text(strings.emailLabel) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        BWitchTextField(
            value = password,
            onValueChange = {
                password = it
                clearLocalFeedback()
            },
            label = { Text(strings.passwordLabel) },
            singleLine = true,
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            trailingIcon = {
                val toggleDescription = if (passwordVisible) strings.hidePassword else strings.showPassword
                IconButton(
                    onClick = { passwordVisible = !passwordVisible },
                    modifier = Modifier.semantics { contentDescription = toggleDescription },
                ) {
                    PasswordVisibilityIcon(
                        visible = passwordVisible,
                        modifier = Modifier.size(20.dp),
                    )
                }
            },
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dimens.spacingSm + dimens.spacingXs)
        ) {
            BWitchPrimaryButton(
                onClick = {
                    if (validateEmail()) {
                        isSubmitting = true
                        localMessage = null
                        viewModel.signInWithEmail(email.trim(), password)
                    }
                },
                enabled = !state.isLoading && !isSubmitting && email.isNotBlank() && password.isNotBlank(),
                modifier = Modifier.weight(1f)
            ) {
                Text(strings.signIn)
            }

            BWitchSecondaryButton(
                onClick = {
                    if (validateEmail()) {
                        isSubmitting = true
                        localMessage = null
                        viewModel.signUpWithEmail(email.trim(), password)
                    }
                },
                enabled = !state.isLoading && !isSubmitting && email.isNotBlank() && password.isNotBlank(),
                modifier = Modifier.weight(1f)
            ) {
                Text(strings.signUp)
            }
        }

        TextButton(
            onClick = {
                if (validateEmail()) {
                    isSubmitting = true
                    localMessage = null
                    viewModel.sendPasswordResetEmail(email.trim()) { success ->
                        isSubmitting = false
                        if (success) {
                            localError = null
                            localMessage = strings.passwordResetSent
                        } else {
                            localError = strings.passwordResetErrorFallback
                        }
                    }
                }
            },
            enabled = !state.isLoading && !isSubmitting && email.isNotBlank(),
        ) {
            Text(strings.forgotPassword)
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

@Composable
private fun PasswordVisibilityIcon(
    visible: Boolean,
    modifier: Modifier = Modifier,
) {
    val tint = MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(modifier = modifier) {
        val strokeWidth = size.minDimension * 0.09f
        val center = Offset(size.width / 2f, size.height / 2f)
        val eyeWidth = size.width * 0.78f
        val eyeHeight = size.height * 0.46f

        drawOval(
            color = tint,
            topLeft = Offset(center.x - eyeWidth / 2f, center.y - eyeHeight / 2f),
            size = androidx.compose.ui.geometry.Size(eyeWidth, eyeHeight),
            style = Stroke(width = strokeWidth),
        )
        drawCircle(
            color = tint,
            radius = size.minDimension * 0.12f,
            center = center,
            style = if (visible) androidx.compose.ui.graphics.drawscope.Fill else Stroke(width = strokeWidth),
        )
        if (!visible) {
            drawLine(
                color = tint,
                start = Offset(size.width * 0.18f, size.height * 0.82f),
                end = Offset(size.width * 0.82f, size.height * 0.18f),
                strokeWidth = strokeWidth,
                cap = StrokeCap.Round,
            )
        }
    }
}

@Composable
private fun GoogleIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        val strokeWidth = size.minDimension * 0.16f
        val radius = size.minDimension * 0.34f
        val center = Offset(size.width / 2f, size.height / 2f)
        drawArc(
            color = Color(0xFF4285F4),
            startAngle = -35f,
            sweepAngle = 115f,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2f),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )
        drawArc(
            color = Color(0xFF34A853),
            startAngle = 80f,
            sweepAngle = 100f,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2f),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )
        drawArc(
            color = Color(0xFFFBBC05),
            startAngle = 180f,
            sweepAngle = 80f,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2f),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )
        drawArc(
            color = Color(0xFFEA4335),
            startAngle = 260f,
            sweepAngle = 65f,
            useCenter = false,
            topLeft = Offset(center.x - radius, center.y - radius),
            size = androidx.compose.ui.geometry.Size(radius * 2f, radius * 2f),
            style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
        )
        drawLine(
            color = Color(0xFF4285F4),
            start = Offset(center.x, center.y),
            end = Offset(size.width * 0.88f, center.y),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
    }
}

private fun String.isValidEmail(): Boolean {
    return matches(Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"))
}
