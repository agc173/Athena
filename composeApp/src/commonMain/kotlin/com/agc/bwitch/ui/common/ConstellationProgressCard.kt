package com.agc.bwitch.ui.common

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun ConstellationProgressCard(
    progressSteps: Int,
    totalSteps: Int,
    modifier: Modifier = Modifier,
) {
    val nodePoints = remember {
        listOf(
            Offset(0.10f, 0.78f), Offset(0.18f, 0.62f), Offset(0.31f, 0.52f), Offset(0.44f, 0.45f),
            Offset(0.57f, 0.35f), Offset(0.69f, 0.27f), Offset(0.80f, 0.22f), Offset(0.88f, 0.30f),
            Offset(0.83f, 0.45f), Offset(0.71f, 0.59f), Offset(0.56f, 0.72f), Offset(0.38f, 0.82f),
        )
    }
    val activeCount = progressSteps.coerceIn(0, minOf(totalSteps, nodePoints.size))
    val activeProgress by animateFloatAsState(
        targetValue = activeCount.toFloat(),
        animationSpec = tween(durationMillis = 600),
    )
    val pulseTransition = rememberInfiniteTransition(label = "aries-pulse")
    val pulse by pulseTransition.animateFloat(
        initialValue = 0.75f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "aries-pulse-alpha",
    )
    val cardColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    val inactiveLine = Color(0xFF86A3B8).copy(alpha = 0.22f)
    val inactiveNode = Color(0xFFD9E5F0).copy(alpha = 0.25f)
    val activeLine = Color(0xFF7AA5FF).copy(alpha = 0.85f)
    val activeNode = Color(0xFFFFD88A)

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardColor),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Constelación de Aries",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = "$activeCount/$totalSteps luces despertadas",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f),
            )
            Canvas(
                modifier = Modifier.fillMaxWidth().aspectRatio(1.9f),
            ) {
                val scaledPoints = nodePoints.map { Offset(it.x * size.width, it.y * size.height) }
                val totalLines = (scaledPoints.size - 1).coerceAtLeast(0)
                for (lineIndex in 0 until totalLines) {
                    drawLine(
                        color = inactiveLine,
                        start = scaledPoints[lineIndex],
                        end = scaledPoints[lineIndex + 1],
                        strokeWidth = 2.5f,
                    )
                    if (lineIndex + 1 < activeCount) {
                        drawLine(
                            color = activeLine,
                            start = scaledPoints[lineIndex],
                            end = scaledPoints[lineIndex + 1],
                            strokeWidth = 3.1f,
                        )
                    }
                }
                scaledPoints.forEachIndexed { index, point ->
                    drawCircle(color = inactiveNode, radius = 7f, center = point)
                    if (index < activeProgress.toInt()) {
                        val isLastActive = index == activeProgress.toInt() - 1
                        val glowAlpha = if (isLastActive) pulse else 0.85f
                        drawCircle(
                            color = activeNode.copy(alpha = 0.30f * glowAlpha),
                            radius = 12f,
                            center = point,
                        )
                        drawCircle(
                            color = activeNode.copy(alpha = glowAlpha),
                            radius = 7.5f,
                            center = point,
                        )
                    }
                }
            }
        }
    }
}
