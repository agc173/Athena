package com.agc.bwitch.ui.oracle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import com.agc.bwitch.localization.OracleStrings
import com.agc.bwitch.localization.appStrings
import com.agc.bwitch.presentation.oracle.OracleAskMessage
import com.agc.bwitch.presentation.oracle.OracleAskMessageId
import com.agc.bwitch.presentation.oracle.ORACLE_QUESTION_MAX_LENGTH
import com.agc.bwitch.presentation.oracle.OracleAskViewModel
import com.agc.bwitch.presentation.economy.EconomyViewModel
import com.agc.bwitch.presentation.economy.runWithEconomyGate
import com.agc.bwitch.ui.common.economy.EconomyGateInfoRow
import com.agc.bwitch.ui.common.designsystem.BWitchCard
import com.agc.bwitch.ui.common.designsystem.BWitchPrimaryButton
import com.agc.bwitch.ui.common.designsystem.BWitchSecondaryButton
import com.agc.bwitch.ui.common.designsystem.BWitchTextField
import com.agc.bwitch.ui.common.designsystem.BWitchScreen
import com.agc.bwitch.ui.common.designsystem.BWitchSectionHeader
import com.agc.bwitch.ui.theme.BWitchThemeTokens
import org.koin.compose.koinInject

@Composable
fun OracleScreen(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    viewModel: OracleAskViewModel = koinInject(),
    economyViewModel: EconomyViewModel = koinInject(),
) {
    val dimens = BWitchThemeTokens.dimens
    val colors = MaterialTheme.colorScheme
    val strings = appStrings.oracle
    val state by viewModel.uiState.collectAsState()
    val economyState by economyViewModel.uiState.collectAsState()
    val oraclePreview = economyState.modulePreviews
        .firstOrNull { it.module == "ORACLE_1Q" }

    LaunchedEffect(state.answer?.coreGuidance) {
        if (state.answer != null) {
            economyViewModel.loadEconomy()
        }
    }

    LaunchedEffect(state.error?.id) {
        if (state.error?.id.isEconomyRestrictionError()) {
            economyViewModel.loadEconomy()
        }
    }

    BWitchScreen(
        contentPadding = contentPadding,
        modifier = modifier.verticalScroll(rememberScrollState()),
    ) {
        BWitchSectionHeader(
            title = strings.headerTitle,
            subtitle = strings.headerSubtitle,
        )

        if (state.answer == null && state.error == null && !state.inProgress && !state.isLoading) {
            Text(
                text = strings.introText,
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }

        var questionLimitExceeded by remember { mutableStateOf(false) }
        var questionFieldValue by remember { mutableStateOf(TextFieldValue(state.question)) }
        LaunchedEffect(state.question) {
            if (state.question != questionFieldValue.text) {
                questionFieldValue = TextFieldValue(
                    text = state.question,
                    selection = TextRange(state.question.length),
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(dimens.spacingXs / 2)) {
            BWitchTextField(
                value = questionFieldValue,
                onValueChange = { value ->
                    questionLimitExceeded = value.text.length > ORACLE_QUESTION_MAX_LENGTH
                    val limitedValue = value.limitTextLength(ORACLE_QUESTION_MAX_LENGTH)
                    questionFieldValue = limitedValue
                    viewModel.onQuestionChange(limitedValue.text)
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(strings.questionLabel) },
                enabled = !state.isLoading,
                minLines = 3,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                isError = questionLimitExceeded,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = if (questionLimitExceeded) strings.questionLimitError else "",
                    style = MaterialTheme.typography.bodySmall,
                    color = if (questionLimitExceeded) colors.error else colors.onSurfaceVariant,
                )
                Text(
                    text = "${state.question.length}/$ORACLE_QUESTION_MAX_LENGTH",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onSurfaceVariant,
                )
            }
        }

        EconomyGateInfoRow(
            preview = oraclePreview,
            economyStrings = appStrings.economy,
            fallbackCost = 3,
        )

        BWitchPrimaryButton(
            onClick = {
                runWithEconomyGate(
                    preview = oraclePreview,
                    economyViewModel = economyViewModel,
                    source = "oracle_1q",
                    fallbackCost = 3,
                ) {
                    viewModel.ask()
                }
            },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading && !state.inProgress && state.question.length <= ORACLE_QUESTION_MAX_LENGTH,
        ) {
            Text(strings.askCta)
        }

        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = dimens.spacingLg - dimens.spacingXs),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(dimens.spacingSm + dimens.spacingXs),
                ) {
                    CircularProgressIndicator()
                    Text(
                        text = strings.loading,
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        if (state.inProgress) {
            Text(
                text = strings.inProgressMessage,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        state.answer?.let { answer ->
            BWitchCard(
                modifier = Modifier.fillMaxWidth(),
            ) {
                answer.title?.let {
                    Text(it, style = MaterialTheme.typography.titleLarge)
                }

                Text(strings.guidanceTitle, style = MaterialTheme.typography.titleMedium)
                Text(answer.coreGuidance, style = MaterialTheme.typography.bodyLarge)

                if (answer.doList.isNotEmpty()) {
                    Text(strings.doTitle, style = MaterialTheme.typography.titleSmall)
                    answer.doList.forEach { item ->
                        Text(
                            strings.listItemBulletFormat.replaceFirst("%s", item),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }

                if (answer.avoidList.isNotEmpty()) {
                    Text(strings.avoidTitle, style = MaterialTheme.typography.titleSmall)
                    answer.avoidList.forEach { item ->
                        Text(
                            strings.listItemBulletFormat.replaceFirst("%s", item),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }

                answer.reflection?.let {
                    Text(strings.reflectionTitle, style = MaterialTheme.typography.titleSmall)
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                }

                state.quotaSnapshot?.let { quota ->
                    val quotaLines = buildList {
                        quota.maxRequestsRemaining?.let { add("${strings.quotaRemainingTodayLabel}: $it") }
                        quota.adUnlockRemaining?.let { add("${strings.adUnlockRemainingLabel}: $it") }
                    }
                    if (quotaLines.isNotEmpty()) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = MaterialTheme.shapes.small,
                        ) {
                            Column(
                                modifier = Modifier.padding(dimens.spacingSm + dimens.spacingXs / 2),
                                verticalArrangement = Arrangement.spacedBy(dimens.spacingXs / 2),
                            ) {
                                quotaLines.forEach { line ->
                                    Text(
                                        text = line,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            BWitchSecondaryButton(
                onClick = viewModel::startNewConsultation,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading,
            ) {
                Text(strings.newConsultationCta)
            }
        }

        state.error?.let { error ->
            BWitchCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            ) {
                Text(
                    text = error.toUiText(strings),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
                BWitchPrimaryButton(
                    onClick = {
                        if (error.id.isEconomyRestrictionError()) {
                            runWithEconomyGate(
                                preview = oraclePreview,
                                economyViewModel = economyViewModel,
                                source = "oracle_1q",
                                fallbackCost = 3,
                            ) {
                                viewModel.retry()
                            }
                        } else {
                            viewModel.retry()
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isLoading && !state.inProgress,
                ) {
                    Text(strings.retryCta)
                }
            }
        }
    }
}

private fun OracleAskMessage.toUiText(strings: OracleStrings) = when (id) {
    OracleAskMessageId.EmptyQuestion -> strings.errorEmptyQuestion
    OracleAskMessageId.QuestionTooLong -> strings.questionLimitError
    OracleAskMessageId.Unauthenticated -> strings.errorUnauthenticated
    OracleAskMessageId.PermissionDenied -> strings.errorPermissionDenied
    OracleAskMessageId.ResourceExhaustedWithAdUnlock -> strings.errorResourceExhaustedWithAdUnlock
    OracleAskMessageId.ResourceExhaustedGeneric -> strings.errorResourceExhaustedGeneric
    OracleAskMessageId.FailedPreconditionWithAdUnlock -> strings.errorFailedPreconditionWithAdUnlock
    OracleAskMessageId.FailedPreconditionTemporaryUnavailable -> strings.errorFailedPreconditionTemporaryUnavailable
    OracleAskMessageId.FailedPreconditionGeneric -> strings.errorFailedPreconditionGeneric
    OracleAskMessageId.InsufficientMoons -> strings.errorInsufficientMoons
    OracleAskMessageId.InvalidArgumentFallback -> strings.errorInvalidArgumentFallback
    OracleAskMessageId.InternalTemporaryUnavailable -> strings.errorInternalTemporaryUnavailable
    OracleAskMessageId.InternalGeneric -> strings.errorInternalGeneric
    OracleAskMessageId.UnknownFallback -> strings.errorUnknownFallback
    OracleAskMessageId.RawBackendMessage -> strings.errorUnknownFallback
}

private fun OracleAskMessageId?.isEconomyRestrictionError(): Boolean =
    this == OracleAskMessageId.InsufficientMoons

private fun TextFieldValue.limitTextLength(maxLength: Int): TextFieldValue {
    if (text.length <= maxLength) return this

    val limitedText = text.take(maxLength)
    return copy(
        text = limitedText,
        selection = TextRange(
            start = selection.start.coerceIn(0, limitedText.length),
            end = selection.end.coerceIn(0, limitedText.length),
        ),
        composition = null,
    )
}
