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
    listOf(
        ConstellationNode(0.199f, 0.103f), // 0 arriba izquierda
        ConstellationNode(0.364f, 0.229f), // 1
        ConstellationNode(0.487f, 0.379f), // 2
        ConstellationNode(0.487f, 0.557f), // 3 centro
        ConstellationNode(0.283f, 0.533f), // 4 izquierda baja
        ConstellationNode(0.103f, 0.353f), // 5 extremo izquierdo
        ConstellationNode(0.667f, 0.601f), // 6 derecha centro
        ConstellationNode(0.832f, 0.679f), // 7 derecha
        ConstellationNode(0.845f, 0.847f), // 8 abajo derecha
        ConstellationNode(0.559f, 0.763f), // 9 abajo centro
        ConstellationNode(0.681f, 0.833f), // 10 abajo centro-derecha
    ),
    listOf(
        ConstellationEdge(0, 1),
        ConstellationEdge(1, 2),
        ConstellationEdge(2, 3),
        ConstellationEdge(3, 4),
        ConstellationEdge(4, 5),
        ConstellationEdge(3, 6),
        ConstellationEdge(6, 7),
        ConstellationEdge(7, 8),
        ConstellationEdge(6, 9),
        ConstellationEdge(9, 10),
    ),
)
val GeminiStylizedTemplate = template(
    "Gemini",
    listOf(
        ConstellationNode(0.064f, 0.323f), // 0 izquierda
        ConstellationNode(0.246f, 0.073f), // 1 arriba izquierda
        ConstellationNode(0.578f, 0.338f), // 2 centro superior
        ConstellationNode(0.785f, 0.398f), // 3 derecha centro
        ConstellationNode(0.935f, 0.184f), // 4 arriba derecha
        ConstellationNode(0.747f, 0.670f), // 5 abajo derecha
        ConstellationNode(0.806f, 0.927f), // 6 extremo inferior derecho
        ConstellationNode(0.523f, 0.628f), // 7 centro inferior
    ),
    listOf(
        ConstellationEdge(0, 1),
        ConstellationEdge(1, 2),
        ConstellationEdge(2, 3),
        ConstellationEdge(3, 4),
        ConstellationEdge(3, 5),
        ConstellationEdge(5, 6),
        ConstellationEdge(5, 7),
        ConstellationEdge(7, 0),
    ),
)
val CancerStylizedTemplate = template(
    "Cancer",
    listOf(
        ConstellationNode(0.106f, 0.491f),
        ConstellationNode(0.464f, 0.546f),
        ConstellationNode(0.286f, 0.827f),
        ConstellationNode(0.674f, 0.370f),
        ConstellationNode(0.864f, 0.105f),
    ),
    listOf(
        ConstellationEdge(0, 1),
        ConstellationEdge(1, 2),
        ConstellationEdge(1, 3),
        ConstellationEdge(3, 4),
    ),
)
val LeoStylizedTemplate = template(
    "Leo",
    listOf(
        ConstellationNode(0.121f, 0.799f),
        ConstellationNode(0.246f, 0.514f),
        ConstellationNode(0.601f, 0.449f),
        ConstellationNode(0.520f, 0.258f),
        ConstellationNode(0.714f, 0.096f),
        ConstellationNode(0.861f, 0.206f),
        ConstellationNode(0.776f, 0.574f),
        ConstellationNode(0.775f, 0.805f),
        ConstellationNode(0.421f, 0.719f),
    ),
    listOf(
        ConstellationEdge(0, 1),
        ConstellationEdge(1, 2),
        ConstellationEdge(2, 3),
        ConstellationEdge(3, 4),
        ConstellationEdge(4, 5),
        ConstellationEdge(2, 6),
        ConstellationEdge(6, 7),
        ConstellationEdge(7, 8),
        ConstellationEdge(8, 0),
    ),
)
val VirgoStylizedTemplate = template(
    "Virgo",
    listOf(
        ConstellationNode(0.783f, 0.103f), // 0 arriba derecha
        ConstellationNode(0.513f, 0.117f), // 1 arriba centro
        ConstellationNode(0.651f, 0.206f), // 2 centro-alto derecha
        ConstellationNode(0.335f, 0.219f), // 3 izquierda alta
        ConstellationNode(0.863f, 0.351f), // 4 derecha
        ConstellationNode(0.519f, 0.416f), // 5 centro
        ConstellationNode(0.745f, 0.465f), // 6 derecha media
        ConstellationNode(0.235f, 0.523f), // 7 izquierda media
        ConstellationNode(0.429f, 0.629f), // 8 centro bajo
        ConstellationNode(0.158f, 0.693f), // 9 izquierda baja
        ConstellationNode(0.543f, 0.763f), // 10 abajo derecha
        ConstellationNode(0.139f, 0.873f), // 11 abajo izquierda
    ),
    listOf(
        ConstellationEdge(3, 1),
        ConstellationEdge(1, 2),
        ConstellationEdge(2, 0),
        ConstellationEdge(3, 5),
        ConstellationEdge(5, 6),
        ConstellationEdge(6, 4),
        ConstellationEdge(5, 8),
        ConstellationEdge(8, 10),
        ConstellationEdge(8, 7),
        ConstellationEdge(7, 9),
        ConstellationEdge(9, 11),
        ConstellationEdge(7, 3),
    ),
)
val LibraStylizedTemplate = template(
    "Libra",
    listOf(
        ConstellationNode(0.065f, 0.128f), // 0 izquierda
        ConstellationNode(0.310f, 0.275f), // 1 centro izquierda
        ConstellationNode(0.599f, 0.065f), // 2 arriba
        ConstellationNode(0.934f, 0.272f), // 3 derecha
        ConstellationNode(0.780f, 0.582f), // 4 abajo derecha
        ConstellationNode(0.436f, 0.702f), // 5 abajo centro
        ConstellationNode(0.439f, 0.933f), // 6 inferior
    ),
    listOf(
        ConstellationEdge(0, 1),
        ConstellationEdge(1, 2),
        ConstellationEdge(2, 3),
        ConstellationEdge(2, 4),
        ConstellationEdge(3, 4),
        ConstellationEdge(4, 5),
        ConstellationEdge(5, 6),
    ),
)

val ScorpioStylizedTemplate = template(
    "Scorpio",
    listOf(
        ConstellationNode(0.119f, 0.670f),
        ConstellationNode(0.228f, 0.534f),
        ConstellationNode(0.254f, 0.846f),
        ConstellationNode(0.471f, 0.739f),
        ConstellationNode(0.459f, 0.536f),
        ConstellationNode(0.514f, 0.346f),
        ConstellationNode(0.649f, 0.265f),
        ConstellationNode(0.677f, 0.117f),
        ConstellationNode(0.854f, 0.131f),
        ConstellationNode(0.853f, 0.321f),
    ),
    listOf(
        ConstellationEdge(0, 1),
        ConstellationEdge(0, 2),
        ConstellationEdge(2, 3),
        ConstellationEdge(3, 4),
        ConstellationEdge(4, 5),
        ConstellationEdge(5, 6),
        ConstellationEdge(6, 7),
        ConstellationEdge(6, 8),
        ConstellationEdge(6, 9),
    ),
)
val SagittariusStylizedTemplate = template(
    "Sagittarius",
    listOf(
        ConstellationNode(0.302f, 0.082f), // 0
        ConstellationNode(0.416f, 0.217f), // 1
        ConstellationNode(0.441f, 0.393f), // 2
        ConstellationNode(0.252f, 0.315f), // 3
        ConstellationNode(0.088f, 0.414f), // 4
        ConstellationNode(0.127f, 0.620f), // 5
        ConstellationNode(0.225f, 0.799f), // 6
        ConstellationNode(0.389f, 0.700f), // 7
        ConstellationNode(0.384f, 0.876f), // 8
        ConstellationNode(0.495f, 0.556f), // 9
        ConstellationNode(0.590f, 0.309f), // 10
        ConstellationNode(0.645f, 0.457f), // 11
        ConstellationNode(0.780f, 0.328f), // 12
        ConstellationNode(0.900f, 0.200f), // 13
        ConstellationNode(0.790f, 0.505f), // 14
        ConstellationNode(0.874f, 0.654f), // 15
        ConstellationNode(0.711f, 0.723f), // 16
        ConstellationNode(0.825f, 0.858f), // 17
    ),
    listOf(
        ConstellationEdge(0, 1),
        ConstellationEdge(1, 2),
        ConstellationEdge(2, 3),
        ConstellationEdge(3, 4),
        ConstellationEdge(4, 5),
        ConstellationEdge(5, 6),
        ConstellationEdge(6, 7),
        ConstellationEdge(6, 8),
        ConstellationEdge(2, 10),
        ConstellationEdge(10, 11),
        ConstellationEdge(11, 9),
        ConstellationEdge(9, 2),
        ConstellationEdge(11, 12),
        ConstellationEdge(12, 13),
        ConstellationEdge(12, 14),
        ConstellationEdge(14, 15),
        ConstellationEdge(14, 16),
        ConstellationEdge(16, 17),
    ),
)
val CapricornStylizedTemplate = template(
    "Capricorn",
    listOf(
        ConstellationNode(0.084f, 0.612f), // 0 izquierda
        ConstellationNode(0.271f, 0.581f), // 1
        ConstellationNode(0.469f, 0.561f), // 2
        ConstellationNode(0.594f, 0.466f), // 3
        ConstellationNode(0.811f, 0.102f), // 4 arriba
        ConstellationNode(0.856f, 0.306f), // 5
        ConstellationNode(0.804f, 0.644f), // 6 pequeño/intermedio
        ConstellationNode(0.711f, 0.864f), // 7 abajo derecha
        ConstellationNode(0.419f, 0.785f), // 8 abajo centro
    ),
    listOf(
        ConstellationEdge(0, 1),
        ConstellationEdge(1, 2),
        ConstellationEdge(2, 3),
        ConstellationEdge(3, 4),
        ConstellationEdge(4, 5),
        ConstellationEdge(5, 6),
        ConstellationEdge(6, 7),
        ConstellationEdge(7, 8),
        ConstellationEdge(8, 0),
    ),
)
val AquariusStylizedTemplate = template(
    "Aquarius",
    listOf(
        ConstellationNode(0.085f, 0.525f),
        ConstellationNode(0.265f, 0.477f),
        ConstellationNode(0.353f, 0.339f),
        ConstellationNode(0.259f, 0.130f),
        ConstellationNode(0.485f, 0.141f),
        ConstellationNode(0.577f, 0.275f),
        ConstellationNode(0.745f, 0.347f),
        ConstellationNode(0.794f, 0.558f),
        ConstellationNode(0.904f, 0.842f),
        ConstellationNode(0.506f, 0.503f),
        ConstellationNode(0.450f, 0.714f),
    ),
    listOf(
        ConstellationEdge(0, 1),
        ConstellationEdge(1, 2),
        ConstellationEdge(2, 3),
        ConstellationEdge(3, 4),
        ConstellationEdge(4, 5),
        ConstellationEdge(5, 6),
        ConstellationEdge(6, 7),
        ConstellationEdge(7, 8),
        ConstellationEdge(6, 9),
        ConstellationEdge(9, 10),
    ),
)
val PiscesStylizedTemplate = template(
    "Pisces",
    listOf(
        ConstellationNode(0.111f, 0.559f),
        ConstellationNode(0.193f, 0.740f),
        ConstellationNode(0.373f, 0.700f),
        ConstellationNode(0.567f, 0.812f),
        ConstellationNode(0.788f, 0.855f),
        ConstellationNode(0.692f, 0.702f),
        ConstellationNode(0.555f, 0.521f),
        ConstellationNode(0.680f, 0.355f),
        ConstellationNode(0.599f, 0.147f),
        ConstellationNode(0.793f, 0.106f),
        ConstellationNode(0.861f, 0.287f),
    ),
    listOf(
        ConstellationEdge(0, 1),
        ConstellationEdge(1, 2),
        ConstellationEdge(2, 3),
        ConstellationEdge(3, 4),
        ConstellationEdge(4, 5),
        ConstellationEdge(5, 6),
        ConstellationEdge(6, 7),
        ConstellationEdge(7, 8),
        ConstellationEdge(8, 9),
        ConstellationEdge(9, 10),
        ConstellationEdge(10, 7),
    ),
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
