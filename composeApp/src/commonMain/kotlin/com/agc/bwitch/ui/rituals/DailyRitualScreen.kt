package com.agc.bwitch.ui.rituals

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.agc.bwitch.domain.rituals.i18n.DailyRitualContentRepository
import com.agc.bwitch.domain.localization.AppLanguage
import com.agc.bwitch.domain.rituals.DailyRitualStep
import com.agc.bwitch.domain.rituals.DailyRitualStepKind
import com.agc.bwitch.domain.rituals.DailyRitualTemplate
import com.agc.bwitch.domain.security.InputPolicy
import com.agc.bwitch.localization.DailyRitualStrings
import com.agc.bwitch.localization.appStrings
import com.agc.bwitch.presentation.localization.AppLanguageViewModel
import com.agc.bwitch.presentation.rituals.DailyRitualError
import com.agc.bwitch.presentation.rituals.DailyRitualUiState
import com.agc.bwitch.presentation.rituals.DailyRitualViewModel
import com.agc.bwitch.ui.common.designsystem.BWitchCard
import com.agc.bwitch.ui.common.designsystem.BWitchScreen
import com.agc.bwitch.ui.common.designsystem.BWitchSectionHeader
import org.koin.compose.koinInject

@Composable
fun DailyRitualScreen(
    contentPadding: PaddingValues,
    onBack: () -> Unit,
    viewModel: DailyRitualViewModel = koinInject(),
    appLanguageViewModel: AppLanguageViewModel = koinInject(),
) {
    val strings = appStrings.dailyRitual
    val state by viewModel.uiState.collectAsState()
    val languageState by appLanguageViewModel.uiState.collectAsState()

    LaunchedEffect(viewModel) {
        viewModel.onScreenVisible()
    }

    BWitchScreen(
        contentPadding = contentPadding,
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        when {
            state.isLoading -> {
                Text(strings.loading, style = MaterialTheme.typography.bodyMedium)
            }

            state.isCompleted -> {
                CompletionBlock(
                    state = state,
                    strings = strings,
                    language = languageState.currentLanguage,
                    onBack = onBack,
                )
            }

            state.hasStarted -> {
                StepFlowBlock(
                    state = state,
                    strings = strings,
                    language = languageState.currentLanguage,
                    onTextChanged = viewModel::onTextAnswerChange,
                    onOptionSelected = viewModel::onOptionSelected,
                    onContinue = viewModel::onContinue,
                )
            }

            else -> {
                IntroBlock(
                    state = state,
                    strings = strings,
                    language = languageState.currentLanguage,
                    onStart = viewModel::onStartRitual,
                )
            }
        }
    }
}

@Composable
private fun IntroBlock(
    state: DailyRitualUiState,
    strings: DailyRitualStrings,
    language: AppLanguage,
    onStart: () -> Unit,
) {
    val ritual = state.ritual ?: return

    BWitchSectionHeader(
        title = ritual.localizedTitle(language),
        subtitle = ritual.localizedSubtitle(language),
    )

    BWitchCard {
        Text(text = ritual.localizedIntro(language), style = MaterialTheme.typography.bodyLarge)
        Text(
            text = strings.durationFormat.withInts(ritual.estimatedMinutes),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (state.streakCount > 0) {
            Text(
                text = strings.streakFormat.withInts(state.streakCount),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
        }
        Button(
            onClick = onStart,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(strings.startCta)
        }
    }
}

@Composable
private fun StepFlowBlock(
    state: DailyRitualUiState,
    strings: DailyRitualStrings,
    language: AppLanguage,
    onTextChanged: (String) -> Unit,
    onOptionSelected: (String) -> Unit,
    onContinue: () -> Unit,
) {
    val step = state.currentSteps.getOrNull(state.currentStepIndex) ?: return

    Text(
        text = strings.stepProgressFormat.withInts(state.currentStepIndex + 1, state.currentSteps.size),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )

    BWitchCard {
        DailyRitualStepContent(
            step = step,
            strings = strings,
            language = language,
            textAnswer = state.textAnswer,
            selectedOptionKey = state.selectedOptionKey,
            onTextChanged = onTextChanged,
            onOptionSelected = onOptionSelected,
        )

        state.error?.let { error ->
            Text(
                text = error.toLocalizedMessage(strings),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }

        Button(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(step.localizedCtaLabel(language))
        }
    }
}

@Composable
private fun CompletionBlock(
    state: DailyRitualUiState,
    strings: DailyRitualStrings,
    language: AppLanguage,
    onBack: () -> Unit,
) {
    val ritual = state.ritual ?: return

    BWitchSectionHeader(
        title = strings.completedTitle,
        subtitle = ritual.localizedSubtitle(language),
    )

    BWitchCard {
        Text(text = ritual.localizedCompletionMessage(language), style = MaterialTheme.typography.bodyLarge)
        Text(
            text = strings.currentStreakSentenceFormat.withInts(state.streakCount),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )
        Button(
            onClick = onBack,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(strings.backCta)
        }
    }
}

@Composable
private fun DailyRitualStepContent(
    step: DailyRitualStep,
    strings: DailyRitualStrings,
    language: AppLanguage,
    textAnswer: String,
    selectedOptionKey: String?,
    onTextChanged: (String) -> Unit,
    onOptionSelected: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(text = step.localizedText(language), style = MaterialTheme.typography.bodyLarge)

        when (step.kind) {
            DailyRitualStepKind.Info,
            DailyRitualStepKind.Confirmation,
            -> Unit

            DailyRitualStepKind.TextInput -> {
                OutlinedTextField(
                    value = textAnswer,
                    onValueChange = onTextChanged,
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text(strings.inputPlaceholder) },
                    singleLine = false,
                    maxLines = 3,
                )
                Text(
                    text = "${textAnswer.length}/${InputPolicy.DAILY_RITUAL_TEXT_MAX_LENGTH}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            DailyRitualStepKind.SingleChoice,
            DailyRitualStepKind.BinaryChoice,
            -> {
                ChoiceGroup(
                    step = step,
                    language = language,
                    selectedOptionKey = selectedOptionKey,
                    onOptionSelected = onOptionSelected,
                )
            }
        }
    }
}

@Composable
private fun ChoiceGroup(
    step: DailyRitualStep,
    language: AppLanguage,
    selectedOptionKey: String?,
    onOptionSelected: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        step.optionKeys.forEach { optionKey ->
            val optionLabel = step.localizedOption(optionKey = optionKey, language = language)
            val isSelected = optionKey == selectedOptionKey
            if (isSelected) {
                Surface(
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f),
                ) {
                    Text(
                        text = optionLabel,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            } else {
                AssistChip(
                    onClick = { onOptionSelected(optionKey) },
                    label = { Text(optionLabel) },
                )
            }
        }
    }
}

private fun DailyRitualTemplate.localizedTitle(language: AppLanguage): String =
    DailyRitualContentRepository.resolve(language = language, key = titleKey)

private fun DailyRitualTemplate.localizedSubtitle(language: AppLanguage): String =
    DailyRitualContentRepository.resolve(language = language, key = subtitleKey)

private fun DailyRitualTemplate.localizedIntro(language: AppLanguage): String =
    DailyRitualContentRepository.resolve(language = language, key = introKey)

private fun DailyRitualTemplate.localizedCompletionMessage(language: AppLanguage): String =
    DailyRitualContentRepository.resolve(language = language, key = completionMessageKey)

private fun DailyRitualStep.localizedText(language: AppLanguage): String =
    DailyRitualContentRepository.resolve(language = language, key = textKey)

private fun DailyRitualStep.localizedCtaLabel(language: AppLanguage): String =
    DailyRitualContentRepository.resolve(language = language, key = ctaLabelKey)

private fun DailyRitualStep.localizedOption(optionKey: String, language: AppLanguage): String =
    DailyRitualContentRepository.resolve(language = language, key = "daily_ritual.option.$id.$optionKey")

private fun DailyRitualError.toLocalizedMessage(strings: DailyRitualStrings): String =
    when (this) {
        DailyRitualError.TextRequired -> strings.validationTextRequired
        DailyRitualError.OptionRequired -> strings.validationOptionRequired
    }

private fun String.withInts(vararg values: Int): String {
    var resolved = this
    values.forEach { value ->
        resolved = resolved.replaceFirst("%d", value.toString())
    }
    return resolved
}
