package com.agc.bwitch.ui.portal

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.material3.ExperimentalMaterial3Api
import com.agc.bwitch.ui.common.AppScaffold



@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PortalScreen(
    onOpenDailyHoroscope: () -> Unit
) {
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

            PortalSection(title = "Astrología") {
                PortalItem(
                    title = "Horóscopo diario",
                    subtitle = "Tu guía del día según tu signo",
                    onClick = onOpenDailyHoroscope
                )
            }

            PortalSection(title = "Próximamente") {
                PortalItem(
                    title = "Tarot",
                    subtitle = "Tiradas y lecturas",
                    onClick = { /* no-op */ },
                    enabled = false
                )
                PortalItem(
                    title = "Luna",
                    subtitle = "Fases y rituales",
                    onClick = { /* no-op */ },
                    enabled = false
                )
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
    Card(
        onClick = onClick,
        enabled = enabled
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
