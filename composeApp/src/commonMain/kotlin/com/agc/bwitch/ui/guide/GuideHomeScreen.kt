package com.agc.bwitch.ui.guide

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.agc.bwitch.presentation.navigation.Destination

@Composable
fun GuideHomeScreen(
    contentPadding: PaddingValues,
    onNavigate: (Destination) -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(contentPadding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Guía", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Busca orientación para lo que necesitas saber hoy",
            style = MaterialTheme.typography.bodyMedium,
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
    Card(onClick = onClick) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium)
            if (!details.isNullOrBlank()) {
                Text(details, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}
