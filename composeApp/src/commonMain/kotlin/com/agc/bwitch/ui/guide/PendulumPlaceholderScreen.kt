package com.agc.bwitch.ui.guide

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun PendulumPlaceholderScreen(contentPadding: PaddingValues) {
    Column(
        modifier = Modifier
            .padding(contentPadding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text("El Péndulo", style = MaterialTheme.typography.headlineMedium)
        Text("Próximamente", style = MaterialTheme.typography.titleMedium)
        Text(
            "Esta entrada queda preparada para el siguiente bloque del módulo Guía.",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}
