package com.agc.bwitch.ui.tarot.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RectangleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.agc.bwitch.domain.tarot.TarotCard
import com.agc.bwitch.ui.tarot.TarotCardArt
import bwitch.composeapp.generated.resources.*
import org.jetbrains.compose.resources.painterResource
import kotlin.math.abs


@Composable
fun TarotCardView(
    card: TarotCard?,
    revealed: Boolean,
    cardWidth: Dp = 160.dp,
    onClick: (() -> Unit)? = null,
) {
    val flipRotation = remember { Animatable(0f) }

    LaunchedEffect(revealed) {
        if (revealed) {
            flipRotation.animateTo(
                targetValue = 180f,
                animationSpec = tween(durationMillis = REVEAL_FLIP_DURATION_MS),
            )
        } else {
            flipRotation.snapTo(0f)
        }
    }

    val visualRotation = if (revealed) flipRotation.value else 0f
    val isFrontVisible = revealed && visualRotation >= 90f
    val revealPeak = (1f - (abs(visualRotation - 90f) / 90f)).coerceIn(0f, 1f)
    val revealScale = 1f + (0.035f * revealPeak)

    val cardModifier = Modifier
        .width(cardWidth)
        .aspectRatio(TAROT_CARD_ASPECT_RATIO)
        .shadow(
            elevation = 4.dp,
            shape = RectangleShape,
            clip = false,
        )
        .graphicsLayer {
            scaleX = revealScale
            scaleY = revealScale
        }
        .let { modifier -> if (onClick != null) modifier.clickable(onClick = onClick) else modifier }

    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        Box(modifier = cardModifier) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            rotationY = visualRotation
                            alpha = if (isFrontVisible) 0f else 1f
                            cameraDistance = 12f * density
                        },
                ) {
                    TarotCardFaceContent(card = card, revealed = false)
                }

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            rotationY = visualRotation + 180f
                            alpha = if (isFrontVisible) 1f else 0f
                            cameraDistance = 12f * density
                        },
                ) {
                    TarotCardFaceContent(card = card, revealed = true)
                }
            }
        }
    }
}

private const val TAROT_CARD_ASPECT_RATIO = 0.6f
private const val REVEAL_FLIP_DURATION_MS = 520

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
        contentScale = ContentScale.Fit,
    )
}

@Composable
private fun TarotKnownFace(card: TarotCard?, drawable: org.jetbrains.compose.resources.DrawableResource) {
    Image(
        painter = painterResource(drawable),
        contentDescription = card?.name ?: "Tarot card face",
        modifier = Modifier.fillMaxSize(),
        contentScale = ContentScale.Fit,
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
