package com.agc.bwitch.presentation.tarotcollection

import com.agc.bwitch.domain.analytics.AnalyticsEvent
import com.agc.bwitch.domain.analytics.AnalyticsTracker
import com.agc.bwitch.domain.analytics.NoOpAnalyticsTracker
import com.agc.bwitch.domain.tarot.GetTarotDeckCollectionProgressUseCase
import com.agc.bwitch.presentation.base.BaseViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class TarotCollectionViewModel(
    private val getProgress: GetTarotDeckCollectionProgressUseCase,
    private val analyticsTracker: AnalyticsTracker = NoOpAnalyticsTracker,
) : BaseViewModel() {
    private val _uiState = MutableStateFlow(TarotCollectionUiState())
    val uiState: StateFlow<TarotCollectionUiState> = _uiState.asStateFlow()

    fun load() {
        scope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val progress = runCatching { getProgress() }.getOrDefault(emptyMap())
            _uiState.update { it.copy(isLoading = false, progressByTrackId = progress) }
        }
    }

    fun onGalleryOpened() = analyticsTracker.track(AnalyticsEvent.DeckGalleryOpened)
    fun onDeckDetailOpened(trackId: String) = analyticsTracker.track(AnalyticsEvent.DeckDetailOpened(trackId))
}

data class TarotCollectionUiState(
    val isLoading: Boolean = false,
    val progressByTrackId: Map<String, com.agc.bwitch.domain.tarot.TarotDeckCollectionProgress> = emptyMap(),
)
