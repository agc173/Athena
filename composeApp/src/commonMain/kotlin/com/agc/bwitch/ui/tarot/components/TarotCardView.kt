package com.agc.bwitch.ui.tarot.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.agc.bwitch.domain.tarot.TarotCard
import com.agc.bwitch.ui.tarot.TarotCardArt
import bwitch.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.painterResource


@Composable
fun TarotCardView(
    card: TarotCard?,
    revealed: Boolean,
    cardWidth: Dp = 160.dp,
    cardHeight: Dp = 240.dp,
    onClick: (() -> Unit)? = null,
) {
    val cardModifier = Modifier
        .width(cardWidth)
        .height(cardHeight)
        .let { modifier -> if (onClick != null) modifier.clickable(onClick = onClick) else modifier }

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Card(modifier = cardModifier) {
            AnimatedContent(
                targetState = revealed,
                transitionSpec = {
                    (
                        (fadeIn(animationSpec = tween(280)) + scaleIn(initialScale = 0.98f, animationSpec = tween(280))) togetherWith
                            (fadeOut(animationSpec = tween(180)) + scaleOut(targetScale = 1.01f, animationSpec = tween(180)))
                        ).using(SizeTransform(clip = false))
                },
                label = "tarot-card-reveal-content",
            ) { isRevealed ->
                TarotCardFaceContent(card = card, revealed = isRevealed)
            }
        }
    }
}

@Composable
internal fun TarotCardFaceContent(card: TarotCard?, revealed: Boolean) {
    if (!revealed) {
        TarotBackFace()
        return
    }

    val faceDrawable = TarotCardArt.faceDrawableForCardId(card?.id)
    if (faceDrawable != null) {
        TarotKnownFace(card = card, drawable = faceDrawable)
    } else {
        TarotPremiumFallbackFace(card)
    }
}

@Composable
private fun TarotBackFace() {
    Image(
        painter = painterResource(Res.drawable.tarot_back_bw),
        contentDescription = "Tarot card back",
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
    )
}

@Composable
private fun TarotKnownFace(card: TarotCard?, drawable: org.jetbrains.compose.resources.DrawableResource) {
    Image(
        painter = painterResource(drawable),
        contentDescription = card?.name ?: "Tarot card face",
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Crop,
    )
}

@Composable
private fun TarotPremiumFallbackFace(
    card: TarotCard?,
    status: String = "Ilustración próximamente",
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.tertiaryContainer,
                        MaterialTheme.colorScheme.surfaceVariant,
                        MaterialTheme.colorScheme.surface,
                    ),
                ),
            )
            .padding(16.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(status, style = MaterialTheme.typography.labelMedium)

        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.74f))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(card?.name ?: "Arcano", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            card?.upright?.let { isUpright ->
                Text(if (isUpright) "Al derecho" else "Invertida", style = MaterialTheme.typography.bodySmall)
            }
        }

        Text("✨", style = MaterialTheme.typography.titleLarge)
    }
}
