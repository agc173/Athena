package com.agc.bwitch.ui.guide

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.agc.bwitch.domain.pendulum.PendulumAnswer
import com.agc.bwitch.presentation.pendulum.PendulumPhase
import com.agc.bwitch.presentation.pendulum.PendulumViewModel
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import org.koin.compose.koinInject

@Composable
fun PendulumScreen(
    contentPadding: PaddingValues,
    viewModel: PendulumViewModel = koinInject(),
) {
    val state by viewModel.uiState.collectAsState()
    val orbitProgress = remember { Animatable(0f) }
    val colorScheme = MaterialTheme.colorScheme

    LaunchedEffect(state.phase, state.selectedAnswer) {
        when (state.phase) {
            PendulumPhase.ANIMATING -> {
                orbitProgress.snapTo(0f)
                orbitProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 3000, easing = LinearOutSlowInEasing),
                )
                viewModel.onSwingFinished()
            }

            PendulumPhase.IDLE -> orbitProgress.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
            )

            PendulumPhase.RESULT -> Unit
        }
    }

    val isAnimating = state.phase == PendulumPhase.ANIMATING

    Column(
        modifier = Modifier
            .padding(contentPadding)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text(
            "El Péndulo",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            "Haz una pregunta o piénsala en silencio. Toca el tablero para consultar.",
            style = MaterialTheme.typography.bodyMedium,
            color = colorScheme.onSurfaceVariant,
        )

        OutlinedTextField(
            value = state.question,
            onValueChange = viewModel::onQuestionChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Tu pregunta (opcional)") },
            enabled = !isAnimating,
        )

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            tonalElevation = 3.dp,
            shadowElevation = 1.dp,
            border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.45f)),
            color = colorScheme.surfaceVariant.copy(alpha = 0.28f),
        ) {
            PendulumBoard(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(380.dp)
                    .padding(horizontal = 14.dp, vertical = 12.dp)
                    .clickable(
                        enabled = !isAnimating,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { viewModel.startSwing() },
                phase = state.phase,
                selectedAnswer = state.selectedAnswer,
                animationProgress = orbitProgress.value,
            )
        }

        if (state.phase == PendulumPhase.RESULT) {
            state.selectedAnswer?.let { answer ->
                Text(
                    text = answer.mysticalMessage(),
                    style = MaterialTheme.typography.titleMedium,
                    fontStyle = FontStyle.Italic,
                )
                Text(
                    text = "El péndulo ha marcado: ${answer.label()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (state.phase == PendulumPhase.RESULT) {
            Button(
                onClick = viewModel::reset,
                enabled = !isAnimating,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Nueva pregunta")
            }
        }
    }
}

@Composable
private fun PendulumBoard(
    modifier: Modifier,
    phase: PendulumPhase,
    selectedAnswer: PendulumAnswer?,
    animationProgress: Float,
) {
    val colorScheme = MaterialTheme.colorScheme
    val density = LocalDensity.current
    var boardSize by remember { mutableStateOf(IntSize.Zero) }
    val markerWidth: Dp = 92.dp
    val markerHeight: Dp = 38.dp
    val markerWidthPx = with(density) { markerWidth.toPx() }
    val markerHeightPx = with(density) { markerHeight.toPx() }
    val boardRadiusPx = min(boardSize.width.toFloat(), boardSize.height.toFloat()) * 0.40f
    val crystalOffsetPx = crystalOffsetFor(
        phase = phase,
        selectedAnswer = selectedAnswer,
        animationProgress = animationProgress,
        boardRadius = boardRadiusPx,
    )

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .onSizeChanged { boardSize = it },
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val boardRadius = min(size.width, size.height) * 0.40f

            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        colorScheme.primary.copy(alpha = 0.18f),
                        colorScheme.surface.copy(alpha = 0.08f),
                        Color.Transparent,
                    ),
                    center = center,
                    radius = boardRadius * 1.25f,
                ),
                radius = boardRadius * 1.25f,
                center = center,
            )

            drawCircle(
                color = colorScheme.surface.copy(alpha = 0.44f),
                radius = boardRadius,
                center = center,
            )
            drawCircle(
                color = colorScheme.outlineVariant.copy(alpha = 0.65f),
                radius = boardRadius,
                center = center,
                style = Stroke(width = 2.8f),
            )
            drawCircle(
                color = colorScheme.primary.copy(alpha = 0.18f),
                radius = boardRadius * 0.72f,
                center = center,
                style = Stroke(width = 1.6f),
            )
            drawCircle(
                color = colorScheme.primary.copy(alpha = 0.10f),
                radius = boardRadius * 0.36f,
                center = center,
                style = Stroke(width = 1.2f),
            )
        }

        AnswerMarker(
            text = "NO",
            isSelected = phase == PendulumPhase.RESULT && selectedAnswer == PendulumAnswer.NO,
            markerWidth = markerWidth,
            markerHeight = markerHeight,
            markerWidthPx = markerWidthPx,
            markerHeightPx = markerHeightPx,
            boardSize = boardSize,
            x = 0.23f,
            y = 0.22f,
        )
        AnswerMarker(
            text = "SÍ",
            isSelected = phase == PendulumPhase.RESULT && selectedAnswer == PendulumAnswer.YES,
            markerWidth = markerWidth,
            markerHeight = markerHeight,
            markerWidthPx = markerWidthPx,
            markerHeightPx = markerHeightPx,
            boardSize = boardSize,
            x = 0.77f,
            y = 0.22f,
        )
        AnswerMarker(
            text = "AÚN NO",
            isSelected = phase == PendulumPhase.RESULT && selectedAnswer == PendulumAnswer.NOT_NOW,
            markerWidth = markerWidth,
            markerHeight = markerHeight,
            markerWidthPx = markerWidthPx,
            markerHeightPx = markerHeightPx,
            boardSize = boardSize,
            x = 0.23f,
            y = 0.78f,
        )
        AnswerMarker(
            text = "TAL VEZ",
            isSelected = phase == PendulumPhase.RESULT && selectedAnswer == PendulumAnswer.MAYBE,
            markerWidth = markerWidth,
            markerHeight = markerHeight,
            markerWidthPx = markerWidthPx,
            markerHeightPx = markerHeightPx,
            boardSize = boardSize,
            x = 0.77f,
            y = 0.78f,
        )

        Canvas(
            modifier = Modifier
                .align(Alignment.Center)
                .offset {
                    IntOffset(
                        x = crystalOffsetPx.x.roundToInt(),
                        y = crystalOffsetPx.y.roundToInt(),
                    )
                }
                .size(56.dp),
        ) {
            drawMysticCrystal(
                center = Offset(size.width / 2f, size.height / 2f),
                size = size.minDimension * 0.72f,
                primaryColor = colorScheme.primary,
                glowColor = colorScheme.primary.copy(alpha = 0.26f),
            )
        }
    }
}

@Composable
private fun AnswerMarker(
    text: String,
    isSelected: Boolean,
    markerWidth: Dp,
    markerHeight: Dp,
    markerWidthPx: Float,
    markerHeightPx: Float,
    boardSize: IntSize,
    x: Float,
    y: Float,
) {
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = (boardSize.width * x - markerWidthPx / 2f).roundToInt(),
                    y = (boardSize.height * y - markerHeightPx / 2f).roundToInt(),
                )
            }
            .size(width = markerWidth, height = markerHeight),
        shape = MaterialTheme.shapes.small,
        color = if (isSelected) colorScheme.primary.copy(alpha = 0.20f) else colorScheme.surface.copy(alpha = 0.30f),
        border = BorderStroke(
            width = 1.dp,
            color = if (isSelected) colorScheme.primary.copy(alpha = 0.92f) else colorScheme.outlineVariant.copy(alpha = 0.58f),
        ),
        tonalElevation = if (isSelected) 1.dp else 0.dp,
        shadowElevation = if (isSelected) 3.dp else 0.dp,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    if (isSelected) {
                        Brush.verticalGradient(
                            listOf(
                                colorScheme.primary.copy(alpha = 0.30f),
                                colorScheme.primary.copy(alpha = 0.12f),
                            ),
                        )
                    } else {
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                colorScheme.surfaceVariant.copy(alpha = 0.12f),
                            ),
                        )
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun crystalOffsetFor(
    phase: PendulumPhase,
    selectedAnswer: PendulumAnswer?,
    animationProgress: Float,
    boardRadius: Float,
): Offset {
    val target = answerTarget(selectedAnswer, boardRadius)

    if (phase == PendulumPhase.IDLE || selectedAnswer == null) return Offset.Zero
    if (phase == PendulumPhase.RESULT) return target

    val orbitCutoff = 0.78f
    val t = animationProgress.coerceIn(0f, 1f)
    return if (t <= orbitCutoff) {
        orbitPosition(t = t / orbitCutoff, boardRadius = boardRadius)
    } else {
        val local = ((t - orbitCutoff) / (1f - orbitCutoff)).coerceIn(0f, 1f)
        val eased = 1f - (1f - local).pow(3)
        lerpOffset(
            start = orbitPosition(1f, boardRadius),
            end = target,
            t = eased,
        )
    }
}

private fun orbitPosition(t: Float, boardRadius: Float): Offset {
    val loops = 2.35f
    val angle = (t * loops * 2f * PI).toFloat()
    val startRadius = boardRadius * 0.46f
    val endRadius = boardRadius * 0.22f
    val radius = startRadius - (startRadius - endRadius) * t
    return Offset(
        x = cos(angle) * radius,
        y = sin(angle) * radius,
    )
}

private fun answerTarget(answer: PendulumAnswer?, boardRadius: Float): Offset {
    val diagonal = boardRadius * 0.62f
    return when (answer) {
        PendulumAnswer.NO -> Offset(-diagonal, -diagonal)
        PendulumAnswer.YES -> Offset(diagonal, -diagonal)
        PendulumAnswer.NOT_NOW -> Offset(-diagonal, diagonal)
        PendulumAnswer.MAYBE -> Offset(diagonal, diagonal)
        null -> Offset.Zero
    }
}

private fun lerpOffset(start: Offset, end: Offset, t: Float): Offset = Offset(
    x = start.x + (end.x - start.x) * t,
    y = start.y + (end.y - start.y) * t,
)

private fun DrawScope.drawMysticCrystal(
    center: Offset,
    size: Float,
    primaryColor: Color,
    glowColor: Color,
) {
    drawCircle(
        color = glowColor,
        radius = size * 0.88f,
        center = center,
    )

    val half = size / 2f
    val diamond = Path().apply {
        moveTo(center.x, center.y - half)
        lineTo(center.x + half * 0.65f, center.y)
        lineTo(center.x, center.y + half)
        lineTo(center.x - half * 0.65f, center.y)
        close()
    }

    drawPath(
        path = diamond,
        brush = Brush.verticalGradient(
            colors = listOf(
                primaryColor.copy(alpha = 0.96f),
                primaryColor.copy(alpha = 0.72f),
            ),
            startY = center.y - half,
            endY = center.y + half,
        ),
    )

    drawLine(
        color = Color.White.copy(alpha = 0.45f),
        start = Offset(center.x, center.y - half * 0.62f),
        end = Offset(center.x, center.y + half * 0.58f),
        strokeWidth = 2f,
        cap = StrokeCap.Round,
    )
}

private fun PendulumAnswer.label(): String = when (this) {
    PendulumAnswer.YES -> "SÍ"
    PendulumAnswer.NO -> "NO"
    PendulumAnswer.MAYBE -> "TAL VEZ"
    PendulumAnswer.NOT_NOW -> "AÚN NO"
}

private fun PendulumAnswer.mysticalMessage(): String = when (this) {
    PendulumAnswer.YES -> "El destino parece favorable"
    PendulumAnswer.NO -> "El destino parece desfavorable"
    PendulumAnswer.MAYBE -> "Las señales aún son inciertas"
    PendulumAnswer.NOT_NOW -> "Todavía no es el momento"
}
