package com.agc.bwitch.ui.oracle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.agc.bwitch.presentation.oracle.OracleAskViewModel
import org.koin.compose.koinInject

@Composable
fun OracleScreen(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    viewModel: OracleAskViewModel = koinInject(),
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("Oráculo", style = MaterialTheme.typography.headlineMedium)
        Text("Haz una pregunta clara para recibir guía", style = MaterialTheme.typography.bodyMedium)

        if (state.answer == null && state.error == null && !state.inProgress && !state.isLoading) {
            Text(
                text = "Escribe una pregunta específica y el Oráculo te ofrecerá guía práctica para tu situación.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        OutlinedTextField(
            value = state.question,
            onValueChange = viewModel::onQuestionChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Tu pregunta") },
            enabled = !state.isLoading,
            minLines = 3,
        )

        Button(
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
                    .padding(vertical = 20.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp),
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
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
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
                }
            }

            Button(
                onClick = viewModel::startNewConsultation,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading,
            ) {
                Text("Nueva consulta")
            }
        }

        state.error?.let { error ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer),
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = error,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                    )
                    Button(
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
}
