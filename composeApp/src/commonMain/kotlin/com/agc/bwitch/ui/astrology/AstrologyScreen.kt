package com.agc.bwitch.ui.astrology

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.agc.bwitch.presentation.navigation.Destination
import com.agc.bwitch.ui.common.designsystem.BWitchCard
import com.agc.bwitch.ui.common.designsystem.BWitchScreen
import com.agc.bwitch.ui.common.designsystem.BWitchSectionHeader

@Composable
fun AstrologyScreen(
    contentPadding: PaddingValues,
    onNavigate: (Destination) -> Unit
) {
    BWitchScreen(contentPadding = contentPadding) {
        BWitchSectionHeader(
            title = "Elige una sección",
            titleStyle = MaterialTheme.typography.bodyMedium,
        )

        FeatureItem(
            title = "Horóscopo diario",
            subtitle = "Tu guía del día según tu signo",
            enabled = true,
            onClick = { onNavigate(Destination.HoroscopeDaily()) }
        )

        FeatureItem(
            title = "Carta astral",
            subtitle = "Tu mapa natal (nacimiento)",
            enabled = true,
            onClick = { onNavigate(Destination.BirthChart) }
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
    BWitchCard(
        onClick = onClick,
        enabled = enabled,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
