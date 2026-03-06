package com.agc.bwitch.ui.tarot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.agc.bwitch.domain.tarot.TarotCard
import com.agc.bwitch.domain.tarot.TarotRequestType
import com.agc.bwitch.presentation.tarot.TarotRevealPhase
import com.agc.bwitch.presentation.tarot.TarotViewModel
import org.koin.compose.koinInject

@Composable
fun TarotCardView(
    card: TarotCard?,
    revealed: Boolean,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
    ) {
        if (!revealed) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text("BWitch Tarot", style = MaterialTheme.typography.titleMedium)
            }
        } else {
            val orientation = when (card?.upright) {
                true -> "upright"
                false -> "reversed"
                null -> "unknown"
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(card?.name.orEmpty(), style = MaterialTheme.typography.titleMedium)
                Text("Orientation: $orientation", style = MaterialTheme.typography.bodyMedium)
                card?.position?.let {
                    Text("Position: ${it.name.lowercase()}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun TarotScreen(
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
            onClick = { viewModel.newRequest(TarotRequestType.TAROT_1) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading,
        ) {
            Text(if (state.isLoading) "Drawing..." else "Draw 1 card")
        }

        Button(
            onClick = { viewModel.newRequest(TarotRequestType.TAROT_3) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading,
        ) {
            Text("Draw 3 cards")
        }

        state.requestId?.let {
            Text("Request ID: $it", style = MaterialTheme.typography.bodySmall)
        }

        if (state.revealPhase == TarotRevealPhase.SHUFFLING) {
            Text("Shuffling...")
        }

        state.response?.let { response ->
            Text("Status: ${response.status}")

            if (
                response.cards.isNotEmpty() &&
                (state.revealPhase == TarotRevealPhase.CARDS_READY || state.revealPhase == TarotRevealPhase.READING_VISIBLE)
            ) {
                Text("Cards:", style = MaterialTheme.typography.titleMedium)
                response.cards.forEachIndexed { index, card ->
                    val revealed = index < state.revealedCardCount
                    TarotCardView(
                        card = if (revealed) card else null,
                        revealed = revealed,
                    )
                }
            }

            if (state.revealPhase == TarotRevealPhase.CARDS_READY) {
                if (state.revealedCardCount < response.cards.size) {
                    Button(
                        onClick = viewModel::revealNextCard,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            if (state.selectedType == TarotRequestType.TAROT_1) {
                                "Reveal card"
                            } else {
                                "Reveal next card"
                            },
                        )
                    }
                } else {
                    Button(
                        onClick = viewModel::showReading,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("View reading")
                    }
                }
            }

            if (
                state.revealPhase == TarotRevealPhase.READING_VISIBLE &&
                response.interpretation.isNotBlank()
            ) {
                Text("Interpretation", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = response.interpretation,
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        }

        state.error?.let { error ->
            Text("Error: $error", color = MaterialTheme.colorScheme.error)
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
