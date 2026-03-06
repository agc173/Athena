package com.agc.bwitch.ui.tarot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.agc.bwitch.domain.tarot.TarotCardPosition
import com.agc.bwitch.domain.tarot.TarotReadingDetails
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
                Text("Orientación: $orientation", style = MaterialTheme.typography.bodyMedium)
                card?.position?.let {
                    Text("Posición: ${it.name.lowercase()}", style = MaterialTheme.typography.bodySmall)
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
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Tarot", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Elige una tirada y revela tus cartas",
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Button(
            onClick = { viewModel.newRequest(TarotRequestType.TAROT_1) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading,
        ) {
            Text(if (state.isLoading) "Sacando carta..." else "Sacar 1 carta")
        }

        Button(
            onClick = { viewModel.newRequest(TarotRequestType.TAROT_3) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !state.isLoading,
        ) {
            Text("Tirada de 3 cartas")
        }
        }

        if (state.revealPhase == TarotRevealPhase.SHUFFLING) {
            Text("Barajando...")
        }

        state.response?.let { response ->
            if (
                response.cards.isNotEmpty() &&
                (state.revealPhase == TarotRevealPhase.CARDS_READY || state.revealPhase == TarotRevealPhase.READING_VISIBLE)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Tus cartas", style = MaterialTheme.typography.titleMedium)
                    response.cards.forEachIndexed { index, card ->
                        val revealed = index < state.revealedCardCount
                        TarotCardView(
                            card = if (revealed) card else null,
                            revealed = revealed,
                        )
                    }
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
                                "Revelar carta"
                            } else {
                                "Revelar siguiente carta"
                            },
                        )
                    }
                } else {
                    Button(
                        onClick = viewModel::showReading,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Ver lectura")
                    }
                }
            }

            if (state.revealPhase == TarotRevealPhase.READING_VISIBLE) {
                when (val details = response.details) {
                    is TarotReadingDetails.Tarot1ReadingDetails -> {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Lectura", style = MaterialTheme.typography.titleMedium)
                            TarotReadingSection(title = "Tema", body = details.theme)
                            TarotReadingSection(title = "Significado", body = details.meaning)
                            TarotReadingSection(title = "Consejo", body = details.advice)
                            TarotReadingSection(title = "Atención", body = details.watchOut)
                        }
                    }

                    is TarotReadingDetails.Tarot3ReadingDetails -> {
                        val cardsByPosition = details.cards.associateBy { it.position }
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Lectura", style = MaterialTheme.typography.titleMedium)
                            TarotReadingSection(
                                title = "Pasado",
                                body = cardsByPosition[TarotCardPosition.PAST]?.meaning.orEmpty(),
                            )
                            TarotReadingSection(
                                title = "Presente",
                                body = cardsByPosition[TarotCardPosition.PRESENT]?.meaning.orEmpty(),
                            )
                            TarotReadingSection(
                                title = "Futuro",
                                body = cardsByPosition[TarotCardPosition.FUTURE]?.meaning.orEmpty(),
                            )
                            TarotReadingSection(title = "Resumen", body = details.summary)
                            TarotReadingSection(title = "Consejo", body = details.advice)
                        }
                    }

                    null -> {
                        if (response.interpretation.isNotBlank()) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Lectura", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    text = response.interpretation,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            }
                        }
                    }
                }
            }
        }

        state.error?.let { error ->
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Error: $error", color = MaterialTheme.colorScheme.error)
                Button(
                    onClick = viewModel::retry,
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !state.isLoading && state.requestId != null,
                ) {
                    Text("Reintentar")
                }
            }
        }
    }
}

@Composable
private fun TarotReadingSection(
    title: String,
    body: String,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
