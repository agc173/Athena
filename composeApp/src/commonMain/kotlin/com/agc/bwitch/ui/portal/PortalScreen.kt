package com.agc.bwitch.ui.portal

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.agc.bwitch.presentation.navigation.Destination
import com.agc.bwitch.ui.common.AppScaffold

@Composable
fun PortalScreen(
    onNavigate: (Destination) -> Unit
) {
    val items = listOf(
        PortalItemConfig(
            title = "Horóscopo diario",
            subtitle = "Tu guía del día según tu signo",
            destination = Destination.HoroscopeDaily,
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
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Portal", style = MaterialTheme.typography.headlineSmall)
            Text("Elige un módulo", style = MaterialTheme.typography.bodyMedium)

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

