package com.agc.bwitch.ui.astrology

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.agc.bwitch.presentation.navigation.Destination
import com.agc.bwitch.ui.theme.BWitchThemeTokens

@Composable
fun AstrologyScreen(
    contentPadding: PaddingValues,
    onNavigate: (Destination) -> Unit
) {
    val dimens = BWitchThemeTokens.dimens
    val extras = BWitchThemeTokens.extras

    Column(
        modifier = Modifier
            .padding(contentPadding)
            .padding(dimens.spacingMd),
        verticalArrangement = Arrangement.spacedBy(dimens.spacingSm + dimens.spacingXs)
    ) {
        Text(
            "Elige una sección",
            style = MaterialTheme.typography.bodyMedium,
            color = extras.textSecondary
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
    val dimens = BWitchThemeTokens.dimens
    val colors = MaterialTheme.colorScheme

    Card(
        onClick = onClick,
        enabled = enabled,
        colors = CardDefaults.cardColors(
            containerColor = colors.surfaceVariant,
            contentColor = colors.onSurface
        )
    ) {
        Column(
            modifier = Modifier.padding(dimens.spacingMd),
            verticalArrangement = Arrangement.spacedBy(dimens.spacingXs)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = colors.onSurfaceVariant)
        }
    }
}
