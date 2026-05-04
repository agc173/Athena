package com.agc.bwitch.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.math.roundToInt

@Composable
fun MoonBalanceBadge(
    balance: Int,
    shouldShake: Boolean = false,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val shape = RoundedCornerShape(999.dp)
    val scale = remember { Animatable(1f) }
    val offsetX = remember { Animatable(0f) }

    LaunchedEffect(balance) {
        scale.snapTo(1f)
        scale.animateTo(
            targetValue = 1.08f,
            animationSpec = tween(durationMillis = 110, easing = LinearEasing),
        )
        scale.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 110, easing = LinearEasing),
        )
    }

    LaunchedEffect(shouldShake) {
        if (!shouldShake) return@LaunchedEffect
        offsetX.snapTo(0f)
        val sequence = listOf(-4f, 4f, -3f, 3f, 0f)
        sequence.forEach { target ->
            offsetX.animateTo(
                targetValue = target,
                animationSpec = tween(durationMillis = 36, easing = LinearEasing),
            )
        }
    }

    Text(
        text = "🌙 $balance",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = modifier
            .padding(horizontal = 8.dp, vertical = 8.dp)
            .offset { IntOffset(offsetX.value.roundToInt(), 0) }
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
            }
            .clip(shape)
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), shape)
            .then(
                if (onClick != null) {
                    Modifier.clickable(onClick = onClick)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}
