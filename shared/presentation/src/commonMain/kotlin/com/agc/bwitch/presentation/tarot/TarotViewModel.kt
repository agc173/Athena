package com.agc.bwitch.presentation.tarot

import com.agc.bwitch.domain.shared.ApiResult
import com.agc.bwitch.domain.tarot.TarotDrawResponse
import com.agc.bwitch.domain.tarot.TarotRepository
import com.agc.bwitch.domain.tarot.TarotRequestType
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class TarotRevealPhase {
    IDLE,
    SHUFFLING,
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
)

class TarotViewModel(
    private val tarotRepository: TarotRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _uiState = MutableStateFlow(TarotUiState())
    val uiState: StateFlow<TarotUiState> = _uiState.asStateFlow()

    fun newRequest(type: TarotRequestType) {
        val requestId = generateRequestId()
        _uiState.update {
            it.copy(
                requestId = requestId,
                selectedType = type,
                response = null,
                error = null,
                revealPhase = TarotRevealPhase.SHUFFLING,
                revealedCardCount = 0,
                activeCardIndex = 0,
                activeCardRevealed = false,
            )
        }
        draw(requestId, type)
    }

    fun retry() {
        val currentState = _uiState.value
        val requestId = generateRequestId()
        _uiState.update {
            it.copy(
                requestId = requestId,
                revealPhase = TarotRevealPhase.SHUFFLING,
                revealedCardCount = 0,
                activeCardIndex = 0,
                activeCardRevealed = false,
                response = null,
                error = null,
            )
        }
        draw(requestId, currentState.selectedType)
    }

    fun revealNextCard() {
        _uiState.update { currentState ->
            val response = currentState.response ?: return@update currentState
            if (currentState.revealPhase != TarotRevealPhase.CARDS_READY) return@update currentState

            if (!currentState.activeCardRevealed) {
                currentState.copy(
                    activeCardRevealed = true,
                    revealedCardCount = (currentState.revealedCardCount + 1).coerceAtMost(response.cards.size),
                )
            } else if (currentState.activeCardIndex < response.cards.lastIndex) {
                currentState.copy(
                    activeCardIndex = currentState.activeCardIndex + 1,
                    activeCardRevealed = false,
                )
            } else {
                currentState
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
                )
            ) {
                is ApiResult.Ok -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            response = result.value,
                            error = null,
                            revealPhase = TarotRevealPhase.CARDS_READY,
                            revealedCardCount = 0,
                            activeCardIndex = 0,
                            activeCardRevealed = false,
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
                            error = result.error.message ?: "Unknown error",
                        )
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun generateRequestId(): String = Uuid.random().toString()
}
