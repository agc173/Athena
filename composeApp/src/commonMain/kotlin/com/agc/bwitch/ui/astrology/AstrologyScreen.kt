package com.agc.bwitch.ui.astrology

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.agc.bwitch.presentation.navigation.Destination

@Composable
fun AstrologyScreen(
    contentPadding: PaddingValues,
    onNavigate: (Destination) -> Unit
) {
    Column(
        modifier = Modifier
            .padding(contentPadding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Elige una sección", style = MaterialTheme.typography.bodyMedium)

        FeatureItem(
            title = "Horóscopo diario",
            subtitle = "Tu guía del día según tu signo",
            enabled = true,
            onClick = { onNavigate(Destination.HoroscopeDaily()) }
        )

        FeatureItem(
            title = "Carta astral",
            subtitle = "Tu mapa natal (nacimiento)",
            enabled = false,
            onClick = { }
        )

        FeatureItem(
            title = "Compatibilidad",
            subtitle = "Sinastría básica entre dos personas",
            enabled = false,
            onClick = { }
        )

        FeatureItem(
            title = "Eventos astrológicos",
            subtitle = "Lunas, eclipses, retrogradaciones…",
            enabled = false,
            onClick = { }
        )

        FeatureItem(
            title = "Tránsitos",
            subtitle = "Influencia actual sobre tu carta natal",
            enabled = false,
            onClick = { }
        )
    }
}

@Composable
private fun FeatureItem(
    title: String,
    subtitle: String,
    enabled: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        enabled = enabled
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
