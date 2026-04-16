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
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.agc.bwitch.localization.HabitsStrings
import com.agc.bwitch.localization.appStrings
import com.agc.bwitch.presentation.rituals.HabitIntentionUiModel
import com.agc.bwitch.presentation.rituals.HabitsUiState
import com.agc.bwitch.presentation.rituals.HabitsViewModel
import com.agc.bwitch.ui.common.designsystem.BWitchCard
import com.agc.bwitch.ui.common.designsystem.BWitchScreen
import com.agc.bwitch.ui.common.designsystem.BWitchSectionHeader
import com.agc.bwitch.ui.rituals.components.HabitsProgressBadge
import org.koin.compose.koinInject

@Composable
fun HabitsScreen(
    contentPadding: PaddingValues,
    viewModel: HabitsViewModel = koinInject(),
) {
    val state by viewModel.uiState.collectAsState()
    val strings = appStrings.habits

    BWitchScreen(
        contentPadding = contentPadding,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        BWitchSectionHeader(
            title = strings.headerTitle,
            subtitle = strings.headerSubtitle,
        )

        if (state.isLoading) {
            BWitchCard {
                Text(
                    text = strings.loading,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        } else {
            HabitsProgressCard(state = state, strings = strings)

            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = strings.sectionTitle,
                    style = MaterialTheme.typography.titleMedium,
                )
                state.intentions.forEach { intention ->
                    HabitIntentionCard(
                        intention = intention,
                        strings = strings,
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
private fun HabitsProgressCard(
    state: HabitsUiState,
    strings: HabitsStrings,
) {
    val missing = (state.cycleTarget - state.progressPoints).coerceAtLeast(0)

    BWitchCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            HabitsProgressBadge(
                badgeType = state.activeBadgeType,
                currentPoints = state.progressPoints,
                cycleTarget = state.cycleTarget,
                glowLevel = state.glowLevel,
                modifier = Modifier.size(92.dp),
            )

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    text = strings.progressCompletedFormat.withInts(state.progressPoints),
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    text = strings.progressRemainingFormat.withInts(missing),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (state.completedCycles > 0) {
                    Text(
                        text = strings.completedCyclesFormat.withInts(state.completedCycles),
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
    strings: HabitsStrings,
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
                    text = strings.integratedLabel,
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
                        Text(strings.undoCta)
                    }
                } else {
                    Button(
                        onClick = onToggle,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(strings.addCta)
                    }
                }
            }
        }
    }
}

private fun String.withInts(vararg values: Int): String {
    var resolved = this
    values.forEach { value ->
        resolved = resolved.replaceFirst("%d", value.toString())
    }
    return resolved
}
