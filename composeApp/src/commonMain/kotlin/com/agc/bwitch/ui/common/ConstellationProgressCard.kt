package com.agc.bwitch.ui.common

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.agc.bwitch.domain.astrology.horoscope.ConstellationProgressRules
import com.agc.bwitch.domain.astrology.horoscope.ZodiacSign

data class ConstellationNode(val x: Float, val y: Float)

data class ConstellationEdge(val from: Int, val to: Int)

sealed interface RevealStep {
    data class Node(val index: Int) : RevealStep
    data class Edge(val index: Int) : RevealStep
}

data class ConstellationTemplate(
    val sign: ZodiacSign,
    val name: String,
    val nodes: List<ConstellationNode>,
    val edges: List<ConstellationEdge>,
    val revealSteps: List<RevealStep>,
) {
    val totalSteps: Int get() = revealSteps.size
}

private fun template(sign: ZodiacSign, name: String, nodes: List<ConstellationNode>, edges: List<ConstellationEdge>) =
    ConstellationTemplate(sign = sign, name = name, nodes = nodes, edges = edges, revealSteps = buildRevealSteps(nodes.size, edges))

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
    ZodiacSign.aries,
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
    ZodiacSign.taurus,
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
    ZodiacSign.gemini,
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
    ZodiacSign.cancer,
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
    ZodiacSign.leo,
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
    ZodiacSign.virgo,
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
    ZodiacSign.libra,
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
    ZodiacSign.scorpio,
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
    ZodiacSign.sagittarius,
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
    ZodiacSign.capricorn,
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
    ZodiacSign.aquarius,
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
    ZodiacSign.pisces,
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

private val zodiacTemplatesBySign: Map<ZodiacSign, ConstellationTemplate> = listOf(
    AriesSimplifiedTemplate, TaurusStylizedTemplate, GeminiStylizedTemplate, CancerStylizedTemplate, LeoStylizedTemplate, VirgoStylizedTemplate,
    LibraStylizedTemplate, ScorpioStylizedTemplate, SagittariusStylizedTemplate, CapricornStylizedTemplate, AquariusStylizedTemplate, PiscesStylizedTemplate,
).associateBy(ConstellationTemplate::sign).also { templatesBySign ->
    require(templatesBySign.size == ConstellationProgressRules.zodiacOrder.size) { "Duplicate templates by zodiac sign." }
    require(ConstellationProgressRules.zodiacOrder.all(templatesBySign::containsKey)) { "Missing zodiac template for configured order." }
}

val ZodiacStylizedTemplates: List<ConstellationTemplate> = ConstellationProgressRules.zodiacOrder.map { sign ->
    zodiacTemplatesBySign.getValue(sign)
}

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

    val isComplete = activeCount == totalSteps && totalSteps > 0
    val isPartial = activeCount in 1 until totalSteps
    val inactiveLine = Color(0xFF5F6A88).copy(alpha = 0.52f)
    val inactiveNode = Color(0xFF90A2C0).copy(alpha = 0.56f)
    val activeLine = Color(0xFFF4CB7D).copy(alpha = 0.98f)
    val activeNode = Color(0xFFFFE2A2)
    val nameColor = when {
        isComplete -> Color(0xFFFFE5A6).copy(alpha = 0.96f)
        isPartial -> Color(0xFFF4DDA9).copy(alpha = 0.92f)
        else -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.78f)
    }
    val glowBrush = when {
        isComplete -> Brush.radialGradient(listOf(Color(0x66FFD88A), Color.Transparent))
        isPartial -> Brush.radialGradient(listOf(Color(0x33F4CB7D), Color.Transparent))
        else -> Brush.radialGradient(listOf(Color(0x155A6F9C), Color.Transparent))
    }

    Box(modifier = modifier) {
        Canvas(modifier = Modifier.matchParentSize()) {
            drawCircle(
                brush = glowBrush,
                radius = size.minDimension * 0.72f,
                center = Offset(size.width / 2f, size.height * 0.52f),
            )
        }
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = template.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                color = nameColor,
            )
            Canvas(modifier = Modifier.fillMaxWidth().aspectRatio(1.05f)) {
                val scaledPoints = template.nodes.map { Offset(it.x * size.width, it.y * size.height) }
                template.edges.forEachIndexed { lineIndex, edge ->
                    drawLine(color = inactiveLine, start = scaledPoints[edge.from], end = scaledPoints[edge.to], strokeWidth = 13f, cap = StrokeCap.Round)
                    if (lineIndex in revealedEdgeIndexes) drawLine(color = activeLine, start = scaledPoints[edge.from], end = scaledPoints[edge.to], strokeWidth = 15f, cap = StrokeCap.Round)
                }
                scaledPoints.forEachIndexed { index, point ->
                    drawCircle(color = inactiveNode.copy(alpha = 0.12f * pulse), radius = 36f, center = point, style = Stroke(width = 4f))
                    drawCircle(color = inactiveNode, radius = 20f, center = point)
                    if (index in revealedNodeIndexes) {
                        val baseGlow = when {
                            isComplete -> 0.45f
                            isPartial -> 0.22f
                            else -> 0.08f
                        }
                        val glowAlpha = if (lastRevealedNodeIndex == index) pulse else 0.85f
                        drawCircle(color = activeNode.copy(alpha = baseGlow * glowAlpha), radius = 44f, center = point)
                        drawCircle(color = activeNode.copy(alpha = glowAlpha), radius = 26f, center = point)
                    }
                }
            }
        }
    }
}
