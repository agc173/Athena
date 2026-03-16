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
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.agc.bwitch.domain.pendulum.PendulumAnswer
import com.agc.bwitch.presentation.pendulum.PendulumPhase
import com.agc.bwitch.presentation.pendulum.PendulumViewModel
import kotlin.math.PI
import kotlin.math.atan2
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
                    animationSpec = tween(durationMillis = 4100, easing = LinearOutSlowInEasing),
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
    var boardSize by remember { mutableStateOf(IntSize.Zero) }
    val markerHorizontalHalfFactor = 0.14f
    val markerVerticalHalfFactor = 0.06f
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
                color = colorScheme.outlineVariant.copy(alpha = 0.52f),
                radius = boardRadius,
                center = center,
                style = Stroke(width = 2.4f),
            )
            drawCircle(
                color = colorScheme.primary.copy(alpha = 0.14f),
                radius = boardRadius * 0.70f,
                center = center,
                style = Stroke(width = 1.25f),
            )
            rotate(degrees = -14f, pivot = center) {
                drawArc(
                    color = colorScheme.primary.copy(alpha = 0.14f),
                    startAngle = 22f,
                    sweepAngle = 128f,
                    useCenter = false,
                    topLeft = Offset(center.x - boardRadius * 0.76f, center.y - boardRadius * 0.76f),
                    size = androidx.compose.ui.geometry.Size(boardRadius * 1.52f, boardRadius * 1.52f),
                    style = Stroke(width = 1.15f),
                )
                drawArc(
                    color = colorScheme.outlineVariant.copy(alpha = 0.16f),
                    startAngle = 210f,
                    sweepAngle = 108f,
                    useCenter = false,
                    topLeft = Offset(center.x - boardRadius * 0.52f, center.y - boardRadius * 0.52f),
                    size = androidx.compose.ui.geometry.Size(boardRadius * 1.04f, boardRadius * 1.04f),
                    style = Stroke(width = 1.05f),
                )
            }
        }

        AnswerMarker(
            text = "NO",
            isSelected = phase == PendulumPhase.RESULT && selectedAnswer == PendulumAnswer.NO,
            boardSize = boardSize,
            horizontalHalfFactor = markerHorizontalHalfFactor,
            verticalHalfFactor = markerVerticalHalfFactor,
            x = 0.27f,
            y = 0.26f,
        )
        AnswerMarker(
            text = "SÍ",
            isSelected = phase == PendulumPhase.RESULT && selectedAnswer == PendulumAnswer.YES,
            boardSize = boardSize,
            horizontalHalfFactor = markerHorizontalHalfFactor,
            verticalHalfFactor = markerVerticalHalfFactor,
            x = 0.73f,
            y = 0.26f,
        )
        AnswerMarker(
            text = "AÚN NO",
            isSelected = phase == PendulumPhase.RESULT && selectedAnswer == PendulumAnswer.NOT_NOW,
            boardSize = boardSize,
            horizontalHalfFactor = markerHorizontalHalfFactor,
            verticalHalfFactor = markerVerticalHalfFactor,
            x = 0.27f,
            y = 0.74f,
        )
        AnswerMarker(
            text = "TAL VEZ",
            isSelected = phase == PendulumPhase.RESULT && selectedAnswer == PendulumAnswer.MAYBE,
            boardSize = boardSize,
            horizontalHalfFactor = markerHorizontalHalfFactor,
            verticalHalfFactor = markerVerticalHalfFactor,
            x = 0.73f,
            y = 0.74f,
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
    boardSize: IntSize,
    horizontalHalfFactor: Float,
    verticalHalfFactor: Float,
    x: Float,
    y: Float,
) {
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        modifier = Modifier
            .offset {
                IntOffset(
                    x = (boardSize.width * (x - horizontalHalfFactor)).roundToInt(),
                    y = (boardSize.height * (y - verticalHalfFactor)).roundToInt(),
                )
            }
            .size(width = 92.dp, height = 38.dp),
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

    val t = animationProgress.coerceIn(0f, 1f)
    val orbitCutoff = 0.70f
    val settleCutoff = 0.88f
    val preferredTarget = target * 0.82f

    return if (t <= orbitCutoff) {
        orbitPosition(t = t / orbitCutoff, boardRadius = boardRadius)
    } else if (t <= settleCutoff) {
        val local = ((t - orbitCutoff) / (settleCutoff - orbitCutoff)).coerceIn(0f, 1f)
        val eased = 1f - (1f - local).pow(2.5f)
        val approach = lerpOffset(
            start = orbitPosition(1f, boardRadius),
            end = preferredTarget,
            t = eased,
        )
        val targetAngle = atan2(target.y, target.x)
        val swirlRadius = boardRadius * 0.095f * (1f - local).pow(1.35f)
        approach + Offset(
            x = cos((local * 2.2f * 2f * PI).toFloat() + targetAngle) * swirlRadius,
            y = sin((local * 2.2f * 2f * PI).toFloat() + targetAngle) * swirlRadius,
        )
    } else {
        val local = ((t - settleCutoff) / (1f - settleCutoff)).coerceIn(0f, 1f)
        val eased = 1f - (1f - local).pow(3.6f)
        val settle = lerpOffset(
            start = preferredTarget,
            end = target,
            t = eased,
        )
        val targetAngle = atan2(target.y, target.x)
        val driftRadius = boardRadius * 0.028f * (1f - eased).pow(1.6f)
        settle + Offset(
            x = cos((local * 1.25f * 2f * PI).toFloat() + targetAngle + 0.45f) * driftRadius,
            y = sin((local * 1.25f * 2f * PI).toFloat() + targetAngle + 0.45f) * driftRadius,
        )
    }
}

private fun orbitPosition(t: Float, boardRadius: Float): Offset {
    val easedT = t.coerceIn(0f, 1f)
    val loops = 2.85f
    val angleBase = easedT * loops * 2f * PI
    val angleWobble = sin(easedT * 2.4f * PI) * 0.34f
    val angle = (angleBase + angleWobble).toFloat()

    val contraction = easedT.pow(1.28f)
    val startRadius = boardRadius * 0.44f
    val endRadius = boardRadius * 0.16f
    val radius = startRadius - (startRadius - endRadius) * contraction
    val noiseX = cos((easedT * 6.3f * PI).toFloat() + 0.8f) * boardRadius * 0.028f * (1f - easedT)
    val noiseY = sin((easedT * 5.1f * PI).toFloat() - 0.45f) * boardRadius * 0.024f * (1f - easedT)

    return Offset(
        x = cos(angle) * radius + noiseX,
        y = sin(angle) * radius + noiseY,
    )
}

private fun answerTarget(answer: PendulumAnswer?, boardRadius: Float): Offset {
    val diagonal = boardRadius * 0.56f
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
