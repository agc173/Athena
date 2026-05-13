package com.agc.bwitch.ui.tarot.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun TarotLoadingDeck(
    title: String,
    subtitle: String,
) {
    val cardWidth = 172.dp
    val cardHeight = cardWidth / TAROT_LOADING_CARD_ASPECT_RATIO
    val transition = rememberInfiniteTransition(label = "tarot-loading-transition")
    val backCardMovement by transition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(offsetMillis = 120),
        ),
        label = "back-card-movement",
    )
    val middleCardMovement by transition.animateFloat(
        initialValue = 6f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(offsetMillis = 240),
        ),
        label = "middle-card-movement",
    )
    val topCardMovement by transition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "top-card-movement",
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().height(cardHeight + 32.dp),
            contentAlignment = Alignment.Center,
        ) {
            TarotLoadingBackCard(
                cardWidth = cardWidth,
                modifier = Modifier.graphicsLayer {
                    rotationZ = -7f + backCardMovement
                    translationX = -14f
                    translationY = 10f
                },
            )

            TarotLoadingBackCard(
                cardWidth = cardWidth,
                modifier = Modifier.graphicsLayer {
                    rotationZ = 7f + middleCardMovement
                    translationX = 14f
                    translationY = 2f
                },
            )

            TarotLoadingBackCard(
                cardWidth = cardWidth,
                modifier = Modifier.graphicsLayer { rotationZ = topCardMovement },
            )
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun TarotLoadingBackCard(
    cardWidth: Dp,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .width(cardWidth)
            .aspectRatio(TAROT_LOADING_CARD_ASPECT_RATIO)
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(2.dp),
                clip = false,
            ),
    ) {
        TarotCardFaceContent(card = null, revealed = false)
    }
}

private const val TAROT_LOADING_CARD_ASPECT_RATIO = 0.6f
