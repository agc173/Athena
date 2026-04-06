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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun HabitsProgressRing(
    current: Int,
    target: Int,
    modifier: Modifier = Modifier,
    size: Dp = 92.dp,
    strokeWidth: Dp = 8.dp,
) {
    val normalizedCurrent = current.coerceAtLeast(0)
    val normalizedTarget = target.coerceAtLeast(1)
    val progress = (normalizedCurrent.toFloat() / normalizedTarget.toFloat()).coerceIn(0f, 1f)
    val trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
    val progressColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier.size(size),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(
            modifier = Modifier
                .size(size)
                .height(size),
        ) {
            val stroke = Stroke(width = strokeWidth.toPx(), cap = StrokeCap.Round)
            drawArc(
                color = trackColor,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                style = stroke,
            )
            drawArc(
                color = progressColor,
                startAngle = -90f,
                sweepAngle = 360f * progress,
                useCenter = false,
                style = stroke,
            )
        }

        Text(
            text = "$normalizedCurrent/$normalizedTarget",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}
