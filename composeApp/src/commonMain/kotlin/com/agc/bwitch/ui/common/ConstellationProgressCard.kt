package com.agc.bwitch.ui.common

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.text.font.FontWeight
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
        ConstellationNode(0.20f, 0.72f),
        ConstellationNode(0.42f, 0.57f),
        ConstellationNode(0.56f, 0.40f),
        ConstellationNode(0.80f, 0.49f),
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

@Composable
fun ConstellationBadgeCard(
    progressSteps: Int,
    template: ConstellationTemplate,
    modifier: Modifier = Modifier,
) {
    val totalSteps = template.totalSteps
    val activeCount = progressSteps.coerceIn(0, totalSteps)
    val revealedSteps = remember(activeCount, template) { template.revealSteps.take(activeCount) }
    val revealedNodeIndexes = remember(revealedSteps) { revealedSteps.filterIsInstance<RevealStep.Node>().map { it.index }.toSet() }
    val revealedEdgeIndexes = remember(revealedSteps) { revealedSteps.filterIsInstance<RevealStep.Edge>().map { it.index }.toSet() }
    val lastRevealedNodeIndex = remember(revealedSteps) { revealedSteps.lastOrNull { it is RevealStep.Node }?.let { (it as RevealStep.Node).index } }
    val pulseTransition = rememberInfiniteTransition(label = "badge-pulse")
    val pulse by pulseTransition.animateFloat(
        initialValue = 0.82f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(durationMillis = 1800), repeatMode = RepeatMode.Reverse),
        label = "badge-pulse-alpha",
    )

    val inactiveLine = Color(0xFF9FB1C4).copy(alpha = 0.56f)
    val inactiveNode = Color(0xFFD9E4F1).copy(alpha = 0.72f)
    val activeLine = Color(0xFFAAC0FF).copy(alpha = 0.95f)
    val activeNode = Color(0xFFFFE3A8)
    val frame = Color(0xFFB5C7DC).copy(alpha = 0.32f)
    val halo = Color(0xFFAFC5FF).copy(alpha = 0.10f)

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            Text(text = template.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(text = "$activeCount/$totalSteps", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.05f)
                    .border(width = 1.dp, color = frame, shape = RoundedCornerShape(18.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(modifier = Modifier.fillMaxWidth().aspectRatio(1.05f).padding(10.dp)) {
                    val scaledPoints = template.nodes.map { Offset(it.x * size.width, it.y * size.height) }
                    drawCircle(color = halo.copy(alpha = 0.08f * pulse), radius = size.minDimension * 0.45f, center = Offset(size.width * 0.5f, size.height * 0.5f))
                    template.edges.forEachIndexed { lineIndex, edge ->
                        drawLine(color = inactiveLine, start = scaledPoints[edge.from], end = scaledPoints[edge.to], strokeWidth = 5f, cap = StrokeCap.Round)
                        if (lineIndex in revealedEdgeIndexes) {
                            drawLine(color = activeLine, start = scaledPoints[edge.from], end = scaledPoints[edge.to], strokeWidth = 6f, cap = StrokeCap.Round)
                        }
                    }
                    scaledPoints.forEachIndexed { index, point ->
                        drawCircle(color = inactiveNode.copy(alpha = 0.18f * pulse), radius = 18f, center = point, style = Stroke(width = 2.5f))
                        drawCircle(color = inactiveNode, radius = 8.5f, center = point)
                        if (index in revealedNodeIndexes) {
                            val glowAlpha = if (lastRevealedNodeIndex == index) pulse else 0.85f
                            drawCircle(color = activeNode.copy(alpha = 0.20f * glowAlpha), radius = 18f, center = point)
                            drawCircle(color = activeNode.copy(alpha = glowAlpha), radius = 10f, center = point)
                        }
                    }
                }
            }
        }
    }
}
