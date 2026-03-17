package com.agc.bwitch.ui.tarot

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
import com.agc.bwitch.domain.tarot.TarotRequestType

@Composable
fun TarotHomeScreen(
    contentPadding: PaddingValues,
    onSelectRequestType: (TarotRequestType) -> Unit,
) {
    Column(
        modifier = Modifier
            .padding(contentPadding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            "Las cartas ofrecen una lectura simbólica para tu momento actual",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        TarotOptionCard(
            title = "Carta única",
            subtitle = "Una guía clara para tu situación actual",
            onClick = { onSelectRequestType(TarotRequestType.TAROT_1) },
        )

        TarotOptionCard(
            title = "Tirada de 3 cartas",
            subtitle = "Pasado · Presente · Futuro",
            onClick = { onSelectRequestType(TarotRequestType.TAROT_3) },
        )
    }
}

@Composable
private fun TarotOptionCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Card(onClick = onClick) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
