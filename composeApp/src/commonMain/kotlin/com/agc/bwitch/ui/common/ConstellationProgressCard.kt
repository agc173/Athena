package com.agc.bwitch.ui.common

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
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

private fun template(name: String, nodes: List<ConstellationNode>, edges: List<ConstellationEdge>) =
    ConstellationTemplate(name = name, nodes = nodes, edges = edges, revealSteps = buildRevealSteps(nodes.size, edges))

private fun buildRevealSteps(nodeCount: Int, edges: List<ConstellationEdge>): List<RevealStep> = buildList {
    if (nodeCount == 0) return@buildList
    add(RevealStep.Node(0))
    edges.forEachIndexed { edgeIndex, edge ->
        add(RevealStep.Edge(edgeIndex))
        add(RevealStep.Node(edge.to))
    }
    (1 until nodeCount).filter { nodeIndex -> edges.none { it.to == nodeIndex } }.forEach { add(RevealStep.Node(it)) }
}

// Nota UX importante: estas geometrías son estilizaciones zodiacales para identidad visual.
// No representan posiciones astronómicas reales.
val AriesSimplifiedTemplate = template(
    "Aries",
    listOf(
        ConstellationNode(0.118f, 0.328f),
        ConstellationNode(0.358f, 0.120f),
        ConstellationNode(0.616f, 0.438f),
        ConstellationNode(0.831f, 0.605f),
        ConstellationNode(0.854f, 0.811f),
    ),
    listOf(ConstellationEdge(0, 1), ConstellationEdge(1, 2), ConstellationEdge(2, 3), ConstellationEdge(3, 4)),
)
val TaurusStylizedTemplate = template(
    "Taurus",
    listOf(ConstellationNode(0.24f, 0.62f), ConstellationNode(0.40f, 0.44f), ConstellationNode(0.50f, 0.30f), ConstellationNode(0.62f, 0.43f), ConstellationNode(0.76f, 0.60f), ConstellationNode(0.60f, 0.72f), ConstellationNode(0.38f, 0.72f)),
    listOf(ConstellationEdge(0, 1), ConstellationEdge(1, 2), ConstellationEdge(2, 3), ConstellationEdge(3, 4), ConstellationEdge(4, 5), ConstellationEdge(5, 6), ConstellationEdge(6, 0)),
)
val GeminiStylizedTemplate = template(
    "Gemini",
    listOf(ConstellationNode(0.30f, 0.24f), ConstellationNode(0.30f, 0.78f), ConstellationNode(0.70f, 0.24f), ConstellationNode(0.70f, 0.78f), ConstellationNode(0.45f, 0.24f), ConstellationNode(0.55f, 0.78f)),
    listOf(ConstellationEdge(0, 4), ConstellationEdge(4, 2), ConstellationEdge(1, 5), ConstellationEdge(5, 3), ConstellationEdge(0, 1), ConstellationEdge(2, 3)),
)
val CancerStylizedTemplate = template(
    "Cancer",
    listOf(ConstellationNode(0.26f, 0.42f), ConstellationNode(0.42f, 0.30f), ConstellationNode(0.58f, 0.44f), ConstellationNode(0.74f, 0.58f), ConstellationNode(0.58f, 0.72f), ConstellationNode(0.40f, 0.62f), ConstellationNode(0.30f, 0.52f)),
    listOf(ConstellationEdge(0, 1), ConstellationEdge(1, 2), ConstellationEdge(2, 3), ConstellationEdge(3, 4), ConstellationEdge(4, 5), ConstellationEdge(5, 6), ConstellationEdge(6, 0)),
)
val LeoStylizedTemplate = template(
    "Leo",
    listOf(ConstellationNode(0.24f, 0.60f), ConstellationNode(0.36f, 0.44f), ConstellationNode(0.52f, 0.34f), ConstellationNode(0.66f, 0.42f), ConstellationNode(0.74f, 0.58f), ConstellationNode(0.64f, 0.72f), ConstellationNode(0.48f, 0.70f), ConstellationNode(0.36f, 0.62f)),
    listOf(ConstellationEdge(0, 1), ConstellationEdge(1, 2), ConstellationEdge(2, 3), ConstellationEdge(3, 4), ConstellationEdge(4, 5), ConstellationEdge(5, 6), ConstellationEdge(6, 7), ConstellationEdge(7, 0), ConstellationEdge(2, 6)),
)
val VirgoStylizedTemplate = template(
    "Virgo",
    listOf(ConstellationNode(0.22f, 0.26f), ConstellationNode(0.22f, 0.74f), ConstellationNode(0.42f, 0.26f), ConstellationNode(0.42f, 0.74f), ConstellationNode(0.58f, 0.34f), ConstellationNode(0.58f, 0.68f), ConstellationNode(0.72f, 0.52f), ConstellationNode(0.82f, 0.62f)),
    listOf(ConstellationEdge(0, 1), ConstellationEdge(2, 3), ConstellationEdge(1, 3), ConstellationEdge(2, 4), ConstellationEdge(4, 5), ConstellationEdge(5, 6), ConstellationEdge(6, 7)),
)
val LibraStylizedTemplate = template(
    "Libra",
    listOf(ConstellationNode(0.22f, 0.68f), ConstellationNode(0.78f, 0.68f), ConstellationNode(0.34f, 0.48f), ConstellationNode(0.50f, 0.36f), ConstellationNode(0.66f, 0.48f), ConstellationNode(0.50f, 0.68f)),
    listOf(ConstellationEdge(0, 5), ConstellationEdge(5, 1), ConstellationEdge(2, 3), ConstellationEdge(3, 4), ConstellationEdge(2, 5), ConstellationEdge(4, 5)),
)
val ScorpioStylizedTemplate = template(
    "Scorpio",
    listOf(
        ConstellationNode(0.12f, 0.28f),
        ConstellationNode(0.12f, 0.52f),
        ConstellationNode(0.28f, 0.34f),
        ConstellationNode(0.28f, 0.64f),
        ConstellationNode(0.46f, 0.42f),
        ConstellationNode(0.46f, 0.72f),
        ConstellationNode(0.62f, 0.56f),
        ConstellationNode(0.76f, 0.72f),
        ConstellationNode(0.90f, 0.52f),
    ),
    listOf(
        ConstellationEdge(0,1),
        ConstellationEdge(1,3),
        ConstellationEdge(2,3),
        ConstellationEdge(2,4),
        ConstellationEdge(4,5),
        ConstellationEdge(5,6),
        ConstellationEdge(6,7),
        ConstellationEdge(7,8),
    ),
)
val SagittariusStylizedTemplate = template(
    "Sagittarius",
    listOf(
        ConstellationNode(0.18f, 0.78f),
        ConstellationNode(0.44f, 0.52f),
        ConstellationNode(0.72f, 0.26f),
        ConstellationNode(0.74f, 0.46f),
        ConstellationNode(0.88f, 0.24f),
        ConstellationNode(0.58f, 0.18f),
        ConstellationNode(0.30f, 0.36f),
    ),
    listOf(
        ConstellationEdge(0,1),
        ConstellationEdge(1,2),
        ConstellationEdge(2,3),
        ConstellationEdge(2,4),
        ConstellationEdge(2,5),
        ConstellationEdge(1,6),
    ),
)
val CapricornStylizedTemplate = template(
    "Capricorn",
    listOf(ConstellationNode(0.22f, 0.70f), ConstellationNode(0.30f, 0.32f), ConstellationNode(0.46f, 0.58f), ConstellationNode(0.60f, 0.34f), ConstellationNode(0.74f, 0.50f), ConstellationNode(0.70f, 0.72f), ConstellationNode(0.54f, 0.74f)),
    listOf(ConstellationEdge(0, 1), ConstellationEdge(1, 2), ConstellationEdge(2, 3), ConstellationEdge(3, 4), ConstellationEdge(4, 5), ConstellationEdge(5, 6), ConstellationEdge(6, 2)),
)
val AquariusStylizedTemplate = template(
    "Aquarius",
    listOf(
        ConstellationNode(0.10f, 0.38f),
        ConstellationNode(0.24f, 0.30f),
        ConstellationNode(0.36f, 0.42f),
        ConstellationNode(0.48f, 0.32f),
        ConstellationNode(0.60f, 0.44f),
        ConstellationNode(0.74f, 0.34f),
        ConstellationNode(0.88f, 0.46f),
        ConstellationNode(0.12f, 0.62f),
        ConstellationNode(0.26f, 0.54f),
        ConstellationNode(0.40f, 0.66f),
        ConstellationNode(0.54f, 0.56f),
        ConstellationNode(0.68f, 0.68f),
        ConstellationNode(0.82f, 0.58f),
    ),
    listOf(
        ConstellationEdge(0,1),
        ConstellationEdge(1,2),
        ConstellationEdge(2,3),
        ConstellationEdge(3,4),
        ConstellationEdge(4,5),
        ConstellationEdge(5,6),
        ConstellationEdge(7,8),
        ConstellationEdge(8,9),
        ConstellationEdge(9,10),
        ConstellationEdge(10,11),
        ConstellationEdge(11,12),
    ),
)
val PiscesStylizedTemplate = template(
    "Pisces",
    listOf(ConstellationNode(0.28f, 0.28f), ConstellationNode(0.28f, 0.72f), ConstellationNode(0.72f, 0.28f), ConstellationNode(0.72f, 0.72f), ConstellationNode(0.50f, 0.50f)),
    listOf(ConstellationEdge(0, 4), ConstellationEdge(4, 3), ConstellationEdge(1, 4), ConstellationEdge(4, 2), ConstellationEdge(0, 1), ConstellationEdge(2, 3)),
)

val ZodiacStylizedTemplates = listOf(
    AriesSimplifiedTemplate, TaurusStylizedTemplate, GeminiStylizedTemplate, CancerStylizedTemplate, LeoStylizedTemplate, VirgoStylizedTemplate,
    LibraStylizedTemplate, ScorpioStylizedTemplate, SagittariusStylizedTemplate, CapricornStylizedTemplate, AquariusStylizedTemplate, PiscesStylizedTemplate,
)

@Composable
fun ConstellationProgressCard(progressSteps: Int, template: ConstellationTemplate, modifier: Modifier = Modifier) {
    val totalSteps = template.totalSteps
    val activeCount = progressSteps.coerceIn(0, totalSteps)
    val revealedSteps = remember(activeCount, template) { template.revealSteps.take(activeCount) }
    val revealedNodeIndexes = remember(revealedSteps) { revealedSteps.filterIsInstance<RevealStep.Node>().map { it.index }.toSet() }
    val revealedEdgeIndexes = remember(revealedSteps) { revealedSteps.filterIsInstance<RevealStep.Edge>().map { it.index }.toSet() }
    val lastRevealedNodeIndex = remember(revealedSteps) { revealedSteps.lastOrNull { it is RevealStep.Node }?.let { (it as RevealStep.Node).index } }
    val pulseTransition = rememberInfiniteTransition(label = "aries-pulse")
    val pulse by pulseTransition.animateFloat(initialValue = 0.75f, targetValue = 1f, animationSpec = infiniteRepeatable(animation = tween(durationMillis = 1400), repeatMode = RepeatMode.Reverse), label = "aries-pulse-alpha")
    val cardColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
    val inactiveLine = Color(0xFF90A4B8).copy(alpha = 0.58f)
    val inactiveNode = Color(0xFFDCE6F2).copy(alpha = 0.78f)
    val activeLine = Color(0xFFF4CB7D).copy(alpha = 0.96f)
    val activeNode = Color(0xFFFFE2A2)

    Card(modifier = modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = cardColor)) {
        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(text = "Constelación de ${template.name}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
            Text(text = "$activeCount/$totalSteps luces despertadas", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.72f))
            Canvas(modifier = Modifier.fillMaxWidth().aspectRatio(1.9f)) {
                val scaledPoints = template.nodes.map { Offset(it.x * size.width, it.y * size.height) }
                template.edges.forEachIndexed { lineIndex, edge ->
                    drawLine(color = inactiveLine, start = scaledPoints[edge.from], end = scaledPoints[edge.to], strokeWidth = 16f, cap = StrokeCap.Round)
                    if (lineIndex in revealedEdgeIndexes) drawLine(color = activeLine, start = scaledPoints[edge.from], end = scaledPoints[edge.to], strokeWidth = 18f, cap = StrokeCap.Round)
                }
                scaledPoints.forEachIndexed { index, point ->
                    drawCircle(color = inactiveNode.copy(alpha = 0.26f * pulse), radius = 44f, center = point)
                    drawCircle(color = inactiveNode, radius = 26f, center = point)
                    if (index in revealedNodeIndexes) {
                        val isLastActive = lastRevealedNodeIndex == index
                        val glowAlpha = if (isLastActive) pulse else 0.85f
                        drawCircle(color = activeNode.copy(alpha = 0.18f * glowAlpha), radius = 44f, center = point)
                        drawCircle(color = activeNode.copy(alpha = glowAlpha), radius = 30f, center = point)
                    }
                }
            }
        }
    }
}

@Composable
fun ConstellationBadgeCard(progressSteps: Int, template: ConstellationTemplate, modifier: Modifier = Modifier) {
    val totalSteps = template.totalSteps
    val activeCount = progressSteps.coerceIn(0, totalSteps)
    val revealedSteps = remember(activeCount, template) { template.revealSteps.take(activeCount) }
    val revealedNodeIndexes = remember(revealedSteps) { revealedSteps.filterIsInstance<RevealStep.Node>().map { it.index }.toSet() }
    val revealedEdgeIndexes = remember(revealedSteps) { revealedSteps.filterIsInstance<RevealStep.Edge>().map { it.index }.toSet() }
    val lastRevealedNodeIndex = remember(revealedSteps) { revealedSteps.lastOrNull { it is RevealStep.Node }?.let { (it as RevealStep.Node).index } }
    val pulseTransition = rememberInfiniteTransition(label = "badge-pulse")
    val pulse by pulseTransition.animateFloat(initialValue = 0.82f, targetValue = 1f, animationSpec = infiniteRepeatable(animation = tween(durationMillis = 1800), repeatMode = RepeatMode.Reverse), label = "badge-pulse-alpha")

    val inactiveLine = Color(0xFF90A4B8).copy(alpha = 0.62f)
    val inactiveNode = Color(0xFFDCE6F2).copy(alpha = 0.84f)
    val activeLine = Color(0xFFF4CB7D).copy(alpha = 0.98f)
    val activeNode = Color(0xFFFFE2A2)
    val frame = Color(0xFFB5C7DC).copy(alpha = 0.32f)

    Card(modifier = modifier, shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f))) {
        Column(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(text = template.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(text = "$activeCount/$totalSteps", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
            Box(modifier = Modifier.fillMaxWidth().aspectRatio(1.05f).border(width = 1.dp, color = frame, shape = RoundedCornerShape(18.dp)), contentAlignment = Alignment.Center) {
                Canvas(modifier = Modifier.fillMaxWidth().aspectRatio(1.05f).padding(4.dp)) {
                    val scaledPoints = template.nodes.map { Offset(it.x * size.width, it.y * size.height) }
                    template.edges.forEachIndexed { lineIndex, edge ->
                        drawLine(color = inactiveLine, start = scaledPoints[edge.from], end = scaledPoints[edge.to], strokeWidth = 16f, cap = StrokeCap.Round)
                        if (lineIndex in revealedEdgeIndexes) drawLine(color = activeLine, start = scaledPoints[edge.from], end = scaledPoints[edge.to], strokeWidth = 18f, cap = StrokeCap.Round)
                    }
                    scaledPoints.forEachIndexed { index, point ->
                        drawCircle(color = inactiveNode.copy(alpha = 0.18f * pulse), radius = 44f, center = point, style = Stroke(width = 6f))
                        drawCircle(color = inactiveNode, radius = 26f, center = point)
                        if (index in revealedNodeIndexes) {
                            val glowAlpha = if (lastRevealedNodeIndex == index) pulse else 0.85f
                            drawCircle(color = activeNode.copy(alpha = 0.20f * glowAlpha), radius = 44f, center = point)
                            drawCircle(color = activeNode.copy(alpha = glowAlpha), radius = 30f, center = point)
                        }
                    }
                }
            }
        }
    }
}
