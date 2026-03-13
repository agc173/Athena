package com.agc.bwitch.ui.guide

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.rotate
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
    val onSurface = MaterialTheme.colorScheme.onSurface
    val pendulumWeightColor = MaterialTheme.colorScheme.primary

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
        }
    }

    val isAnimating = state.phase == PendulumPhase.ANIMATING

    Column(
        modifier = Modifier
            .padding(contentPadding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("El Péndulo", style = MaterialTheme.typography.headlineMedium)
        Text(
            "Haz una pregunta o piénsala en silencio. Toca el péndulo para consultar.",
            style = MaterialTheme.typography.bodyMedium,
        )

        OutlinedTextField(
            value = state.question,
            onValueChange = viewModel::onQuestionChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Tu pregunta (opcional)") },
            enabled = !isAnimating,
        )

        Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
            PendulumAnswerBoard(selectedAnswer = if (state.phase == PendulumPhase.RESULT) state.selectedAnswer else null)
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .clickable(
                        enabled = !isAnimating,
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                    ) { viewModel.startSwing() },
            ) {
                val centerX = size.width / 2
                val topY = size.height * 0.10f
                val length = size.height * 0.50f

                rotate(degrees = angle.value, pivot = Offset(centerX, topY)) {
                    drawLine(
                        color = onSurface,
                        start = Offset(centerX, topY),
                        end = Offset(centerX, topY + length),
                        strokeWidth = 6f,
                    )
                    drawCircle(
                        color = pendulumWeightColor,
                        radius = 28f,
                        center = Offset(centerX, topY + length + 20f),
                    )
                }
            }
        }

        if (state.phase == PendulumPhase.RESULT) {
            state.selectedAnswer?.let { answer ->
                Text(
                    text = "El péndulo dice: ${answer.label()}",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
        }

        Button(
            onClick = viewModel::reset,
            enabled = !isAnimating,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Nueva pregunta")
        }
    }
}

@Composable
private fun PendulumAnswerBoard(selectedAnswer: PendulumAnswer?) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(280.dp)
            .padding(horizontal = 16.dp),
    ) {
        AnswerNode(
            text = "Sí",
            isSelected = selectedAnswer == PendulumAnswer.YES,
            modifier = Modifier.align(Alignment.TopCenter),
        )
        AnswerNode(
            text = "No",
            isSelected = selectedAnswer == PendulumAnswer.NO,
            modifier = Modifier.align(Alignment.CenterStart),
        )
        AnswerNode(
            text = "Tal vez",
            isSelected = selectedAnswer == PendulumAnswer.MAYBE,
            modifier = Modifier.align(Alignment.CenterEnd),
        )
        AnswerNode(
            text = "Aún no",
            isSelected = selectedAnswer == PendulumAnswer.NOT_NOW,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun AnswerNode(
    text: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
) {
    val selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
    val defaultContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
    val textColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant

    Box(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .background(if (isSelected) selectedContainerColor else defaultContainerColor)
            .padding(horizontal = 10.dp, vertical = 6.dp),
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = textColor,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
        )
    }
}

private fun PendulumAnswer?.toTargetAngle(): Float = when (this) {
    PendulumAnswer.YES -> 0f
    PendulumAnswer.NO -> -24f
    PendulumAnswer.MAYBE -> 24f
    PendulumAnswer.NOT_NOW -> 10f
    null -> 0f
}

private fun PendulumAnswer.label(): String = when (this) {
    PendulumAnswer.YES -> "SÍ"
    PendulumAnswer.NO -> "NO"
    PendulumAnswer.MAYBE -> "TAL VEZ"
    PendulumAnswer.NOT_NOW -> "AÚN NO"
}
