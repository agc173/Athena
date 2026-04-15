package com.agc.bwitch.presentation.tarot

import com.agc.bwitch.domain.shared.ApiResult
import com.agc.bwitch.domain.localization.AppLanguage
import com.agc.bwitch.domain.localization.ObserveCurrentLanguageUseCase
import com.agc.bwitch.domain.localization.ResolveCurrentLanguageUseCase
import com.agc.bwitch.domain.tarot.TarotDrawResponse
import com.agc.bwitch.domain.tarot.TarotRepository
import com.agc.bwitch.domain.tarot.TarotRequestType
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class TarotRevealPhase {
    IDLE,
    WAITING_TO_SHUFFLE,
    SHUFFLING,
    WAITING_TO_REVEAL,
    CARDS_READY,
    READING_VISIBLE,
}

data class TarotUiState(
    val requestId: String? = null,
    val selectedType: TarotRequestType = TarotRequestType.TAROT_1,
    val isLoading: Boolean = false,
    val response: TarotDrawResponse? = null,
    val error: String? = null,
    val revealPhase: TarotRevealPhase = TarotRevealPhase.IDLE,
    val revealedCardCount: Int = 0,
    val activeCardIndex: Int = 0,
    val activeCardRevealed: Boolean = false,
    val overlayVisible: Boolean = false,
    val overlayCardIndex: Int? = null,
    val overlayCardRevealed: Boolean = false,
    val openedMiniCardIndex: Int? = null,
)

class TarotViewModel(
    private val tarotRepository: TarotRepository,
    private val resolveCurrentLanguageUseCase: ResolveCurrentLanguageUseCase,
    private val observeCurrentLanguageUseCase: ObserveCurrentLanguageUseCase,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var shuffleDelayJob: Job? = null
    private var shuffleMinDurationElapsed = false
    private var shuffleRequestId: String? = null
    private val currentLanguageCode = MutableStateFlow(AppLanguage.fallback.code)

    private val _uiState = MutableStateFlow(TarotUiState())
    val uiState: StateFlow<TarotUiState> = _uiState.asStateFlow()

    init {
        scope.launch {
            runCatching { resolveCurrentLanguageUseCase() }
        }

        scope.launch {
            observeCurrentLanguageUseCase()
                .map { it.code }
                .distinctUntilChanged()
                .collectLatest { languageCode ->
                    currentLanguageCode.value = languageCode
                }
        }
    }

    fun newRequest(type: TarotRequestType) {
        shuffleDelayJob?.cancel()
        shuffleMinDurationElapsed = false
        shuffleRequestId = null

        val requestId = generateRequestId()
        _uiState.update {
            it.copy(
                requestId = requestId,
                selectedType = type,
                response = null,
                error = null,
                revealPhase = TarotRevealPhase.WAITING_TO_SHUFFLE,
                revealedCardCount = 0,
                activeCardIndex = 0,
                activeCardRevealed = false,
                overlayVisible = false,
                overlayCardIndex = null,
                overlayCardRevealed = false,
                openedMiniCardIndex = null,
            )
        }
        draw(requestId, type)
    }

    fun startShuffle() {
        var startedShuffle = false
        val requestId = _uiState.value.requestId
        _uiState.update { currentState ->
            if (currentState.revealPhase != TarotRevealPhase.WAITING_TO_SHUFFLE) return@update currentState

            startedShuffle = true
            currentState.copy(revealPhase = TarotRevealPhase.SHUFFLING)
        }
        if (!startedShuffle) return

        shuffleDelayJob?.cancel()
        shuffleMinDurationElapsed = false
        shuffleRequestId = requestId
        shuffleDelayJob = scope.launch {
            delay(MIN_SHUFFLE_DURATION_MS)
            shuffleMinDurationElapsed = true

            _uiState.update { currentState ->
                if (
                    currentState.revealPhase == TarotRevealPhase.SHUFFLING &&
                    currentState.requestId == shuffleRequestId &&
                    currentState.response != null
                ) {
                    currentState.copy(revealPhase = TarotRevealPhase.WAITING_TO_REVEAL)
                } else {
                    currentState
                }
            }
        }
    }

    fun startReveal() {
        _uiState.update { currentState ->
            val response = currentState.response ?: return@update currentState
            val canStartReveal = currentState.revealPhase == TarotRevealPhase.WAITING_TO_REVEAL ||
                currentState.revealPhase == TarotRevealPhase.CARDS_READY
            if (!canStartReveal || response.cards.isEmpty()) return@update currentState

            currentState.copy(
                revealPhase = TarotRevealPhase.CARDS_READY,
                activeCardIndex = 0,
                activeCardRevealed = false,
                overlayVisible = true,
                overlayCardIndex = 0,
                overlayCardRevealed = false,
            )
        }
    }

    fun closeOverlay() {
        _uiState.update {
            it.copy(overlayVisible = false)
        }
    }

    fun retry() {
        shuffleDelayJob?.cancel()
        shuffleMinDurationElapsed = false
        shuffleRequestId = null

        val currentState = _uiState.value
        val requestId = generateRequestId()
        _uiState.update {
            it.copy(
                requestId = requestId,
                revealPhase = TarotRevealPhase.WAITING_TO_SHUFFLE,
                revealedCardCount = 0,
                activeCardIndex = 0,
                activeCardRevealed = false,
                overlayVisible = false,
                overlayCardIndex = null,
                overlayCardRevealed = false,
                response = null,
                error = null,
                openedMiniCardIndex = null,
            )
        }
        draw(requestId, currentState.selectedType)
    }

    fun toggleMiniCard(index: Int) {
        _uiState.update {
            it.copy(
                openedMiniCardIndex = if (it.openedMiniCardIndex == index) {
                    null
                } else {
                    index
                },
            )
        }
    }

    fun revealNextCard() {
        _uiState.update { currentState ->
            val response = currentState.response ?: return@update currentState
            if (currentState.revealPhase != TarotRevealPhase.CARDS_READY) return@update currentState
            val overlayCardIndex = currentState.overlayCardIndex ?: return@update currentState
            if (!currentState.overlayVisible) return@update currentState

            if (!currentState.overlayCardRevealed) {
                currentState.copy(
                    activeCardIndex = overlayCardIndex,
                    activeCardRevealed = true,
                    overlayCardRevealed = true,
                    revealedCardCount = (currentState.revealedCardCount + 1).coerceAtMost(response.cards.size),
                )
            } else if (overlayCardIndex < response.cards.lastIndex) {
                currentState.copy(
                    activeCardIndex = overlayCardIndex + 1,
                    activeCardRevealed = false,
                    overlayCardIndex = overlayCardIndex + 1,
                    overlayCardRevealed = false,
                )
            } else {
                currentState.copy(
                    overlayVisible = false,
                    overlayCardIndex = null,
                    overlayCardRevealed = false,
                )
            }
        }
    }

    fun showReading() {
        _uiState.update { currentState ->
            val response = currentState.response ?: return@update currentState
            val allCardsRevealed = currentState.revealedCardCount >= response.cards.size
            if (!allCardsRevealed) return@update currentState

            currentState.copy(revealPhase = TarotRevealPhase.READING_VISIBLE)
        }
    }

    private fun draw(requestId: String, type: TarotRequestType) {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (
                val result = tarotRepository.tarotDraw(
                    requestId = requestId,
                    type = type,
                    lang = currentLanguageCode.value,
                )
            ) {
                is ApiResult.Ok -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            response = result.value,
                            error = null,
                            revealPhase = if (
                                it.revealPhase == TarotRevealPhase.SHUFFLING &&
                                shuffleMinDurationElapsed &&
                                it.requestId == shuffleRequestId
                            ) {
                                TarotRevealPhase.WAITING_TO_REVEAL
                            } else {
                                it.revealPhase
                            },
                            revealedCardCount = 0,
                            activeCardIndex = 0,
                            activeCardRevealed = false,
                            overlayVisible = false,
                            overlayCardIndex = null,
                            overlayCardRevealed = false,
                            openedMiniCardIndex = null,
                        )
                    }
                }

                is ApiResult.Err -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            response = null,
                            revealPhase = TarotRevealPhase.IDLE,
                            revealedCardCount = 0,
                            activeCardIndex = 0,
                            activeCardRevealed = false,
                            overlayVisible = false,
                            overlayCardIndex = null,
                            overlayCardRevealed = false,
                            openedMiniCardIndex = null,
                            error = result.error.message.orEmpty(),
                        )
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun generateRequestId(): String = Uuid.random().toString()

    private companion object {
        const val MIN_SHUFFLE_DURATION_MS = 1200L
    }
}
