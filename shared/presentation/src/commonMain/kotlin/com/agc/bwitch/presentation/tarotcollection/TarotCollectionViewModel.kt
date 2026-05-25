package com.agc.bwitch.presentation.tarotcollection

import com.agc.bwitch.domain.analytics.AnalyticsEvent
import com.agc.bwitch.domain.analytics.AnalyticsTracker
import com.agc.bwitch.domain.analytics.NoOpAnalyticsTracker
import com.agc.bwitch.domain.tarot.GetSelectedTarotDeckUseCase
import com.agc.bwitch.domain.tarot.GetTarotDeckCollectionProgressUseCase
import com.agc.bwitch.domain.tarot.SetSelectedTarotDeckUseCase
import com.agc.bwitch.domain.tarot.TarotDeckId
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TarotCollectionViewModel(
    private val getProgress: GetTarotDeckCollectionProgressUseCase,
    private val getSelectedDeck: GetSelectedTarotDeckUseCase,
    private val setSelectedDeck: SetSelectedTarotDeckUseCase,
    private val analyticsTracker: AnalyticsTracker = NoOpAnalyticsTracker,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val _uiState = MutableStateFlow(TarotCollectionUiState())
    val uiState: StateFlow<TarotCollectionUiState> = _uiState.asStateFlow()

    fun load() {
        scope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val selectedDeckId = runCatching { getSelectedDeck() }.getOrDefault(TarotDeckId.RIDER_WAITE)
            runCatching { getProgress() }
                .onSuccess { progress ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            progressByTrackId = progress,
                            selectedDeckId = selectedDeckId,
                        )
                    }
                }
                .onFailure {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            selectedDeckId = selectedDeckId,
                        )
                    }
                }
        }
    }

    fun selectDeck(deckId: TarotDeckId, isFullyUnlocked: Boolean) {
        val canUseDeck = deckId == TarotDeckId.RIDER_WAITE || isFullyUnlocked
        if (!canUseDeck) return

        setSelectedDeck(deckId)
        _uiState.update { it.copy(selectedDeckId = deckId) }
        analyticsTracker.track(
            AnalyticsEvent.DeckSelected(
                deckId = deckId.value,
                sourceScreen = "arcana_collection",
                isFullyUnlocked = isFullyUnlocked,
            ),
        )
    }

    fun onGalleryOpened() = analyticsTracker.track(AnalyticsEvent.DeckGalleryOpened)
    fun onDeckDetailOpened(trackId: String) = analyticsTracker.track(AnalyticsEvent.DeckDetailOpened(trackId))
}

data class TarotCollectionUiState(
    val isLoading: Boolean = false,
    val progressByTrackId: Map<String, com.agc.bwitch.domain.tarot.TarotDeckCollectionProgress> = emptyMap(),
    val selectedDeckId: TarotDeckId = TarotDeckId.RIDER_WAITE,
)
