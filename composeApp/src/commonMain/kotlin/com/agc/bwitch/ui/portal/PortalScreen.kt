package com.agc.bwitch.ui.portal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.agc.bwitch.presentation.auth.SessionViewModel
import com.agc.bwitch.presentation.navigation.Destination
import com.agc.bwitch.ui.common.AppScaffold
import org.koin.compose.koinInject
import com.agc.bwitch.domain.session.ClearLocalUserDataUseCase
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun PortalScreen(
    contentPadding: PaddingValues,
    onNavigate: (Destination) -> Unit
) {
    val sessionVm: SessionViewModel = koinInject()

    val clearLocalUserData: ClearLocalUserDataUseCase = koinInject()
    val scope = rememberCoroutineScope()

    val items = listOf(
        PortalItemConfig(
            title = "Astrología",
            subtitle = "Horóscopo, carta astral, compatibilidad…",
            destination = Destination.Astrology,
            enabled = true
        ),
        PortalItemConfig(
            title = "Perfil",
            subtitle = "Nombre, foto y sincronización",
            destination = Destination.UserProfile,
            enabled = true
        ),
        PortalItemConfig(
            title = "Tarot",
            subtitle = "Tiradas y lecturas",
            destination = null,
            enabled = false
        ),
        PortalItemConfig(
            title = "Luna",
            subtitle = "Fases y rituales",
            destination = null,
            enabled = false
        )
    )

    AppScaffold(
        title = "BWitch",
        canGoBack = false,
        onBack = {}
    ) { scaffoldPadding ->
        Column(
            modifier = Modifier
                .padding(scaffoldPadding)
                .padding(contentPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Portal", style = MaterialTheme.typography.headlineSmall)
                    Text("Elige un módulo", style = MaterialTheme.typography.bodyMedium)
                }

                Button(
                    onClick = {
                        scope.launch {
                            // Orden recomendado: signOut primero y limpiamos local sí o sí
                            runCatching { sessionVm.signOut() }
                            runCatching { clearLocalUserData() }
                        }
                    }
                ) {
                    Text("Cerrar sesión")
                }
            }

            PortalSection(title = "Módulos") {
                items.forEach { item ->
                    PortalItem(
                        title = item.title,
                        subtitle = item.subtitle,
                        enabled = item.enabled && item.destination != null,
                        onClick = {
                            val dest = item.destination ?: return@PortalItem
                            onNavigate(dest)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PortalSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        content()
    }
}

@Composable
private fun PortalItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    androidx.compose.material3.Card(
        onClick = onClick,
        enabled = enabled
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium)
        }
    }
}


