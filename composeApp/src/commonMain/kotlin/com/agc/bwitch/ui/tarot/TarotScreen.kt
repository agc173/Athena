package com.agc.bwitch.ui.tarot

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
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
    onClick: (() -> Unit)? = null,
) {
    val cardWidth = 160.dp
    val cardHeight = 240.dp
    val cardModifier = Modifier
        .width(cardWidth)
        .height(cardHeight)
        .let { modifier ->
            if (onClick != null) {
                modifier.clickable(onClick = onClick)
            } else {
                modifier
            }
        }

    Box(
        modifier = Modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            modifier = cardModifier,
        ) {
            if (!revealed) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(cardHeight),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("BWitch", style = MaterialTheme.typography.titleMedium)
                    Text("Tarot", style = MaterialTheme.typography.bodyMedium)
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(card?.name.orEmpty(), style = MaterialTheme.typography.titleMedium)
                    Text("Ilustración próximamente", style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

@Composable
private fun TarotLoadingDeck(
    title: String,
    subtitle: String,
) {
    val cardWidth = 150.dp
    val cardHeight = 230.dp
    val transition = rememberInfiniteTransition(label = "tarot-loading-transition")
    val backCardMovement by transition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1400),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(offsetMillis = 120),
        ),
        label = "back-card-movement",
    )
    val middleCardMovement by transition.animateFloat(
        initialValue = 6f,
        targetValue = -6f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1300),
            repeatMode = RepeatMode.Reverse,
            initialStartOffset = StartOffset(offsetMillis = 240),
        ),
        label = "middle-card-movement",
    )
    val topCardMovement by transition.animateFloat(
        initialValue = -2f,
        targetValue = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1000),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "top-card-movement",
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(cardHeight + 32.dp),
            contentAlignment = Alignment.Center,
        ) {
            Card(
                modifier = Modifier
                    .width(cardWidth)
                    .height(cardHeight)
                    .graphicsLayer {
                        rotationZ = -8f + backCardMovement
                        translationX = -16f
                        translationY = 10f
                    },
            ) {}

            Card(
                modifier = Modifier
                    .width(cardWidth)
                    .height(cardHeight)
                    .graphicsLayer {
                        rotationZ = 8f + middleCardMovement
                        translationX = 16f
                        translationY = 2f
                    },
            ) {}

            Card(
                modifier = Modifier
                    .width(cardWidth)
                    .height(cardHeight)
                    .graphicsLayer {
                        rotationZ = topCardMovement
                    },
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(cardHeight),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text("BWitch", style = MaterialTheme.typography.headlineSmall)
                    Text("Tarot", style = MaterialTheme.typography.titleMedium)
                }
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodyMedium)
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
            val loadingTitle: String
            val loadingSubtitle: String
            when (state.selectedType) {
                TarotRequestType.TAROT_1 -> {
                    loadingTitle = "Barajando las cartas..."
                    loadingSubtitle = "Tu lectura se está preparando"
                }

                TarotRequestType.TAROT_3 -> {
                    loadingTitle = "Preparando tu tirada..."
                    loadingSubtitle = "Las cartas están revelando su mensaje"
                }
            }
            TarotLoadingDeck(
                title = loadingTitle,
                subtitle = loadingSubtitle,
            )
        }

        state.response?.let { response ->
            if (
                response.cards.isNotEmpty() &&
                (state.revealPhase == TarotRevealPhase.CARDS_READY || state.revealPhase == TarotRevealPhase.READING_VISIBLE)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Tus cartas", style = MaterialTheme.typography.titleMedium)
                    if (state.selectedType == TarotRequestType.TAROT_3 && (state.revealPhase == TarotRevealPhase.CARDS_READY || state.revealPhase == TarotRevealPhase.READING_VISIBLE)) {
                        val activeIndex = state.activeCardIndex.coerceIn(0, response.cards.lastIndex)
                        val activeCard = response.cards[activeIndex]

                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            TarotCardView(
                                card = if (state.activeCardRevealed) activeCard else null,
                                revealed = state.activeCardRevealed,
                                onClick = if (state.revealPhase == TarotRevealPhase.CARDS_READY) viewModel::revealNextCard else null,
                            )

                            if (state.activeCardRevealed) {
                                val orientation = when (activeCard.upright) {
                                    true -> "Al derecho"
                                    false -> "Invertida"
                                    null -> "Desconocida"
                                }
                                Text("Posición: ${
                                    when (activeCard.position) {
                                        TarotCardPosition.PAST -> "Pasado"
                                        TarotCardPosition.PRESENT -> "Presente"
                                        TarotCardPosition.FUTURE -> "Futuro"
                                        null -> "Desconocida"
                                    }
                                }", style = MaterialTheme.typography.bodySmall)
                                Text("Orientación: $orientation", style = MaterialTheme.typography.bodyMedium)
                            }
                        }

                        if (state.revealedCardCount > 0) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text("Cartas reveladas", style = MaterialTheme.typography.titleSmall)
                                response.cards.take(state.revealedCardCount).forEachIndexed { index, card ->
                                    val label = when (index) {
                                        0 -> "Pasado"
                                        1 -> "Presente"
                                        else -> "Futuro"
                                    }
                                    Text("$label — ${card.name}", style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    } else {
                        response.cards.forEachIndexed { index, card ->
                            val revealed = index < state.revealedCardCount
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                TarotCardView(
                                    card = if (revealed) card else null,
                                    revealed = revealed,
                                    onClick = if (!revealed && state.revealPhase == TarotRevealPhase.CARDS_READY) {
                                        viewModel::revealNextCard
                                    } else {
                                        null
                                    },
                                )

                                if (revealed) {
                                    val orientation = when (card.upright) {
                                        true -> "Al derecho"
                                        false -> "Invertida"
                                        null -> "Desconocida"
                                    }
                                    Text("Orientación: $orientation", style = MaterialTheme.typography.bodyMedium)

                                    val position = when (card.position) {
                                        TarotCardPosition.PAST -> "Pasado"
                                        TarotCardPosition.PRESENT -> "Presente"
                                        TarotCardPosition.FUTURE -> "Futuro"
                                        null -> null
                                    }
                                    if (position != null) {
                                        Text("Posición: $position", style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            if (state.revealPhase == TarotRevealPhase.CARDS_READY) {
                if (state.revealedCardCount >= response.cards.size) {
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
