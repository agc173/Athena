package com.agc.bwitch.ui.guide

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.agc.bwitch.presentation.navigation.Destination
import com.agc.bwitch.ui.common.designsystem.BWitchCard
import com.agc.bwitch.ui.common.designsystem.BWitchScreen
import com.agc.bwitch.ui.common.designsystem.BWitchSectionHeader

@Composable
fun GuideHomeScreen(
    contentPadding: PaddingValues,
    onNavigate: (Destination) -> Unit,
) {
    BWitchScreen(contentPadding = contentPadding) {
        BWitchSectionHeader(
            title = "Explora tu intuición",
            subtitle = "Elige una práctica para lo que necesitas comprender hoy",
        )

        GuideOptionCard(
            title = "Tarot",
            subtitle = "Lecturas e interpretación simbólica",
            details = "Carta única · Tirada de 3",
            onClick = { onNavigate(Destination.TarotHome) },
        )

        GuideOptionCard(
            title = "Oráculo",
            subtitle = "Haz una pregunta y recibe guía",
            onClick = { onNavigate(Destination.Oracle) },
        )

        GuideOptionCard(
            title = "El Péndulo",
            subtitle = "Una respuesta rápida para tu pregunta",
            details = "Sí · No · Tal vez · Aún no",
            onClick = { onNavigate(Destination.Pendulum) },
        )
    }
}

@Composable
private fun GuideOptionCard(
    title: String,
    subtitle: String,
    details: String? = null,
    onClick: () -> Unit,
) {
    BWitchCard(onClick = onClick) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!details.isNullOrBlank()) {
            Text(
                details,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
