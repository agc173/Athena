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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.agc.bwitch.presentation.userprofile.OnboardingProfileViewModel
import com.agc.bwitch.domain.userprofile.UsernameRules
import com.agc.bwitch.ui.userprofile.AvatarPickerButton
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import kotlinx.datetime.LocalDate
import org.koin.compose.koinInject

@Composable
fun OnboardingProfileScreen(contentPadding: PaddingValues) {
    val vm: OnboardingProfileViewModel = koinInject()
    val state by vm.uiState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }
    var username by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }
    var touched by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        vm.snackbarEvents.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    LaunchedEffect(state.profile) {
        if (!touched) {
            username = state.profile?.username.orEmpty()
            birthDate = state.profile?.birthDate?.toString().orEmpty()
        }
    }

    val normalizedUsername = UsernameRules.normalize(username).orEmpty()
    val usernameValid = normalizedUsername.isNotBlank() && UsernameRules.isValid(normalizedUsername)
    val birthDateParsed = runCatching { LocalDate.parse(birthDate.trim()) }.getOrNull()
    val birthDateValid = birthDateParsed != null

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
        Text("Completa tu perfil", style = MaterialTheme.typography.headlineSmall)
        Text(
            "Necesitamos tu username y fecha de nacimiento para personalizar tu experiencia.",
            style = MaterialTheme.typography.bodyMedium
        )

        if (!state.authDisplayName.isNullOrBlank() || !state.authEmail.isNullOrBlank()) {
            Text(
                text = listOfNotNull(state.authDisplayName, state.authEmail).joinToString(" · "),
                style = MaterialTheme.typography.bodySmall
            )
        }

        if (!photoUrl.isNullOrBlank()) {
            KamelImage(
                resource = asyncPainterResource(photoUrl),
                contentDescription = "Avatar",
                modifier = Modifier.size(96.dp),
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
            label = { Text("Username *") },
            isError = touched && !usernameValid,
            supportingText = {
                if (touched && !usernameValid) {
                    Text("Usa 3-30 caracteres: letras, números, punto o guion bajo")
                }
            }
        )

        OutlinedTextField(
            value = birthDate,
            onValueChange = {
                touched = true
                birthDate = it
            },
            enabled = !state.isBusy,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Fecha de nacimiento *") },
            placeholder = { Text("YYYY-MM-DD") },
            isError = touched && !birthDateValid,
            supportingText = {
                if (touched && !birthDateValid) {
                    Text("Formato esperado: YYYY-MM-DD")
                }
            }
        )

        AvatarPickerButton(enabled = !state.isBusy) { uriString, mimeType ->
            vm.uploadAvatarAndSave(uriString, mimeType)
        }

        Button(
            onClick = {
                touched = true
                vm.completeOnboarding(usernameText = normalizedUsername, birthDateText = birthDate.trim())
            },
            enabled = canContinue,
            modifier = Modifier.fillMaxWidth()
        ) {
            if (state.isSaving) {
                CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
            } else {
                Text("Guardar y continuar")
            }
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
