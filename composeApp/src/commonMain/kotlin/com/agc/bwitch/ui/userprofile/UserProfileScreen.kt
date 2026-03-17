package com.agc.bwitch.ui.userprofile

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.agc.bwitch.presentation.userprofile.UserProfileViewModel
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import kotlinx.datetime.LocalDate
import org.koin.compose.koinInject

private const val MAX_NAME_LEN = 40
private const val MAX_USERNAME_LEN = 30

@Composable
fun UserProfileScreen(
    contentPadding: PaddingValues,
    onBack: () -> Unit
) {
    val vm: UserProfileViewModel = koinInject()
    val state by vm.uiState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        vm.snackbarEvents.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    var isDirty by remember { mutableStateOf(false) }
    var isEditing by remember { mutableStateOf(false) }
    var saveRequested by remember { mutableStateOf(false) }
    var saveInProgressAfterRequest by remember { mutableStateOf(false) }

    var displayName by remember { mutableStateOf("") }
    var photoUrl by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var birthDate by remember { mutableStateOf("") }

    val isBusy = state.isSaving || state.isUploadingAvatar || state.isRefreshing
    val inputsEnabled = !state.isInitialLoading && !isBusy

    val trimmedName = displayName.trim()
    val trimmedUrl = photoUrl.trim()
    val trimmedUsername = username.trim().removePrefix("@")
    val trimmedBirthDate = birthDate.trim()

    val nameTooLong = trimmedName.length > MAX_NAME_LEN
    val usernameTooLong = trimmedUsername.length > MAX_USERNAME_LEN
    val birthDateLooksValid = trimmedBirthDate.isBlank() || runCatching {
        LocalDate.parse(trimmedBirthDate)
    }.isSuccess

    val normalizedName = if (nameTooLong) trimmedName.take(MAX_NAME_LEN) else trimmedName
    val normalizedUsername = if (usernameTooLong) trimmedUsername.take(MAX_USERNAME_LEN) else trimmedUsername

    val urlLooksValid = trimmedUrl.isBlank() ||
            trimmedUrl.startsWith("https://", ignoreCase = true) ||
            trimmedUrl.startsWith("http://", ignoreCase = true)

    val canSave = !state.isInitialLoading &&
            !isBusy &&
            !nameTooLong &&
            !usernameTooLong &&
            urlLooksValid &&
            birthDateLooksValid

    val avatarUrl = trimmedUrl.takeIf { it.isNotBlank() }

    val overlayMessage = when {
        state.isUploadingAvatar -> "Subiendo avatar…"
        state.isSaving -> "Guardando…"
        state.isRefreshing -> "Actualizando…"
        state.isInitialLoading -> "Cargando perfil…"
        else -> null
    }

    LaunchedEffect(state.profile) {
        if (!isDirty) {
            displayName = state.profile?.displayName.orEmpty()
            photoUrl = state.profile?.photoUrl.orEmpty()
            email = state.profile?.email.orEmpty()
            username = state.profile?.username.orEmpty()
            birthDate = state.profile?.birthDate?.toString().orEmpty()
        }
    }

    LaunchedEffect(state.isSaving, state.error, saveRequested) {
        if (!saveRequested) return@LaunchedEffect

        if (state.isSaving) {
            saveInProgressAfterRequest = true
            return@LaunchedEffect
        }

        if (!saveInProgressAfterRequest) return@LaunchedEffect

        if (state.error == null) {
            isDirty = false
            isEditing = false
        }

        saveRequested = false
        saveInProgressAfterRequest = false
    }

    Box(
        modifier = Modifier
            .padding(contentPadding)
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Perfil")

            val canLoadRemoteAvatar = avatarUrl != null &&
                    (avatarUrl.startsWith("https://", ignoreCase = true) || avatarUrl.startsWith("http://", ignoreCase = true))

            if (canLoadRemoteAvatar) {
                KamelImage(
                    resource = asyncPainterResource(avatarUrl!!),
                    contentDescription = "Avatar",
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                    onLoading = { AvatarPlaceholder(isLoading = true, displayName = normalizedName) },
                    onFailure = {
                        AvatarPlaceholder(isLoading = false, displayName = normalizedName)
                    }
                )
            } else {
                AvatarPlaceholder(isLoading = false, displayName = normalizedName)
            }

            if (isEditing) {
                OutlinedTextField(
                    value = displayName,
                    onValueChange = {
                        displayName = it
                        isDirty = true
                    },
                    enabled = inputsEnabled,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Nombre") },
                    isError = nameTooLong,
                    supportingText = {
                        val count = trimmedName.length
                        val text = if (nameTooLong) {
                            "Máximo $MAX_NAME_LEN caracteres ($count)"
                        } else {
                            "$count / $MAX_NAME_LEN"
                        }
                        Text(text)
                    }
                )

                OutlinedTextField(
                    value = username,
                    onValueChange = {
                        username = it
                        isDirty = true
                    },
                    enabled = inputsEnabled,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Username (@)") },
                    isError = usernameTooLong,
                    supportingText = {
                        val count = trimmedUsername.length
                        val text = if (usernameTooLong) {
                            "Máximo $MAX_USERNAME_LEN caracteres ($count)"
                        } else {
                            "$count / $MAX_USERNAME_LEN"
                        }
                        Text(text)
                    }
                )

                OutlinedTextField(
                    value = birthDate,
                    onValueChange = {
                        birthDate = it
                        isDirty = true
                    },
                    enabled = inputsEnabled,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Fecha de nacimiento") },
                    placeholder = { Text("YYYY-MM-DD") },
                    isError = !birthDateLooksValid,
                    supportingText = {
                        if (!birthDateLooksValid) {
                            Text("Formato esperado: YYYY-MM-DD")
                        }
                    }
                )

                Box(modifier = Modifier.fillMaxWidth()) {
                    if (inputsEnabled) {
                        AvatarPickerButton(enabled = inputsEnabled) { uriString, mimeType ->
                            vm.uploadAvatarAndSave(uriString, mimeType)
                        }
                    } else {
                        Button(
                            onClick = {},
                            enabled = false,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            if (state.isUploadingAvatar) SmallSpinnerWithText("Subiendo…") else Text("Seleccionar avatar")
                        }
                    }
                }

                Button(
                    onClick = {
                        saveRequested = true
                        saveInProgressAfterRequest = false
                        vm.updateAndSave(
                            displayName = normalizedName.ifBlank { null },
                            photoUrl = trimmedUrl.ifBlank { null },
                            email = email.trim().ifBlank { null },
                            username = normalizedUsername.ifBlank { null },
                            birthDateText = trimmedBirthDate.ifBlank { null }
                        )
                    },
                    enabled = canSave,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (state.isSaving) SmallSpinnerWithText("Guardando…") else Text("Guardar")
                }

                Button(
                    onClick = {
                        isDirty = false
                        displayName = state.profile?.displayName.orEmpty()
                        photoUrl = state.profile?.photoUrl.orEmpty()
                        email = state.profile?.email.orEmpty()
                        username = state.profile?.username.orEmpty()
                        birthDate = state.profile?.birthDate?.toString().orEmpty()
                        isEditing = false
                    },
                    enabled = !isBusy,
                    modifier = Modifier.fillMaxWidth()
                ) { Text("Cancelar") }
            } else {
                Text("@${trimmedUsername.ifBlank { "sin_username" }}", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = if (trimmedName.isBlank()) "Sin nombre" else trimmedName,
                    style = MaterialTheme.typography.headlineSmall
                )
                Text("Email: ${email.ifBlank { "No disponible" }}")
                Text("Fecha de nacimiento: ${birthDate.ifBlank { "No disponible" }}")
                state.profile?.zodiacSign?.let {
                    Text("Signo zodiacal: ${it.label}")
                }

                Button(
                    onClick = { isEditing = true },
                    enabled = !isBusy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Editar perfil")
                }
            }

            Button(
                onClick = { vm.refresh() },
                enabled = !state.isInitialLoading && !isBusy,
                modifier = Modifier.fillMaxWidth()
            ) {
                if (state.isRefreshing) SmallSpinnerWithText("Refrescando…") else Text("Refrescar")
            }

            Button(
                onClick = onBack,
                enabled = !isBusy,
                modifier = Modifier.fillMaxWidth()
            ) { Text("Volver") }

            state.error?.let { Text("Error: $it", color = MaterialTheme.colorScheme.error) }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )

        if (overlayMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.35f)),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CircularProgressIndicator()
                    Text(overlayMessage)
                }
            }
        }
    }
}

@Composable
private fun SmallSpinnerWithText(text: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
        Text(text)
    }
}

@Composable
private fun AvatarPlaceholder(
    isLoading: Boolean,
    displayName: String
) {
    val initial = displayName.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "👤"

    Box(
        modifier = Modifier
            .size(96.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator()
        } else {
            Text(
                initial,
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.headlineMedium
            )
        }
    }
}
