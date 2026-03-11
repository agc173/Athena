package com.agc.bwitch.ui.tarot.components

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.agc.bwitch.domain.tarot.TarotCard

@Composable
fun TarotMiniCard(
    card: TarotCard,
    label: String,
    selected: Boolean = false,
    onClick: (() -> Unit)? = null,
) {
    val cardWidth = 92.dp
    val cardHeight = 136.dp

    Column(
        verticalArrangement = Arrangement.spacedBy(6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall)
        Card(
            modifier = Modifier
                .width(cardWidth)
                .height(cardHeight)
                .shadow(
                    elevation = if (selected) 6.dp else 3.dp,
                    ambientColor = Color.Black.copy(alpha = 0.06f),
                    spotColor = Color.Black.copy(alpha = 0.12f),
                    shape = MaterialTheme.shapes.medium,
                )
                .graphicsLayer {
                    scaleX = if (selected) 1.03f else 1f
                    scaleY = if (selected) 1.03f else 1f
                    alpha = if (selected) 1f else 0.96f
                }
                .clip(MaterialTheme.shapes.medium)
                .border(
                    width = if (selected) 1.5.dp else 0.dp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                    shape = MaterialTheme.shapes.medium,
                )
                .let { modifier ->
                    if (onClick != null) modifier.clickable(onClick = onClick) else modifier
                },
            elevation = CardDefaults.cardElevation(defaultElevation = if (selected) 2.dp else 0.dp),
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                TarotCardFaceContent(card = card, revealed = true)
            }
        }
    }
}
