package com.agc.bwitch.presentation.moons

import com.agc.bwitch.domain.analytics.AnalyticsEvent
import com.agc.bwitch.domain.analytics.AnalyticsTracker
import com.agc.bwitch.domain.analytics.NoOpAnalyticsTracker
import com.agc.bwitch.domain.moons.GetMoonBalanceUseCase
import com.agc.bwitch.domain.moons.GetMoonPacksUseCase
import com.agc.bwitch.domain.moons.MoonPack
import com.agc.bwitch.domain.moons.ObserveMoonBalanceUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MoonStoreUiState(
    val isLoading: Boolean = true,
    val balance: Int = 0,
    val packs: List<MoonPack> = emptyList(),
    val feedbackMessage: String? = null,
)

class MoonStoreViewModel(
    private val getMoonPacks: GetMoonPacksUseCase,
    private val getMoonBalance: GetMoonBalanceUseCase,
    private val observeMoonBalance: ObserveMoonBalanceUseCase,
    private val analyticsTracker: AnalyticsTracker = NoOpAnalyticsTracker,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _uiState = MutableStateFlow(MoonStoreUiState())
    val uiState: StateFlow<MoonStoreUiState> = _uiState

    init {
        scope.launch {
            observeMoonBalance()
                .catch { }
                .collectLatest { balance ->
                    _uiState.update { it.copy(balance = balance.amount) }
                }
        }

        scope.launch {
            runCatching {
                val balance = getMoonBalance().amount
                val packs = getMoonPacks()
                balance to packs
            }.onSuccess { (balance, packs) ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        balance = balance,
                        packs = packs,
                    )
                }
                packs.forEach { pack ->
                    analyticsTracker.track(
                        AnalyticsEvent.MoonPackViewed(
                            packId = pack.id,
                            moons = pack.moons,
                            price = pack.displayPrice,
                        ),
                    )
                }
            }.onFailure {
                _uiState.update { state ->
                    state.copy(
                        isLoading = false,
                        feedbackMessage = STORE_LOAD_ERROR_KEY,
                    )
                }
            }
        }
    }

    fun onBuyPackClicked(packId: String) {
        _uiState.value.packs.firstOrNull { it.id == packId }?.let { pack ->
            analyticsTracker.track(
                AnalyticsEvent.MoonPackSelected(
                    packId = pack.id,
                    moons = pack.moons,
                    price = pack.displayPrice,
                ),
            )
            analyticsTracker.track(
                AnalyticsEvent.MoonPackPurchaseFailed(
                    packId = pack.id,
                    reason = "not_available",
                ),
            )
        }
        _uiState.update {
            it.copy(feedbackMessage = "$STORE_COMING_SOON_KEY:$packId")
        }
    }

    fun clearFeedback() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }
}

const val STORE_COMING_SOON_KEY = "store.purchase.coming_soon"
const val STORE_LOAD_ERROR_KEY = "store.load.error"
