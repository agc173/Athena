package com.agc.bwitch.ui.userprofile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import org.koin.compose.koinInject

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

    var displayName by remember(state.profile) { mutableStateOf(state.profile?.displayName.orEmpty()) }
    var photoUrl by remember(state.profile) { mutableStateOf(state.profile?.photoUrl.orEmpty()) }
    var email by remember(state.profile) { mutableStateOf(state.profile?.email.orEmpty()) }

    val isBusy = state.isSaving || state.isUploadingAvatar
    val inputsEnabled = !state.isInitialLoading && !isBusy
    val avatarUrl = photoUrl.trim().takeIf { it.isNotBlank() }

    val overlayMessage = when {
        state.isUploadingAvatar -> "Subiendo avatar…"
        state.isSaving -> "Guardando…"
        state.isInitialLoading -> "Cargando perfil…"
        else -> null
    }

    Box(
        modifier = Modifier
            .padding(contentPadding)
            .fillMaxSize()
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Perfil")

            // Avatar
            if (avatarUrl != null) {
                KamelImage(
                    resource = asyncPainterResource(avatarUrl),
                    contentDescription = "Avatar",
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                    onLoading = { AvatarPlaceholder(isLoading = true) },
                    onFailure = { error ->
                        println("Kamel avatar load failed: ${error.message}")
                        error.printStackTrace()
                        AvatarPlaceholder(isLoading = false)
                    }
                )
            } else {
                AvatarPlaceholder(isLoading = false)
            }

            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                enabled = inputsEnabled,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Nombre") }
            )

            OutlinedTextField(
                value = photoUrl,
                onValueChange = { photoUrl = it },
                enabled = inputsEnabled,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Photo URL") }
            )

            OutlinedTextField(
                value = email,
                onValueChange = {},
                enabled = false,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Email") }
            )

            AvatarPickerButton(
                enabled = inputsEnabled
            ) { uriString, mimeType ->
                vm.uploadAvatarAndSave(uriString, mimeType)
            }

            Button(
                onClick = {
                    vm.updateAndSave(
                        displayName = displayName.ifBlank { null },
                        photoUrl = photoUrl.ifBlank { null },
                        email = email.ifBlank { null }
                    )
                },
                enabled = !state.isInitialLoading && !isBusy
            ) {
                Text(if (state.isSaving) "Guardando..." else "Guardar")
            }

            Button(
                onClick = { vm.refresh() },
                enabled = !state.isInitialLoading && !state.isBusy
            ) {
                Text(if (state.isRefreshing) "Refrescando..." else "Refrescar")
            }

            Button(
                onClick = onBack,
                enabled = !isBusy
            ) { Text("Volver") }

            state.error?.let { Text("Error: $it") }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )

        // Overlay global con mensaje
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
private fun AvatarPlaceholder(isLoading: Boolean) {
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
            Text("👤", textAlign = TextAlign.Center)
        }
    }
}