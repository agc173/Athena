package com.agc.bwitch.ui.userprofile

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.agc.bwitch.presentation.userprofile.UserProfileViewModel
import org.koin.compose.koinInject
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.painter.Painter
import io.kamel.core.Resource
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource

@Composable
fun UserProfileScreen(
    contentPadding: PaddingValues,
    onBack: () -> Unit
) {
    val vm: UserProfileViewModel = koinInject()
    val state by vm.uiState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    // ✅ Escucha eventos y muestra snackbar
    LaunchedEffect(Unit) {
        vm.snackbarEvents.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    var displayName by remember(state.profile) { mutableStateOf(state.profile?.displayName.orEmpty()) }
    var photoUrl by remember(state.profile) { mutableStateOf(state.profile?.photoUrl.orEmpty()) }
    var email by remember(state.profile) { mutableStateOf(state.profile?.email.orEmpty()) }

    Box(
        modifier = Modifier
            .padding(contentPadding)
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Perfil")

            val avatarUrl = state.profile?.photoUrl

            if (!avatarUrl.isNullOrBlank()) {
                KamelImage(
                    resource = asyncPainterResource(data = avatarUrl),
                    contentDescription = "Avatar",
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape),
                    onLoading = {
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    },
                    onFailure = { error ->
                        // Nunca crashear: mostramos placeholder
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("👤")
                        }
                        println("Kamel avatar load failed: ${error.message}")
                    }
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Text("👤")
                }
            }

            OutlinedTextField(
                value = displayName,
                onValueChange = { displayName = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Nombre") }
            )

            OutlinedTextField(
                value = photoUrl,
                onValueChange = { photoUrl = it },
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

            AvatarPickerButton { uriString, mimeType ->
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
                enabled = !state.isLoading
            ) {
                Text("Guardar")
            }

            Button(onClick = onBack) { Text("Volver") }

            // Opcional: si quieres mantener el error también en pantalla
            state.error?.let { Text("Error: $it") }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(16.dp)
        )
    }
}