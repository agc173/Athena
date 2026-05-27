package com.agc.bwitch.ui.common

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
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

data class ConstellationNode(val x: Float, val y: Float)

data class ConstellationEdge(val from: Int, val to: Int)

sealed interface RevealStep {
    data class Node(val index: Int) : RevealStep
    data class Edge(val index: Int) : RevealStep
}

data class ConstellationTemplate(
    val name: String,
    val nodes: List<ConstellationNode>,
    val edges: List<ConstellationEdge>,
    val revealSteps: List<RevealStep>,
) {
    val totalSteps: Int get() = revealSteps.size
}

val AriesSimplifiedTemplate = ConstellationTemplate(
    name = "Aries",
    nodes = listOf(
        ConstellationNode(0.16f, 0.74f),
        ConstellationNode(0.34f, 0.58f),
        ConstellationNode(0.56f, 0.44f),
        ConstellationNode(0.78f, 0.29f),
    ),
    edges = listOf(
        ConstellationEdge(0, 1),
        ConstellationEdge(1, 2),
        ConstellationEdge(2, 3),
    ),
    revealSteps = listOf(
        RevealStep.Node(0),
        RevealStep.Edge(0),
        RevealStep.Node(1),
        RevealStep.Edge(1),
        RevealStep.Node(2),
        RevealStep.Edge(2),
        RevealStep.Node(3),
    ),
)

@Composable
fun ConstellationProgressCard(
    progressSteps: Int,
    template: ConstellationTemplate,
    modifier: Modifier = Modifier,
) {
    val totalSteps = template.totalSteps
    val activeCount = progressSteps.coerceIn(0, totalSteps)
    val revealedSteps = remember(activeCount, template) {
        template.revealSteps.take(activeCount)
    }
    val revealedNodeIndexes = remember(revealedSteps) {
        revealedSteps.filterIsInstance<RevealStep.Node>().map { it.index }.toSet()
    }
    val revealedEdgeIndexes = remember(revealedSteps) {
        revealedSteps.filterIsInstance<RevealStep.Edge>().map { it.index }.toSet()
    }
    val lastRevealedNodeIndex = remember(revealedSteps) {
        revealedSteps.lastOrNull { it is RevealStep.Node }?.let { (it as RevealStep.Node).index }
    }
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
    val inactiveLine = Color(0xFF9AAEC0).copy(alpha = 0.20f)
    val inactiveNode = Color(0xFFD8E2EC).copy(alpha = 0.38f)
    val activeLine = Color(0xFF98B6FF).copy(alpha = 0.74f)
    val activeNode = Color(0xFFFFDF9C)

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = cardColor),
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "Constelación de ${template.name}",
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
                val scaledPoints = template.nodes.map { Offset(it.x * size.width, it.y * size.height) }
                template.edges.forEachIndexed { lineIndex, edge ->
                    drawLine(
                        color = inactiveLine,
                        start = scaledPoints[edge.from],
                        end = scaledPoints[edge.to],
                        strokeWidth = 1.65f,
                    )
                    if (lineIndex in revealedEdgeIndexes) {
                        drawLine(
                            color = activeLine,
                            start = scaledPoints[edge.from],
                            end = scaledPoints[edge.to],
                            strokeWidth = 2.2f,
                        )
                    }
                }
                scaledPoints.forEachIndexed { index, point ->
                    drawCircle(color = inactiveNode, radius = 5.5f, center = point)
                    if (index in revealedNodeIndexes) {
                        val isLastActive = lastRevealedNodeIndex == index
                        val glowAlpha = if (isLastActive) pulse else 0.85f
                        drawCircle(
                            color = activeNode.copy(alpha = 0.18f * glowAlpha),
                            radius = 9.5f,
                            center = point,
                        )
                        drawCircle(
                            color = activeNode.copy(alpha = glowAlpha),
                            radius = 5.8f,
                            center = point,
                        )
                    }
                }
            }
        }
    }
}
