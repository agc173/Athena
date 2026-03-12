package com.agc.bwitch.ui.oracle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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
            enabled = !state.isLoading,
        ) {
            Text(if (state.isLoading) "Consultando..." else "Consultar Oráculo")
        }

        if (state.isLoading) {
            CircularProgressIndicator()
        }

        if (state.inProgress) {
            Text(
                text = "Tu consulta está en progreso. Intenta de nuevo en unos segundos.",
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        state.answer?.let { answer ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
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

        state.error?.let { error ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Error: $error", color = MaterialTheme.colorScheme.error)
                Button(
                    onClick = { viewModel.retry() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isLoading,
                ) {
                    Text("Reintentar")
                }
            }
        }
    }
}
