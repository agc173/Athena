package com.agc.bwitch.ui.guide

import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.agc.bwitch.domain.pendulum.PendulumAnswer
import com.agc.bwitch.presentation.pendulum.PendulumPhase
import com.agc.bwitch.presentation.pendulum.PendulumViewModel
import org.koin.compose.koinInject

@Composable
fun PendulumScreen(
    contentPadding: PaddingValues,
    viewModel: PendulumViewModel = koinInject(),
) {
    val state by viewModel.uiState.collectAsState()
    val angle = remember { Animatable(0f) }
    val colorScheme = MaterialTheme.colorScheme

    LaunchedEffect(state.phase, state.selectedAnswer) {
        if (state.phase == PendulumPhase.ANIMATING) {
            angle.snapTo(0f)
            angle.animateTo(35f, animationSpec = tween(durationMillis = 260))
            angle.animateTo(-35f, animationSpec = tween(durationMillis = 260))
            angle.animateTo(28f, animationSpec = tween(durationMillis = 220))
            angle.animateTo(-24f, animationSpec = tween(durationMillis = 220))
            angle.animateTo(18f, animationSpec = tween(durationMillis = 180))
            angle.animateTo(-14f, animationSpec = tween(durationMillis = 180))
            angle.animateTo(
                targetValue = state.selectedAnswer.toTargetAngle(),
                animationSpec = tween(durationMillis = 580),
            )
            viewModel.onSwingFinished()
        } else if (state.phase == PendulumPhase.IDLE) {
            angle.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 260),
            )
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
            "Haz una pregunta o piénsala en silencio. Toca el péndulo para consultar.",
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
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                PendulumAnswerBoard(selectedAnswer = if (state.phase == PendulumPhase.RESULT) state.selectedAnswer else null)
                Canvas(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            enabled = !isAnimating,
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                        ) { viewModel.startSwing() },
                ) {
                    val centerX = size.width / 2
                    val topY = size.height * 0.10f
                    val length = size.height * 0.50f

                    drawCircle(
                        color = colorScheme.primary.copy(alpha = 0.08f),
                        radius = 18f,
                        center = Offset(centerX, topY),
                    )

                    rotate(degrees = angle.value, pivot = Offset(centerX, topY)) {
                        drawLine(
                            color = colorScheme.onSurface.copy(alpha = 0.86f),
                            start = Offset(centerX, topY + 8f),
                            end = Offset(centerX, topY + length),
                            strokeWidth = 3.2f,
                            cap = StrokeCap.Round,
                        )

                        drawCircle(
                            color = colorScheme.onSurfaceVariant,
                            radius = 7f,
                            center = Offset(centerX, topY),
                        )

                        val crystalCenter = Offset(centerX, topY + length + 24f)
                        drawMysticCrystal(
                            center = crystalCenter,
                            size = 52f,
                            primaryColor = colorScheme.primary,
                            glowColor = colorScheme.primary.copy(alpha = 0.20f),
                        )
                    }
                }
            }
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
private fun PendulumAnswerBoard(selectedAnswer: PendulumAnswer?) {
    val colorScheme = MaterialTheme.colorScheme

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .padding(horizontal = 14.dp)
            .drawBehind {
                val arcTop = size.height * 0.48f
                drawArc(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            colorScheme.primary.copy(alpha = 0.06f),
                            Color.Transparent,
                        ),
                        center = Offset(size.width / 2f, arcTop + 30f),
                        radius = size.width * 0.48f,
                    ),
                    startAngle = 200f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(size.width * 0.17f, arcTop),
                    size = Size(size.width * 0.66f, size.height * 0.50f),
                )
            },
    ) {
        AnswerNode(
            text = "No",
            isSelected = selectedAnswer == PendulumAnswer.NO,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .offset(x = 10.dp, y = (-92).dp),
        )
        AnswerNode(
            text = "Aún no",
            isSelected = selectedAnswer == PendulumAnswer.NOT_NOW,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(x = (-74).dp, y = (-28).dp),
        )
        AnswerNode(
            text = "Tal vez",
            isSelected = selectedAnswer == PendulumAnswer.MAYBE,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(x = 74.dp, y = (-28).dp),
        )
        AnswerNode(
            text = "Sí",
            isSelected = selectedAnswer == PendulumAnswer.YES,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .offset(x = (-10).dp, y = (-92).dp),
        )
    }
}

@Composable
private fun AnswerNode(
    text: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
) {
    val colorScheme = MaterialTheme.colorScheme
    val borderColor = if (isSelected) colorScheme.primary.copy(alpha = 0.70f) else colorScheme.outlineVariant.copy(alpha = 0.55f)
    val background = if (isSelected) {
        Brush.verticalGradient(
            colors = listOf(
                colorScheme.primary.copy(alpha = 0.22f),
                colorScheme.primary.copy(alpha = 0.10f),
            ),
        )
    } else {
        Brush.verticalGradient(
            colors = listOf(
                colorScheme.surface.copy(alpha = 0.58f),
                colorScheme.surfaceVariant.copy(alpha = 0.45f),
            ),
        )
    }

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = Color.Transparent,
        border = BorderStroke(1.dp, borderColor),
        tonalElevation = if (isSelected) 2.dp else 0.dp,
        shadowElevation = if (isSelected) 2.dp else 0.dp,
    ) {
        Box(
            modifier = Modifier
                .background(background)
                .padding(horizontal = 12.dp, vertical = 8.dp),
        ) {
            Text(
                text = text.uppercase(),
                style = MaterialTheme.typography.labelLarge,
                color = if (isSelected) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            )
        }
    }
}


private fun DrawScope.drawMysticCrystal(
    center: Offset,
    size: Float,
    primaryColor: Color,
    glowColor: Color,
) {
    drawCircle(
        color = glowColor,
        radius = size * 0.78f,
        center = center,
    )

    val half = size / 2f
    val diamond = Path().apply {
        moveTo(center.x, center.y - half)
        lineTo(center.x + half * 0.62f, center.y)
        lineTo(center.x, center.y + half)
        lineTo(center.x - half * 0.62f, center.y)
        close()
    }

    drawPath(
        path = diamond,
        brush = Brush.verticalGradient(
            colors = listOf(
                primaryColor.copy(alpha = 0.96f),
                primaryColor.copy(alpha = 0.74f),
            ),
            startY = center.y - half,
            endY = center.y + half,
        ),
    )

    drawLine(
        color = Color.White.copy(alpha = 0.45f),
        start = Offset(center.x, center.y - half * 0.66f),
        end = Offset(center.x, center.y + half * 0.62f),
        strokeWidth = 2f,
        cap = StrokeCap.Round,
    )
}

private fun PendulumAnswer?.toTargetAngle(): Float = when (this) {
    PendulumAnswer.NO -> 28f
    PendulumAnswer.NOT_NOW -> 10f
    PendulumAnswer.MAYBE -> -10f
    PendulumAnswer.YES -> -28f
    null -> 0f
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
