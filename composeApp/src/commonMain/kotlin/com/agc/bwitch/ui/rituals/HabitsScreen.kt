package com.agc.bwitch.ui.rituals

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.Crossfade
import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.agc.bwitch.presentation.rituals.HabitIntentionUiModel
import com.agc.bwitch.presentation.rituals.HabitsUiState
import com.agc.bwitch.presentation.rituals.HabitsViewModel
import com.agc.bwitch.ui.common.designsystem.BWitchCard
import com.agc.bwitch.ui.common.designsystem.BWitchScreen
import com.agc.bwitch.ui.common.designsystem.BWitchSectionHeader
import com.agc.bwitch.ui.rituals.components.HabitsProgressRing
import org.koin.compose.koinInject

@Composable
fun HabitsScreen(
    contentPadding: PaddingValues,
    viewModel: HabitsViewModel = koinInject(),
) {
    val state by viewModel.uiState.collectAsState()

    BWitchScreen(
        contentPadding = contentPadding,
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        BWitchSectionHeader(
            title = "Hábitos",
            subtitle = "Pequeñas acciones que transforman tu energía",
        )

        if (state.isLoading) {
            BWitchCard {
                Text(
                    text = "Preparando tus intenciones de hoy…",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            HabitsProgressCard(state = state)

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Intenciones del día",
                    style = MaterialTheme.typography.titleMedium,
                )
                state.intentions.forEach { intention ->
                    HabitIntentionCard(
                        intention = intention,
                        onToggle = {
                            viewModel.onToggleIntention(
                                intentionId = intention.id,
                                completed = intention.isCompleted,
                            )
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun HabitsProgressCard(state: HabitsUiState) {
    val missing = (state.cycleTarget - state.progressPoints).coerceAtLeast(0)

    BWitchCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HabitsProgressRing(
                current = state.progressPoints,
                target = state.cycleTarget,
                glowLevel = state.glowLevel,
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = "Has cultivado ${state.progressPoints} acciones en este ciclo",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = "Te faltan $missing para completar este ciclo",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (state.completedCycles > 0) {
                    Text(
                        text = "Ciclos completados: ${state.completedCycles}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }
    }
}

@Composable
private fun HabitIntentionCard(
    intention: HabitIntentionUiModel,
    onToggle: () -> Unit,
) {
    val isCompleted = intention.isCompleted
    val accentShape = MaterialTheme.shapes.small
    val accentBorderColor by animateColorAsState(
        targetValue = if (isCompleted) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.44f)
        } else {
            MaterialTheme.colorScheme.outline.copy(alpha = 0.18f)
        },
        label = "habitIntentionBorder",
    )
    val accentContainerColor by animateColorAsState(
        targetValue = if (isCompleted) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
        } else {
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)
        },
        label = "habitIntentionContainer",
    )
    val actionCopyAlpha by animateFloatAsState(
        targetValue = if (isCompleted) 0.85f else 1f,
        label = "habitIntentionActionAlpha",
    )

    BWitchCard {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(accentShape)
                .background(accentContainerColor)
                .border(width = 1.dp, color = accentBorderColor, shape = accentShape)
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(text = intention.title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = intention.actionText,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.alpha(actionCopyAlpha),
            )

            AnimatedVisibility(
                visible = isCompleted,
                enter = fadeIn(),
                exit = fadeOut(),
            ) {
                Text(
                    text = "Integrada en tu práctica ✦",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }

            Crossfade(
                targetState = isCompleted,
                label = "habitIntentionActionButton",
            ) { completed ->
                if (completed) {
                    OutlinedButton(
                        onClick = onToggle,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Deshacer por ahora")
                    }
                } else {
                    Button(
                        onClick = onToggle,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Sumar a mi práctica")
                    }
                }
            }
        }
    }
}
