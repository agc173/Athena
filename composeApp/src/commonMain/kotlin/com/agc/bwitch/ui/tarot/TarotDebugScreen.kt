package com.agc.bwitch.ui.tarot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.agc.bwitch.presentation.tarot.TarotViewModel
import org.koin.compose.koinInject

@Composable
fun TarotDebugScreen(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    viewModel: TarotViewModel = koinInject(),
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .padding(contentPadding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Button(
            onClick = viewModel::newRequest,
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading,
        ) {
            Text(if (state.isLoading) "Drawing..." else "Draw 1 card")
        }

        state.requestId?.let {
            Text("requestId: $it", style = MaterialTheme.typography.bodySmall)
        }

        state.response?.let { response ->
            Text("status: ${response.status}")

            if (response.cards.isNotEmpty()) {
                Text("cards:", style = MaterialTheme.typography.titleMedium)
                response.cards.forEach { card ->
                    val orientation = when (card.upright) {
                        true -> "upright"
                        false -> "reversed"
                        null -> "unknown"
                    }
                    Text("- ${card.name} (${card.id}) • $orientation")
                }
            }

            if (response.interpretation.isNotBlank()) {
                Text("interpretation", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = response.interpretation,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        state.error?.let { error ->
            Text("error: $error", color = MaterialTheme.colorScheme.error)
            Button(
                onClick = viewModel::retry,
                modifier = Modifier.fillMaxWidth(),
                enabled = !state.isLoading && state.requestId != null,
            ) {
                Text("Retry")
            }
        }
    }
}
