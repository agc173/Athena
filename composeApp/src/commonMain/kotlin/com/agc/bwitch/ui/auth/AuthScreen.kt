package com.agc.bwitch.ui.auth

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.agc.bwitch.presentation.auth.SessionViewModel
import org.koin.compose.koinInject

@Composable
fun AuthScreen(
    modifier: Modifier = Modifier,
    viewModel: SessionViewModel = koinInject()
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier.padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("BWitch", style = MaterialTheme.typography.headlineMedium)
        Text("Inicia sesión para sincronizar tus datos.")

        Button(
            onClick = viewModel::signInAnonymously,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Entrar como invitado")
        }

        if (state.isLoggedIn) {
            Button(
                onClick = viewModel::signOut,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Cerrar sesión")
            }
        }

        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
    }
}
