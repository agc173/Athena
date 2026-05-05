package com.agc.bwitch.presentation.tarot

import com.agc.bwitch.domain.analytics.AnalyticsEvent
import com.agc.bwitch.domain.analytics.AnalyticsTracker
import com.agc.bwitch.domain.analytics.NoOpAnalyticsTracker
import com.agc.bwitch.domain.shared.ApiResult
import com.agc.bwitch.domain.localization.AppLanguage
import com.agc.bwitch.domain.localization.ObserveCurrentLanguageUseCase
import com.agc.bwitch.domain.localization.ResolveCurrentLanguageUseCase
import com.agc.bwitch.domain.moons.AddMoonsUseCase
import com.agc.bwitch.domain.moons.GetMoonBalanceUseCase
import com.agc.bwitch.domain.moons.MoonUnlockCostCatalog
import com.agc.bwitch.domain.moons.MoonUnlockFeature
import com.agc.bwitch.domain.moons.ObserveMoonBalanceUseCase
import com.agc.bwitch.domain.moons.SpendMoonsUseCase
import com.agc.bwitch.domain.tarot.TarotDrawResponse
import com.agc.bwitch.domain.tarot.TarotRepository
import com.agc.bwitch.domain.tarot.TarotRequestType
import com.agc.bwitch.domain.tarot.TarotSessionPhase
import com.agc.bwitch.domain.tarot.TarotSessionRepository
import com.agc.bwitch.domain.tarot.TarotSessionSnapshot
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
import kotlinx.datetime.Clock

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
    val moonBalance: Int = 0,
    val extraReadingCost: Int = MoonUnlockCostCatalog.costFor(MoonUnlockFeature.TarotExtraReading),
    val insufficientMoonsMessage: String? = null,
    val createdAtEpochMillis: Long? = null,
    val isSessionRestoreResolved: Boolean = false,
) {
    val hasActiveRecoverableSession: Boolean
        get() = requestId != null && (isLoading || response != null || revealPhase != TarotRevealPhase.IDLE)
}

class TarotViewModel(
    private val tarotRepository: TarotRepository,
    private val resolveCurrentLanguageUseCase: ResolveCurrentLanguageUseCase,
    private val observeCurrentLanguageUseCase: ObserveCurrentLanguageUseCase,
    private val observeMoonBalanceUseCase: ObserveMoonBalanceUseCase,
    private val getMoonBalanceUseCase: GetMoonBalanceUseCase,
    private val addMoonsUseCase: AddMoonsUseCase,
    private val spendMoonsUseCase: SpendMoonsUseCase,
    private val tarotSessionRepository: TarotSessionRepository,
    private val analyticsTracker: AnalyticsTracker = NoOpAnalyticsTracker,
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
            tarotSessionRepository.loadSession()?.let { restoreSession(it) }
            _uiState.update { it.copy(isSessionRestoreResolved = true) }
        }
        scope.launch {
            runCatching { resolveCurrentLanguageUseCase() }
                .onSuccess { language ->
                    currentLanguageCode.value = language.code
                }
        }

        scope.launch {
            observeCurrentLanguageUseCase()
                .map { it.code }
                .distinctUntilChanged()
                .collectLatest { languageCode ->
                    currentLanguageCode.value = languageCode
                }
        }

        scope.launch {
            observeMoonBalanceUseCase().collectLatest { balance ->
                _uiState.update { it.copy(moonBalance = balance.amount) }
            }
        }
    }

    fun startNew(type: TarotRequestType) {
        if (_uiState.value.isLoading) return
        startNewRequest(type)
    }

    private fun startNewRequest(type: TarotRequestType) {
        analyticsTracker.track(
            AnalyticsEvent.ModuleUsed(
                module = "tarot",
                action = "new_request_${type.name.lowercase()}",
            ),
        )
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
                insufficientMoonsMessage = null,
                createdAtEpochMillis = Clock.System.now().toEpochMilliseconds(),
            )
        }
        scope.launch {
            draw(requestId, type)
        }
    }

    fun openSaved() {
        if (_uiState.value.hasActiveRecoverableSession) return
        scope.launch {
            tarotSessionRepository.loadSession()?.let { restoreSession(it) }
        }
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
        persistSession(TarotSessionPhase.RESULT_READY_WAITING_SHUFFLE_REVEAL)

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
        persistSession(TarotSessionPhase.PARTIALLY_REVEALED)
    }

    fun closeOverlay() {
        _uiState.update {
            it.copy(overlayVisible = false)
        }
    }

    fun retry() {
        startNew(_uiState.value.selectedType)
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
        persistSession(TarotSessionPhase.COMPLETED_READING_VISIBLE)
    }

    private fun draw(requestId: String, type: TarotRequestType) {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val result = tarotRepository.tarotDraw(
                requestId = requestId,
                type = type,
                lang = currentLanguageCode.value,
            )

            when (result) {
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
                    if (type == TarotRequestType.TAROT_3) {
                        refreshMoonBalanceFromBackend()
                    }
                    persistSession(TarotSessionPhase.RESULT_READY_WAITING_SHUFFLE_REVEAL)
                }

                is ApiResult.Err -> {
                    val economyError = result.error.isEconomyRestriction()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            requestId = null,
                            response = null,
                            createdAtEpochMillis = null,
                            revealPhase = TarotRevealPhase.IDLE,
                            revealedCardCount = 0,
                            activeCardIndex = 0,
                            activeCardRevealed = false,
                            overlayVisible = false,
                            overlayCardIndex = null,
                            overlayCardRevealed = false,
                            openedMiniCardIndex = null,
                            insufficientMoonsMessage = if (economyError) {
                                TAROT_EXTRA_READING_NOT_ENOUGH_MOONS_KEY
                            } else {
                                null
                            },
                            error = if (economyError) {
                                null
                            } else {
                                TAROT_DRAW_ERROR_KEY
                            },
                        )
                    }
                    tarotSessionRepository.clearSession()
                    refreshMoonBalanceFromBackend()
                }
            }
        }
    }

    private fun restoreSession(snapshot: TarotSessionSnapshot) {
        _uiState.update {
            it.copy(
                requestId = snapshot.requestId,
                selectedType = snapshot.type,
                isLoading = snapshot.phase == TarotSessionPhase.PENDING_BACKEND_REQUEST,
                response = snapshot.response,
                revealPhase = runCatching { TarotRevealPhase.valueOf(snapshot.revealPhase) }.getOrDefault(TarotRevealPhase.WAITING_TO_SHUFFLE),
                revealedCardCount = snapshot.revealedCardCount,
                activeCardIndex = snapshot.activeCardIndex,
                activeCardRevealed = snapshot.activeCardRevealed,
                overlayVisible = snapshot.overlayVisible,
                overlayCardIndex = snapshot.overlayCardIndex,
                overlayCardRevealed = snapshot.overlayCardRevealed,
                openedMiniCardIndex = snapshot.openedMiniCardIndex,
                createdAtEpochMillis = snapshot.createdAtEpochMillis,
                isSessionRestoreResolved = true,
            )
        }
    }

    private fun persistSession(phase: TarotSessionPhase) {
        val s = _uiState.value
        val requestId = s.requestId ?: return
        scope.launch {
            val now = Clock.System.now().toEpochMilliseconds()
            tarotSessionRepository.saveSession(
                TarotSessionSnapshot(
                    requestId = requestId,
                    type = s.selectedType,
                    createdAtEpochMillis = s.createdAtEpochMillis ?: now,
                    updatedAtEpochMillis = now,
                    phase = phase,
                    response = s.response,
                    revealPhase = s.revealPhase.name,
                    revealedCardCount = s.revealedCardCount,
                    activeCardIndex = s.activeCardIndex,
                    activeCardRevealed = s.activeCardRevealed,
                    overlayVisible = s.overlayVisible,
                    overlayCardIndex = s.overlayCardIndex,
                    overlayCardRevealed = s.overlayCardRevealed,
                    openedMiniCardIndex = s.openedMiniCardIndex,
                ),
            )
        }
    }

    private suspend fun refreshMoonBalanceFromBackend() {
        val syncedBalance = runCatching { getMoonBalanceUseCase().amount }
            .getOrNull()
            ?: return
        _uiState.update { it.copy(moonBalance = syncedBalance) }
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun generateRequestId(): String = Uuid.random().toString()

    fun grantOneMoonFromFutureRewardedAd() {
        scope.launch {
            val previous = _uiState.value.moonBalance
            val balance = addMoonsUseCase(1)
            _uiState.update { it.copy(moonBalance = balance.amount) }
            analyticsTracker.track(
                AnalyticsEvent.RewardedAdCompleted(
                    placement = "tarot_extra_reading",
                    reward = (balance.amount - previous).coerceAtLeast(0),
                    balanceAfter = balance.amount,
                ),
            )
            analyticsTracker.track(
                AnalyticsEvent.MoonEarned(
                    source = "rewarded_ad:tarot_extra_reading",
                    amount = (balance.amount - previous).coerceAtLeast(0),
                    balanceAfter = balance.amount,
                ),
            )
        }
    }

    private companion object {
        const val MIN_SHUFFLE_DURATION_MS = 1200L
    }
}

const val TAROT_EXTRA_READING_NOT_ENOUGH_MOONS_KEY = "tarot.error.not_enough_moons"
const val TAROT_DRAW_ERROR_KEY = "tarot.error.draw_failed"

private fun com.agc.bwitch.domain.shared.ApiError.isEconomyRestriction(): Boolean {
    val normalizedMessage = message.orEmpty().lowercase()
    return "insufficient_moons" in normalizedMessage ||
        "not_enough_moons" in normalizedMessage ||
        "not enough moons" in normalizedMessage ||
        "insufficient moons" in normalizedMessage ||
        "moon_balance" in normalizedMessage
}
