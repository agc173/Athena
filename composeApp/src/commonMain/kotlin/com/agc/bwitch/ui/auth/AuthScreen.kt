package com.agc.bwitch.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.agc.bwitch.platform.rememberPlatformContext
import com.agc.bwitch.presentation.auth.GoogleIdTokenProvider
import com.agc.bwitch.presentation.auth.SessionViewModel
import kotlinx.coroutines.launch
import org.koin.compose.koinInject
import org.koin.core.parameter.parametersOf

@Composable
fun AuthScreen(
    modifier: Modifier = Modifier,
    viewModel: SessionViewModel = koinInject()
) {
    val state by viewModel.uiState.collectAsState()

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
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("BWitch", style = MaterialTheme.typography.headlineMedium)
        Text("Inicia sesión para sincronizar tus datos.")

        OutlinedButton(
            onClick = {
                isSubmitting = true
                localError = null
                scope.launch {
                    runCatching { googleProvider.getIdToken() }
                        .onSuccess { token -> viewModel.signInWithGoogle(token) }
                        .onFailure { e ->
                            isSubmitting = false
                            localError = e.message ?: "Google Sign-In falló"
                        }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading && !isSubmitting
        ) {
            Text("Continuar con Google")
        }

        localError?.let { Text(it, color = MaterialTheme.colorScheme.error) }

        OutlinedTextField(
            value = email,
            onValueChange = {
                email = it
                isSubmitting = false
                localError = null
            },
            label = { Text("Email") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = password,
            onValueChange = {
                password = it
                isSubmitting = false
                localError = null
            },
            label = { Text("Contraseña") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Button(
                onClick = {
                    isSubmitting = true
                    localError = null
                    viewModel.signInWithEmail(email.trim(), password)
                },
                enabled = !state.isLoading && !isSubmitting && email.isNotBlank() && password.isNotBlank(),
                modifier = Modifier.weight(1f)
            ) {
                Text("Entrar")
            }

            OutlinedButton(
                onClick = {
                    isSubmitting = true
                    localError = null
                    viewModel.signUpWithEmail(email.trim(), password)
                },
                enabled = !state.isLoading && !isSubmitting && email.isNotBlank() && password.isNotBlank(),
                modifier = Modifier.weight(1f)
            ) {
                Text("Crear cuenta")
            }
        }

        if (state.isLoading || isSubmitting) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                CircularProgressIndicator(modifier = Modifier.size(18.dp))
                Text("Conectando…")
            }
        }

        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            LaunchedEffect(it) { isSubmitting = false }
        }
    }
}