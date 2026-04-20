package com.agc.bwitch.ui.oracle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import com.agc.bwitch.localization.OracleStrings
import com.agc.bwitch.localization.appStrings
import com.agc.bwitch.presentation.oracle.OracleAskMessage
import com.agc.bwitch.presentation.oracle.OracleAskMessageId
import com.agc.bwitch.presentation.oracle.OracleAskViewModel
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
) {
    val dimens = BWitchThemeTokens.dimens
    val colors = MaterialTheme.colorScheme
    val strings = appStrings.oracle
    val state by viewModel.uiState.collectAsState()

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

        BWitchTextField(
            value = state.question,
            onValueChange = viewModel::onQuestionChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(strings.questionLabel) },
            enabled = !state.isLoading,
            minLines = 3,
        )

        BWitchPrimaryButton(
            onClick = { viewModel.ask() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading && !state.inProgress,
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
                    onClick = { viewModel.retry() },
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
    OracleAskMessageId.Unauthenticated -> strings.errorUnauthenticated
    OracleAskMessageId.PermissionDenied -> strings.errorPermissionDenied
    OracleAskMessageId.ResourceExhaustedWithAdUnlock -> strings.errorResourceExhaustedWithAdUnlock
    OracleAskMessageId.ResourceExhaustedGeneric -> strings.errorResourceExhaustedGeneric
    OracleAskMessageId.FailedPreconditionWithAdUnlock -> strings.errorFailedPreconditionWithAdUnlock
    OracleAskMessageId.FailedPreconditionTemporaryUnavailable -> strings.errorFailedPreconditionTemporaryUnavailable
    OracleAskMessageId.FailedPreconditionGeneric -> strings.errorFailedPreconditionGeneric
    OracleAskMessageId.InvalidArgumentFallback -> strings.errorInvalidArgumentFallback
    OracleAskMessageId.InternalTemporaryUnavailable -> strings.errorInternalTemporaryUnavailable
    OracleAskMessageId.InternalGeneric -> strings.errorInternalGeneric
    OracleAskMessageId.UnknownFallback -> strings.errorUnknownFallback
    OracleAskMessageId.RawBackendMessage -> strings.errorUnknownFallback
}
