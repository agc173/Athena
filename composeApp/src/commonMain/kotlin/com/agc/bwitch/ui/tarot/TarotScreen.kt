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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.agc.bwitch.audio.TarotHaptics
import com.agc.bwitch.audio.TarotSoundPlayer
import com.agc.bwitch.domain.tarot.TarotCardPosition
import com.agc.bwitch.domain.tarot.TarotReadingDetails
import com.agc.bwitch.domain.tarot.TarotRequestType
import com.agc.bwitch.domain.model.DeckCardUnlockReward
import com.agc.bwitch.localization.appStrings
import com.agc.bwitch.platform.share.ShareResult
import com.agc.bwitch.platform.share.ShareTextPayload
import com.agc.bwitch.platform.share.rememberShareLauncher
import com.agc.bwitch.presentation.tarot.TarotRevealPhase
import com.agc.bwitch.presentation.tarot.TarotUiEffect
import com.agc.bwitch.presentation.tarot.TAROT_LIMIT_REACHED_ERROR_KEY
import com.agc.bwitch.presentation.tarot.TarotViewModel
import com.agc.bwitch.presentation.economy.EconomyViewModel
import com.agc.bwitch.ui.common.economy.DailyLimitPaywallCard
import com.agc.bwitch.ui.tarot.components.TarotCardView
import com.agc.bwitch.ui.tarot.components.TarotLoadingDeck
import com.agc.bwitch.ui.tarot.components.TarotMiniCard
import com.agc.bwitch.ui.theme.bwitchDisplayFontFamily
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun TarotScreen(
    contentPadding: PaddingValues,
    initialRequestType: TarotRequestType? = null,
    modifier: Modifier = Modifier,
    viewModel: TarotViewModel = koinInject(),
    economyViewModel: EconomyViewModel = koinInject(),
    onOpenStore: () -> Unit = {},
    onOpenCollection: () -> Unit = {},
    tarotSoundPlayer: TarotSoundPlayer = koinInject(),
    tarotHaptics: TarotHaptics = koinInject(),
) {
    val state by viewModel.uiState.collectAsState()
    val strings = appStrings.tarot
    val shareTitle = appStrings.horoscope.shareCta
    val shareErrorFallback = appStrings.birthChart.shareFailedFallback
    val showDailyLimitPaywall = state.error == TAROT_LIMIT_REACHED_ERROR_KEY
    val shareLauncher = rememberShareLauncher()
    val shareScope = rememberCoroutineScope()
    var shareErrorMessage by remember { mutableStateOf<String?>(null) }
    var rewardDialogRewards by remember { mutableStateOf<List<DeckCardUnlockReward>>(emptyList()) }

    LaunchedEffect(initialRequestType) {
        if (initialRequestType != null) {
            viewModel.newRequest(initialRequestType)
        } else {
            viewModel.openLastReading()
        }
    }

    LaunchedEffect(state.isLoading, state.response, state.error, state.insufficientMoonsMessage) {
        if (!state.isLoading && (state.response != null || state.error != null || state.insufficientMoonsMessage != null)) {
            economyViewModel.loadEconomy()
        }
    }
    LaunchedEffect(viewModel) {
        viewModel.uiEffects.collect { effect ->
            when (effect) {
                is TarotUiEffect.ShowDeckCardUnlockRewards -> rewardDialogRewards = effect.rewards
            }
        }
    }

    Box {
        Column(
            modifier = modifier
                .padding(contentPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    strings.revealIntro,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            state.insufficientMoonsMessage?.let {
                Text(
                    text = strings.insufficientMoonsForExtraReading,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
            }

            when (state.revealPhase) {
                TarotRevealPhase.WAITING_TO_SHUFFLE -> {
                    TarotDeckInteractionStage {
                        TarotCardView(
                            card = null,
                            revealed = false,
                            cardWidth = TAROT_DECK_STAGE_CARD_WIDTH,
                            onClick = viewModel::startShuffle,
                            deckId = state.selectedDeckId,
                        )
                        Text(strings.tapDeckToShuffle, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                TarotRevealPhase.SHUFFLING -> {
                    val loadingTitle: String
                    val loadingSubtitle: String
                    when (state.selectedType) {
                        TarotRequestType.TAROT_1 -> {
                            loadingTitle = strings.loadingSingleTitle
                            loadingSubtitle = strings.loadingSingleSubtitle
                        }

                        TarotRequestType.TAROT_3 -> {
                            loadingTitle = strings.loadingThreeTitle
                            loadingSubtitle = strings.loadingThreeSubtitle
                        }
                    }
                    TarotDeckInteractionStage {
                        TarotLoadingDeck(
                            title = loadingTitle,
                            subtitle = loadingSubtitle,
                        )
                    }
                }

                TarotRevealPhase.WAITING_TO_REVEAL -> {
                    TarotDeckInteractionStage {
                        TarotCardView(
                            card = null,
                            revealed = false,
                            cardWidth = TAROT_DECK_STAGE_CARD_WIDTH,
                            onClick = viewModel::startReveal,
                            deckId = state.selectedDeckId,
                        )
                        Text(strings.tapToReveal, style = MaterialTheme.typography.bodyMedium)
                    }
                }

                else -> Unit
            }

            state.response?.let { response ->
                if (response.cards.isNotEmpty() && state.revealedCardCount > 0) {
                    val allCardsRevealed = state.revealedCardCount >= response.cards.size
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(strings.yourCardsTitle, style = MaterialTheme.typography.titleMedium)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
                        ) {
                            response.cards.take(state.revealedCardCount).forEachIndexed { index, card ->
                                val label = when (state.selectedType) {
                                    TarotRequestType.TAROT_1 -> strings.cardLabel
                                    TarotRequestType.TAROT_3 -> when (card.position) {
                                        TarotCardPosition.PAST -> strings.pastLabel
                                        TarotCardPosition.PRESENT -> strings.presentLabel
                                        TarotCardPosition.FUTURE -> strings.futureLabel
                                        null -> "${strings.cardNumberLabelPrefix} ${index + 1}"
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
                                    deckId = state.selectedDeckId,
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
                            Text(strings.viewReadingCta)
                        }
                    }
                }

                if (state.revealPhase == TarotRevealPhase.READING_VISIBLE) {
                    when (val details = response.details) {
                        is TarotReadingDetails.Tarot1ReadingDetails -> {
                            val shareText = buildTarotShareText(
                                card = response.cards.firstOrNull(),
                                details = details,
                                fallbackTitle = strings.readingTitle,
                                cardLabel = strings.cardLabel,
                                orientationTitle = strings.orientationTitle,
                                themeTitle = strings.themeTitle,
                                meaningTitle = strings.meaningTitle,
                                adviceTitle = strings.adviceTitle,
                                watchOutTitle = strings.watchOutTitle,
                                uprightLabel = strings.uprightLabel,
                                reversedLabel = strings.reversedLabel,
                                orientationUnknownLabel = strings.orientationUnknownLabel,
                            )
                            val sectionVisibility = remember(details) {
                                mutableStateListOf(false, false, false, false)
                            }
                            LaunchedEffect(details) {
                                sectionVisibility.indices.forEach { sectionVisibility[it] = false }
                                sectionVisibility.indices.forEach { index ->
                                    if (index > 0) delay(250)
                                    sectionVisibility[index] = true
                                }
                            }
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(strings.readingTitle, style = MaterialTheme.typography.titleMedium)
                                AnimatedVisibility(
                                    visible = sectionVisibility[0],
                                    enter = fadeIn(animationSpec = tween(400)),
                                ) {
                                    TarotReadingSection(title = strings.themeTitle, body = details.theme)
                                }
                                AnimatedVisibility(
                                    visible = sectionVisibility[1],
                                    enter = fadeIn(animationSpec = tween(400)),
                                ) {
                                    TarotReadingSection(title = strings.meaningTitle, body = details.meaning)
                                }
                                AnimatedVisibility(
                                    visible = sectionVisibility[2],
                                    enter = fadeIn(animationSpec = tween(400)),
                                ) {
                                    TarotReadingSection(title = strings.adviceTitle, body = details.advice)
                                }
                                AnimatedVisibility(
                                    visible = sectionVisibility[3],
                                    enter = fadeIn(animationSpec = tween(400)),
                                ) {
                                    TarotReadingSection(title = strings.watchOutTitle, body = details.watchOut)
                                }
                                if (shareText.isNotBlank()) {
                                    Button(
                                        onClick = {
                                            shareErrorMessage = null
                                            shareScope.launch {
                                                val shareResult = shareLauncher.shareText(
                                                    ShareTextPayload(text = shareText, title = shareTitle),
                                                )
                                                if (shareResult is ShareResult.Error) {
                                                    shareErrorMessage = shareResult.message ?: shareErrorFallback
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                    ) { Text(shareTitle) }
                                }
                            }
                        }

                        is TarotReadingDetails.Tarot3ReadingDetails -> {
                            val shareText = buildTarotShareText(
                                cards = response.cards,
                                details = details,
                                fallbackTitle = strings.readingTitle,
                                pastLabel = strings.pastLabel,
                                presentLabel = strings.presentLabel,
                                futureLabel = strings.futureLabel,
                                cardLabel = strings.cardLabel,
                                orientationTitle = strings.orientationTitle,
                                meaningTitle = strings.meaningTitle,
                                summaryTitle = strings.summaryTitle,
                                adviceTitle = strings.adviceTitle,
                                uprightLabel = strings.uprightLabel,
                                reversedLabel = strings.reversedLabel,
                                orientationUnknownLabel = strings.orientationUnknownLabel,
                            )
                            val sectionVisibility = remember(details) {
                                mutableStateListOf(false, false, false, false, false)
                            }
                            LaunchedEffect(details) {
                                sectionVisibility.indices.forEach { sectionVisibility[it] = false }
                                sectionVisibility.indices.forEach { index ->
                                    if (index > 0) delay(250)
                                    sectionVisibility[index] = true
                                }
                            }
                            val cardsByPosition = details.cards.associateBy { it.position }
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(strings.readingTitle, style = MaterialTheme.typography.titleMedium)
                                AnimatedVisibility(
                                    visible = sectionVisibility[0],
                                    enter = fadeIn(animationSpec = tween(400)),
                                ) {
                                    TarotReadingSection(
                                        title = strings.pastLabel,
                                        body = cardsByPosition[TarotCardPosition.PAST]?.meaning.orEmpty(),
                                    )
                                }
                                AnimatedVisibility(
                                    visible = sectionVisibility[1],
                                    enter = fadeIn(animationSpec = tween(400)),
                                ) {
                                    TarotReadingSection(
                                        title = strings.presentLabel,
                                        body = cardsByPosition[TarotCardPosition.PRESENT]?.meaning.orEmpty(),
                                    )
                                }
                                AnimatedVisibility(
                                    visible = sectionVisibility[2],
                                    enter = fadeIn(animationSpec = tween(400)),
                                ) {
                                    TarotReadingSection(
                                        title = strings.futureLabel,
                                        body = cardsByPosition[TarotCardPosition.FUTURE]?.meaning.orEmpty(),
                                    )
                                }
                                AnimatedVisibility(
                                    visible = sectionVisibility[3],
                                    enter = fadeIn(animationSpec = tween(400)),
                                ) {
                                    TarotReadingSection(title = strings.summaryTitle, body = details.summary)
                                }
                                AnimatedVisibility(
                                    visible = sectionVisibility[4],
                                    enter = fadeIn(animationSpec = tween(400)),
                                ) {
                                    TarotReadingSection(title = strings.adviceTitle, body = details.advice)
                                }
                                if (shareText.isNotBlank()) {
                                    Button(
                                        onClick = {
                                            shareErrorMessage = null
                                            shareScope.launch {
                                                val shareResult = shareLauncher.shareText(
                                                    ShareTextPayload(text = shareText, title = shareTitle),
                                                )
                                                if (shareResult is ShareResult.Error) {
                                                    shareErrorMessage = shareResult.message ?: shareErrorFallback
                                                }
                                            }
                                        },
                                        modifier = Modifier.fillMaxWidth(),
                                    ) { Text(shareTitle) }
                                }
                            }
                        }

                        null -> {
                            if (response.interpretation.isNotBlank()) {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(strings.readingTitle, style = MaterialTheme.typography.titleMedium)
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

            if (showDailyLimitPaywall) {
                DailyLimitPaywallCard(
                    economyStrings = appStrings.economy,
                    onOpenStore = onOpenStore,
                    module = state.selectedType.name,
                    placement = "tarot_daily_limit",
                    reason = "daily_limit",
                    hasPremiumBenefit = state.selectedType == TarotRequestType.TAROT_3,
                    onPaywallShown = economyViewModel::onDailyLimitPaywallShown,
                    onPaywallActionClicked = economyViewModel::onDailyLimitPaywallActionClicked,
                )
            }

            state.error?.takeUnless { it == TAROT_LIMIT_REACHED_ERROR_KEY }?.let {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        "${strings.errorPrefix} ${strings.unknownErrorFallback}",
                        color = MaterialTheme.colorScheme.error,
                    )
                    Button(
                        onClick = viewModel::retry,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !state.isLoading && state.requestId != null,
                    ) {
                        Text(strings.retryCta)
                    }
                }
            }
            shareErrorMessage?.let {
                Text(
                    text = it,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                )
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
                            val labels = remember(overlayCard.id, strings) {
                                tarotOverlayLabels(overlayCard, strings)
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
                                                    tarotHaptics.performRevealHaptic()
                                                    viewModel.revealNextCard()
                                                }
                                            }
                                        },
                                        deckId = state.selectedDeckId,
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
    if (rewardDialogRewards.isNotEmpty()) {
        DeckCardUnlockRewardDialog(
            strings = appStrings,
            rewards = rewardDialogRewards,
            onDismiss = { rewardDialogRewards = emptyList() },
            onOpenCollection = {
                rewardDialogRewards = emptyList()
                onOpenCollection()
            },
        )
    }
}

private fun buildTarotShareText(
    card: com.agc.bwitch.domain.tarot.TarotCard?,
    details: TarotReadingDetails.Tarot1ReadingDetails,
    fallbackTitle: String,
    cardLabel: String,
    orientationTitle: String,
    themeTitle: String,
    meaningTitle: String,
    adviceTitle: String,
    watchOutTitle: String,
    uprightLabel: String,
    reversedLabel: String,
    orientationUnknownLabel: String,
): String {
    if (card == null) return ""
    val lines = listOfNotNull(
        fallbackTitle.ifBlank { "Lectura de Tarot" },
        "$cardLabel: ${card.name}".takeIf { card.name.isNotBlank() },
        "$orientationTitle: ${card.orientationText(uprightLabel, reversedLabel, orientationUnknownLabel)}",
        "$themeTitle: ${details.theme}".takeIf { details.theme.isNotBlank() },
        "$meaningTitle: ${details.meaning}".takeIf { details.meaning.isNotBlank() },
        "$adviceTitle: ${details.advice}".takeIf { details.advice.isNotBlank() },
        "$watchOutTitle: ${details.watchOut}".takeIf { details.watchOut.isNotBlank() },
    )
    return lines.joinToString("\n\n")
}

private fun buildTarotShareText(
    cards: List<com.agc.bwitch.domain.tarot.TarotCard>,
    details: TarotReadingDetails.Tarot3ReadingDetails,
    fallbackTitle: String,
    pastLabel: String,
    presentLabel: String,
    futureLabel: String,
    cardLabel: String,
    orientationTitle: String,
    meaningTitle: String,
    summaryTitle: String,
    adviceTitle: String,
    uprightLabel: String,
    reversedLabel: String,
    orientationUnknownLabel: String,
): String {
    if (cards.isEmpty()) return ""
    val cardsByPosition = cards.associateBy { it.position }
    val detailsByPosition = details.cards.associateBy { it.position }
    fun section(label: String, position: TarotCardPosition): String? {
        val card = cardsByPosition[position] ?: return null
        val meaning = detailsByPosition[position]?.meaning.orEmpty()
        return listOfNotNull(
            label,
            "$cardLabel: ${card.name}".takeIf { card.name.isNotBlank() },
            "$orientationTitle: ${card.orientationText(uprightLabel, reversedLabel, orientationUnknownLabel)}",
            "$meaningTitle: $meaning".takeIf { meaning.isNotBlank() },
        ).joinToString("\n")
    }

    val blocks = listOfNotNull(
        fallbackTitle.ifBlank { "Lectura de Tarot" },
        section(pastLabel, TarotCardPosition.PAST),
        section(presentLabel, TarotCardPosition.PRESENT),
        section(futureLabel, TarotCardPosition.FUTURE),
        "$summaryTitle: ${details.summary}".takeIf { details.summary.isNotBlank() },
        "$adviceTitle: ${details.advice}".takeIf { details.advice.isNotBlank() },
    )
    return blocks.joinToString("\n\n")
}

private fun com.agc.bwitch.domain.tarot.TarotCard.orientationText(
    uprightLabel: String,
    reversedLabel: String,
    orientationUnknownLabel: String,
): String =
    when (upright) {
        true -> uprightLabel
        false -> reversedLabel
        null -> orientationUnknownLabel
    }

@Composable
private fun TarotDeckInteractionStage(
    content: @Composable () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = TAROT_DECK_STAGE_MIN_HEIGHT),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            content()
        }
    }
}

private val TAROT_DECK_STAGE_MIN_HEIGHT = 420.dp
private val TAROT_DECK_STAGE_CARD_WIDTH = 180.dp

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
    fontFamily = bwitchDisplayFontFamily(),
    fontWeight = fontWeight,
    letterSpacing = 0.08.em,
    textAlign = TextAlign.Center,
)

@Composable
private fun TarotReadingSection(
    title: String,
    body: String,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 10.dp,
                shape = RoundedCornerShape(18.dp),
                ambientColor = Color(0xFF9E84D8).copy(alpha = 0.24f),
                spotColor = Color(0xFF6046A8).copy(alpha = 0.20f),
            ),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f),
        ),
        border = BorderStroke(1.dp, Color(0xFFD7C4FF).copy(alpha = 0.26f)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "✦ ${title.uppercase()}",
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontFamily = bwitchDisplayFontFamily(),
                    letterSpacing = 0.05.em,
                    textAlign = TextAlign.Center,
                ),
            )
            Text(
                text = body,
                modifier = Modifier
                    .widthIn(max = 620.dp)
                    .align(Alignment.CenterHorizontally),
                style = MaterialTheme.typography.bodyLarge,
            )
        }
    }
}
