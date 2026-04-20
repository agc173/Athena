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
import com.agc.bwitch.localization.appStrings

@Composable
fun TarotHomeScreen(
    contentPadding: PaddingValues,
    onSelectRequestType: (TarotRequestType) -> Unit,
) {
    val strings = appStrings.tarot

    Column(
        modifier = Modifier
            .padding(contentPadding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            strings.homeIntro,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        TarotOptionCard(
            title = strings.homeSingleCardTitle,
            subtitle = strings.homeSingleCardSubtitle,
            onClick = { onSelectRequestType(TarotRequestType.TAROT_1) },
        )

        TarotOptionCard(
            title = strings.homeThreeCardTitle,
            subtitle = strings.homeThreeCardSubtitle,
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
