package com.agc.bwitch.ui.guide

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.agc.bwitch.presentation.navigation.Destination
import com.agc.bwitch.ui.theme.BWitchThemeTokens

@Composable
fun GuideHomeScreen(
    contentPadding: PaddingValues,
    onNavigate: (Destination) -> Unit,
) {
    val dimens = BWitchThemeTokens.dimens
    val extras = BWitchThemeTokens.extras

    Column(
        modifier = Modifier
            .padding(contentPadding)
            .padding(dimens.spacingMd),
        verticalArrangement = Arrangement.spacedBy(dimens.spacingSm + dimens.spacingXs),
    ) {
        Text("Guía", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Busca orientación para lo que necesitas saber hoy",
            style = MaterialTheme.typography.bodyMedium,
            color = extras.textSecondary,
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
    val dimens = BWitchThemeTokens.dimens

    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
            contentColor = MaterialTheme.colorScheme.onSurface
        )
    ) {
        Column(
            modifier = Modifier.padding(dimens.spacingMd),
            verticalArrangement = Arrangement.spacedBy(dimens.spacingXs),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (!details.isNullOrBlank()) {
                Text(details, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
