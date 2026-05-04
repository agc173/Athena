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
import com.agc.bwitch.domain.moons.SpendMoonsResult
import com.agc.bwitch.domain.moons.SpendMoonsUseCase
import com.agc.bwitch.domain.tarot.TarotDrawResponse
import com.agc.bwitch.domain.tarot.TarotRepository
import com.agc.bwitch.domain.tarot.TarotRequestType
import com.agc.bwitch.presentation.economy.UNLOCK_FLOW_ORIGIN_DIRECT_BALANCE
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
    val moonBalance: Int = 0,
    val extraReadingCost: Int = MoonUnlockCostCatalog.costFor(MoonUnlockFeature.TarotExtraReading),
    val insufficientMoonsMessage: String? = null,
)

class TarotViewModel(
    private val tarotRepository: TarotRepository,
    private val resolveCurrentLanguageUseCase: ResolveCurrentLanguageUseCase,
    private val observeCurrentLanguageUseCase: ObserveCurrentLanguageUseCase,
    private val observeMoonBalanceUseCase: ObserveMoonBalanceUseCase,
    private val getMoonBalanceUseCase: GetMoonBalanceUseCase,
    private val addMoonsUseCase: AddMoonsUseCase,
    private val spendMoonsUseCase: SpendMoonsUseCase,
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

    fun newRequest(type: TarotRequestType) {
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
            )
        }
        scope.launch {
            draw(requestId, type)
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
        newRequest(_uiState.value.selectedType)
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
                }

                is ApiResult.Err -> {
                    if (type == TarotRequestType.TAROT_3 && result.error.requiresLegacyAdUnlockFallback()) {
                        when (val spendResult = spendMoonsUseCase(_uiState.value.extraReadingCost)) {
                            is SpendMoonsResult.Success -> {
                                analyticsTracker.track(
                                    AnalyticsEvent.MoonSpent(
                                        module = "tarot",
                                        cost = _uiState.value.extraReadingCost,
                                        balanceAfter = spendResult.updatedBalance.amount,
                                    ),
                                )
                                analyticsTracker.track(
                                    AnalyticsEvent.ContentUnlocked(
                                        module = "tarot_extra_reading",
                                        method = "moons",
                                        costCharged = _uiState.value.extraReadingCost,
                                        balanceAfter = spendResult.updatedBalance.amount,
                                        unlockFlowOrigin = UNLOCK_FLOW_ORIGIN_DIRECT_BALANCE,
                                    ),
                                )
                                val retryResult = tarotRepository.tarotDraw(
                                    requestId = requestId,
                                    type = type,
                                    lang = currentLanguageCode.value,
                                )
                                if (retryResult is ApiResult.Ok) {
                                    _uiState.update {
                                        it.copy(
                                            isLoading = false,
                                            response = retryResult.value,
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
                                            insufficientMoonsMessage = null,
                                        )
                                    }
                                    return@launch
                                }
                                if (retryResult is ApiResult.Err) {
                                    val retryEconomyError = retryResult.error.isEconomyRestriction()
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
                                            insufficientMoonsMessage = if (retryEconomyError) {
                                                TAROT_EXTRA_READING_NOT_ENOUGH_MOONS_KEY
                                            } else {
                                                null
                                            },
                                            error = if (retryEconomyError) {
                                                null
                                            } else {
                                                TAROT_DRAW_ERROR_KEY
                                            },
                                        )
                                    }
                                    return@launch
                                }
                            }

                            is SpendMoonsResult.InsufficientBalance -> {
                                analyticsTracker.track(
                                    AnalyticsEvent.ModuleLimitReached(
                                        module = "tarot_extra_reading",
                                        isPremium = false,
                                    ),
                                )
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
                                        insufficientMoonsMessage = TAROT_EXTRA_READING_NOT_ENOUGH_MOONS_KEY,
                                        error = null,
                                    )
                                }
                                return@launch
                            }
                        }
                    }

                    val economyError = result.error.isEconomyRestriction()
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
                    refreshMoonBalanceFromBackend()
                }
            }
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

private fun com.agc.bwitch.domain.shared.ApiError.requiresLegacyAdUnlockFallback(): Boolean {
    val normalizedMessage = message.orEmpty().lowercase()
    return this is com.agc.bwitch.domain.shared.ApiError.FailedPrecondition &&
        (
            "rewardedproof" in normalizedMessage ||
                "rewarded proof" in normalizedMessage ||
                "ad_unlock" in normalizedMessage ||
                "ad unlock" in normalizedMessage
            )
}
