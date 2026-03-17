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
    val state by viewModel.uiState.collectAsState()

    BWitchScreen(
        contentPadding = contentPadding,
        modifier = modifier.verticalScroll(rememberScrollState()),
    ) {
        BWitchSectionHeader(
            title = "Consulta intuitiva",
            subtitle = "Haz una pregunta clara para recibir una guía enfocada",
        )

        if (state.answer == null && state.error == null && !state.inProgress && !state.isLoading) {
            Text(
                text = "Escribe una pregunta específica y el Oráculo te ofrecerá guía práctica para tu situación.",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
            )
        }

        BWitchTextField(
            value = state.question,
            onValueChange = viewModel::onQuestionChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Tu pregunta") },
            enabled = !state.isLoading,
            minLines = 3,
        )

        BWitchPrimaryButton(
            onClick = { viewModel.ask() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading && !state.inProgress,
        ) {
            Text("Consultar Oráculo")
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
                        text = "Consultando el Oráculo...",
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }

        if (state.inProgress) {
            Text(
                text = "Tu consulta sigue en proceso. Puedes editar tu pregunta para iniciar una nueva consulta.",
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

                Text("Guía", style = MaterialTheme.typography.titleMedium)
                Text(answer.coreGuidance, style = MaterialTheme.typography.bodyLarge)

                if (answer.doList.isNotEmpty()) {
                    Text("Haz", style = MaterialTheme.typography.titleSmall)
                    answer.doList.forEach { item ->
                        Text("• $item", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                if (answer.avoidList.isNotEmpty()) {
                    Text("Evita", style = MaterialTheme.typography.titleSmall)
                    answer.avoidList.forEach { item ->
                        Text("• $item", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                answer.reflection?.let {
                    Text("Reflexión", style = MaterialTheme.typography.titleSmall)
                    Text(it, style = MaterialTheme.typography.bodyMedium)
                }

                state.quotaSnapshot?.let { quota ->
                    val quotaLines = buildList {
                        quota.maxRequestsRemaining?.let { add("Consultas restantes hoy: $it") }
                        quota.adUnlockRemaining?.let { add("Desbloqueos por anuncio restantes: $it") }
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
                Text("Nueva consulta")
            }
        }

        state.error?.let { error ->
            BWitchCard(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            ) {
                Text(
                    text = error,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onErrorContainer,
                )
                BWitchPrimaryButton(
                    onClick = { viewModel.retry() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isLoading && !state.inProgress,
                ) {
                    Text("Reintentar")
                }
            }
        }
    }
}
