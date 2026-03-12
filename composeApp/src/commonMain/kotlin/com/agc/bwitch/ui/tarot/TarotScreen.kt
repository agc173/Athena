package com.agc.bwitch.ui.tarot

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import bwitch.composeapp.generated.resources.*
import com.agc.bwitch.audio.TarotSoundPlayer
import com.agc.bwitch.domain.tarot.TarotCardPosition
import com.agc.bwitch.domain.tarot.TarotReadingDetails
import com.agc.bwitch.domain.tarot.TarotRequestType
import com.agc.bwitch.presentation.tarot.TarotRevealPhase
import com.agc.bwitch.presentation.tarot.TarotViewModel
import com.agc.bwitch.ui.tarot.components.TarotCardView
import com.agc.bwitch.ui.tarot.components.TarotLoadingDeck
import com.agc.bwitch.ui.tarot.components.TarotMiniCard
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.Font
import org.koin.compose.koinInject

@Composable
fun TarotScreen(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    viewModel: TarotViewModel = koinInject(),
    tarotSoundPlayer: TarotSoundPlayer = koinInject(),
) {
    val state by viewModel.uiState.collectAsState()
    val language = currentTarotUiLanguage()

    Box {
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

            when (state.revealPhase) {
                TarotRevealPhase.WAITING_TO_SHUFFLE -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        TarotCardView(card = null, revealed = false, onClick = viewModel::startShuffle)
                        Text("Pulsa el mazo para barajar", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                TarotRevealPhase.SHUFFLING -> {
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

                TarotRevealPhase.WAITING_TO_REVEAL -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        TarotCardView(card = null, revealed = false, onClick = viewModel::startReveal)
                        Text("Pulsa para revelar tus cartas", style = MaterialTheme.typography.bodyMedium)
                    }
                }

                else -> Unit
            }

            state.response?.let { response ->
                if (response.cards.isNotEmpty() && state.revealedCardCount > 0) {
                    val allCardsRevealed = state.revealedCardCount >= response.cards.size
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("Tus cartas", style = MaterialTheme.typography.titleMedium)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                        ) {
                            response.cards.take(state.revealedCardCount).forEachIndexed { index, card ->
                                val label = when (state.selectedType) {
                                    TarotRequestType.TAROT_1 -> "Carta"
                                    TarotRequestType.TAROT_3 -> when (card.position) {
                                        TarotCardPosition.PAST -> "Pasado"
                                        TarotCardPosition.PRESENT -> "Presente"
                                        TarotCardPosition.FUTURE -> "Futuro"
                                        null -> "Carta ${index + 1}"
                                    }
                                }
                                TarotMiniCard(
                                    card = card,
                                    label = label,
                                    selected = state.openedMiniCardIndex == index,
                                    onClick = if (allCardsRevealed) {
                                        { viewModel.toggleMiniCard(index) }
                                    } else {
                                        null
                                    },
                                )
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

        state.response?.let { response ->
            val overlayIndex = when {
                state.overlayVisible -> state.overlayCardIndex
                state.openedMiniCardIndex != null -> state.openedMiniCardIndex
                else -> null
            }?.coerceIn(0, response.cards.lastIndex)
            val overlayCard = overlayIndex?.let { response.cards[it] }
            if (overlayCard != null && overlayIndex != null) {
                val isMiniOverlay = !state.overlayVisible
                val isRevealed = if (state.overlayVisible) state.overlayCardRevealed else true

                Dialog(
                    onDismissRequest = {
                        if (isMiniOverlay) {
                            viewModel.toggleMiniCard(overlayIndex)
                        }
                    },
                    properties = DialogProperties(usePlatformDefaultWidth = false),
                ) {
                    val scrimInteractionSource = remember { MutableInteractionSource() }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(tarotOverlayAtmosphereBrush())
                            .clickable(
                                interactionSource = scrimInteractionSource,
                                indication = null,
                            ) {
                                if (isMiniOverlay) {
                                    viewModel.toggleMiniCard(overlayIndex)
                                }
                            },
                        contentAlignment = Alignment.Center,
                    ) {
                        var overlayContentVisible by remember(overlayIndex, isMiniOverlay) {
                            mutableStateOf(false)
                        }
                        var advancingFromRevealedCard by remember(overlayIndex, isMiniOverlay) {
                            mutableStateOf(false)
                        }
                        LaunchedEffect(overlayIndex, isMiniOverlay) {
                            overlayContentVisible = true
                            advancingFromRevealedCard = false
                        }
                        LaunchedEffect(advancingFromRevealedCard) {
                            if (advancingFromRevealedCard) {
                                delay(OVERLAY_CARD_EXIT_DURATION_MS.toLong())
                                viewModel.revealNextCard()
                            }
                        }

                        AnimatedVisibility(
                            visible = overlayContentVisible,
                            enter = fadeIn(animationSpec = tween(durationMillis = 220)) +
                                    scaleIn(
                                        initialScale = 0.96f,
                                        animationSpec = tween(durationMillis = 220),
                                    ),
                            exit = fadeOut(animationSpec = tween(durationMillis = OVERLAY_CARD_EXIT_DURATION_MS)) +
                                    scaleOut(
                                        targetScale = 0.96f,
                                        animationSpec = tween(durationMillis = OVERLAY_CARD_EXIT_DURATION_MS),
                                    ),
                        ) {
                            val labels = remember(overlayCard.id, language) {
                                tarotOverlayLabels(overlayCard, language)
                            }
                            val metadataPrimaryColor = Color(0xFFF8EEFF)
                            val metadataSecondaryColor = Color(0xFFD5C4E9)

                            Column(
                                modifier = Modifier
                                    .padding(24.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                Box(
                                    modifier = Modifier.height(OVERLAY_TOP_METADATA_HEIGHT),
                                    contentAlignment = Alignment.BottomCenter,
                                ) {
                                    if (isRevealed) {
                                        Column(
                                            verticalArrangement = Arrangement.spacedBy(2.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                        ) {
                                            labels.position?.let {
                                                Text(
                                                    text = it,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = metadataSecondaryColor,
                                                    textAlign = TextAlign.Center,
                                                )
                                            }

                                            Text(
                                                text = labels.cardName,
                                                style = tarotOverlayTitleTextStyle(
                                                    baseStyle = MaterialTheme.typography.titleLarge,
                                                    fontWeight = FontWeight.SemiBold,
                                                ),
                                                color = metadataPrimaryColor,
                                            )
                                        }
                                    }
                                }

                                Box(contentAlignment = Alignment.Center) {
                                    if (!isMiniOverlay) {
                                        Box(
                                            modifier = Modifier
                                                .size(width = 336.dp, height = 504.dp)
                                                .background(brush = tarotOverlayCardHaloBrush()),
                                        )
                                    }

                                    TarotCardView(
                                        card = if (isRevealed) overlayCard else null,
                                        revealed = isRevealed,
                                        cardWidth = 260.dp,
                                        onClick = if (isMiniOverlay) {
                                            { viewModel.toggleMiniCard(overlayIndex) }
                                        } else {
                                            {
                                                if (isRevealed) {
                                                    if (!advancingFromRevealedCard) {
                                                        advancingFromRevealedCard = true
                                                        overlayContentVisible = false
                                                    }
                                                } else {
                                                    tarotSoundPlayer.playCardFlip()
                                                    viewModel.revealNextCard()
                                                }
                                            }
                                        },
                                    )
                                }

                                Box(
                                    modifier = Modifier.height(OVERLAY_BOTTOM_METADATA_HEIGHT),
                                    contentAlignment = Alignment.TopCenter,
                                ) {
                                    if (isRevealed) {
                                        Text(
                                            text = labels.orientation,
                                            style = MaterialTheme.typography.bodySmall,
                                            textAlign = TextAlign.Center,
                                            color = metadataSecondaryColor,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private const val OVERLAY_CARD_EXIT_DURATION_MS = 180
private val OVERLAY_TOP_METADATA_HEIGHT = 72.dp
private val OVERLAY_BOTTOM_METADATA_HEIGHT = 52.dp

@Composable
private fun tarotOverlayAtmosphereBrush(): Brush {
    val scrim = MaterialTheme.colorScheme.scrim
    val deepBase = lerp(scrim, Color(0xFF080711), 0.68f)
    return Brush.radialGradient(
        colors = listOf(
            Color(0xFF312752).copy(alpha = 0.58f),
            Color(0xFF17142B).copy(alpha = 0.85f),
            deepBase.copy(alpha = 0.96f),
        ),
        radius = 1200f,
    )
}

@Composable
private fun tarotOverlayCardHaloBrush(): Brush = Brush.radialGradient(
    colors = listOf(
        Color(0xFFE8DCFF).copy(alpha = 0.14f),
        Color(0xFF8D79C8).copy(alpha = 0.10f),
        Color(0xFF241A3F).copy(alpha = 0.00f),
    ),
    radius = 520f,
)

@Composable
private fun tarotOverlayTitleTextStyle(
    baseStyle: TextStyle,
    fontWeight: FontWeight,
): TextStyle = baseStyle.copy(
    fontFamily = tarotTitleFontFamily(),
    fontWeight = fontWeight,
    letterSpacing = 0.08.em,
    textAlign = TextAlign.Center,
)

@Composable
private fun tarotTitleFontFamily(): FontFamily = FontFamily(
    Font(Res.allFontResources.getValue("cinzel_regular")),
    Font(Res.allFontResources.getValue("cinzel_semibold"), weight = FontWeight.SemiBold),
)

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
