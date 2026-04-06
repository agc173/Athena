package com.agc.bwitch.ui.rituals.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.agc.bwitch.presentation.rituals.HabitsGlowLevel

@Composable
fun HabitsProgressRing(
    current: Int,
    target: Int,
    modifier: Modifier = Modifier,
    size: Dp = 92.dp,
    strokeWidth: Dp = 8.dp,
    glowLevel: HabitsGlowLevel = HabitsGlowLevel.Base,
) {
    val normalizedCurrent = current.coerceAtLeast(0)
    val normalizedTarget = target.coerceAtLeast(1)
    val progress = (normalizedCurrent.toFloat() / normalizedTarget.toFloat()).coerceIn(0f, 1f)
    val glowStyle = glowStyleFor(glowLevel, MaterialTheme.colorScheme.primary)
    val ringStrokeWidth = strokeWidth + glowStyle.extraStroke

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .size(size)
                .height(size),
        ) {
            val stroke = Stroke(width = ringStrokeWidth.toPx(), cap = StrokeCap.Round)
            drawArc(
                color = glowStyle.trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = stroke,
            )
            drawArc(
                color = glowStyle.progressColor,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                style = stroke,
            )
            if (glowStyle.accentAlpha > 0f && progress > 0f) {
                drawArc(
                    color = Color.White.copy(alpha = glowStyle.accentAlpha),
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    style = Stroke(width = (ringStrokeWidth * 0.34f).toPx(), cap = StrokeCap.Round),
                )
            }
        }

        Text(
            text = "$normalizedCurrent/$normalizedTarget",
            style = MaterialTheme.typography.labelLarge,
            color = glowStyle.progressColor,
        )
    }
}

private data class HabitsRingGlowStyle(
    val trackColor: Color,
    val progressColor: Color,
    val accentAlpha: Float,
    val extraStroke: Dp,
)

private fun glowStyleFor(level: HabitsGlowLevel, primary: Color): HabitsRingGlowStyle = when (level) {
    HabitsGlowLevel.Base -> HabitsRingGlowStyle(
        trackColor = primary.copy(alpha = 0.20f),
        progressColor = primary.copy(alpha = 0.92f),
        accentAlpha = 0f,
        extraStroke = 0.dp,
    )

    HabitsGlowLevel.Soft -> HabitsRingGlowStyle(
        trackColor = primary.copy(alpha = 0.25f),
        progressColor = primary,
        accentAlpha = 0.10f,
        extraStroke = 0.dp,
    )

    HabitsGlowLevel.Bright -> HabitsRingGlowStyle(
        trackColor = primary.copy(alpha = 0.30f),
        progressColor = primary.copy(alpha = 1f),
        accentAlpha = 0.18f,
        extraStroke = 0.5.dp,
    )

    HabitsGlowLevel.Luminous -> HabitsRingGlowStyle(
        trackColor = primary.copy(alpha = 0.35f),
        progressColor = primary.copy(alpha = 1f),
        accentAlpha = 0.28f,
        extraStroke = 1.dp,
    )
}
