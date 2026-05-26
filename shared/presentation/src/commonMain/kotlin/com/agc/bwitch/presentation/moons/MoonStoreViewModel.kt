package com.agc.bwitch.presentation.moons

import com.agc.bwitch.domain.analytics.AnalyticsEvent
import com.agc.bwitch.domain.analytics.AnalyticsTracker
import com.agc.bwitch.domain.analytics.NoOpAnalyticsTracker
import com.agc.bwitch.domain.moons.GetMoonBalanceUseCase
import com.agc.bwitch.domain.moons.GetMoonPacksUseCase
import com.agc.bwitch.domain.moons.MoonPack
import com.agc.bwitch.domain.moons.MoonPackClaimStatus
import com.agc.bwitch.domain.moons.MoonPackPurchaseRepository
import com.agc.bwitch.domain.moons.ObserveMoonBalanceUseCase
import com.agc.bwitch.domain.settings.GooglePlayPurchase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class MoonStoreUiState(
    val isLoading: Boolean = true,
    val isPurchaseInProgress: Boolean = false,
    val balance: Int = 0,
    val packs: List<MoonPack> = emptyList(),
    val feedbackMessage: String? = null,
)

sealed interface MoonStoreUiEffect {
    data class LaunchMoonPackPurchase(val productId: String) : MoonStoreUiEffect
    data object RefreshEconomy : MoonStoreUiEffect
    data class ConsumeMoonPackPurchase(val token: String) : MoonStoreUiEffect
}

class MoonStoreViewModel(
    private val getMoonPacks: GetMoonPacksUseCase,
    private val getMoonBalance: GetMoonBalanceUseCase,
    private val observeMoonBalance: ObserveMoonBalanceUseCase,
    private val moonPackPurchaseRepository: MoonPackPurchaseRepository,
    private val analyticsTracker: AnalyticsTracker = NoOpAnalyticsTracker,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _uiState = MutableStateFlow(MoonStoreUiState())
    val uiState: StateFlow<MoonStoreUiState> = _uiState

    private val _uiEffects = MutableSharedFlow<MoonStoreUiEffect>(extraBufferCapacity = 8)
    val uiEffects: SharedFlow<MoonStoreUiEffect> = _uiEffects

    init {
        observeBalance()
        reloadStore()
    }

    fun reloadStore() {
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
                trackMoonPackViewed(packs)
            }.onFailure {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        feedbackMessage = STORE_LOAD_ERROR_KEY,
                    )
                }
            }
        }
    }

    fun onBuyPackClicked(packId: String) {
        val pack = _uiState.value.packs.firstOrNull { it.productId == packId } ?: return

        analyticsTracker.track(
            AnalyticsEvent.MoonPackSelected(
                packId = pack.productId,
                moons = pack.moonAmount,
                price = pack.localizedPrice ?: "N/A",
            ),
        )
        analyticsTracker.track(AnalyticsEvent.MoonPackPurchaseStarted(pack.productId))

        _uiState.update { it.copy(isPurchaseInProgress = true) }
        _uiEffects.tryEmit(MoonStoreUiEffect.LaunchMoonPackPurchase(packId))
    }

    fun onPurchaseCompleted(purchase: GooglePlayPurchase) {
        scope.launch {
            runCatching {
                moonPackPurchaseRepository.claimGooglePlayMoonPackPurchase(purchase)
            }.onSuccess { claimResult ->
                handleClaimSuccess(purchase, claimResult.status, claimResult.shouldConsume)
            }.onFailure {
                onPurchaseFailed(purchase.productId, reason = "claim_failed")
            }
        }
    }

    fun onPurchaseCancelled(packId: String) {
        _uiState.update {
            it.copy(
                isPurchaseInProgress = false,
                feedbackMessage = STORE_PURCHASE_CANCELLED_KEY,
            )
        }
        analyticsTracker.track(AnalyticsEvent.MoonPackPurchaseFailed(packId = packId, reason = "cancelled"))
    }

    fun onPurchasePending(packId: String) {
        _uiState.update {
            it.copy(
                isPurchaseInProgress = false,
                feedbackMessage = STORE_PURCHASE_PENDING_KEY,
            )
        }
        analyticsTracker.track(AnalyticsEvent.MoonPackPurchaseFailed(packId = packId, reason = "pending"))
    }

    fun onPurchaseFailed(packId: String, reason: String = "failed") {
        _uiState.update {
            it.copy(
                isPurchaseInProgress = false,
                feedbackMessage = STORE_PURCHASE_FAILED_KEY,
            )
        }
        analyticsTracker.track(AnalyticsEvent.MoonPackPurchaseFailed(packId = packId, reason = reason))
    }

    fun onConsumeFailed() {
        _uiState.update {
            val feedback = if (it.feedbackMessage == STORE_PURCHASE_COMPLETED_KEY) {
                STORE_PURCHASE_COMPLETED_WITH_CONSUME_FAILED_KEY
            } else {
                STORE_PURCHASE_CONSUME_FAILED_KEY
            }
            it.copy(feedbackMessage = feedback)
        }
    }

    fun clearFeedback() {
        _uiState.update { it.copy(feedbackMessage = null) }
    }

    private fun observeBalance() {
        scope.launch {
            observeMoonBalance()
                .catch { }
                .collectLatest { balance ->
                    _uiState.update { it.copy(balance = balance.amount) }
                }
        }
    }

    private suspend fun handleClaimSuccess(
        purchase: GooglePlayPurchase,
        claimStatus: MoonPackClaimStatus,
        shouldConsume: Boolean,
    ) {
        _uiEffects.emit(MoonStoreUiEffect.RefreshEconomy)
        if (shouldConsume) {
            _uiEffects.emit(MoonStoreUiEffect.ConsumeMoonPackPurchase(purchase.purchaseToken))
        }

        _uiState.update {
            it.copy(
                isPurchaseInProgress = false,
                feedbackMessage = STORE_PURCHASE_COMPLETED_KEY,
            )
        }

        val pack = _uiState.value.packs.firstOrNull { it.productId == purchase.productId } ?: return
        if (claimStatus == MoonPackClaimStatus.CLAIMED || claimStatus == MoonPackClaimStatus.ALREADY_CLAIMED) {
            analyticsTracker.track(
                AnalyticsEvent.MoonPackPurchaseCompleted(
                    packId = pack.productId,
                    moons = pack.moonAmount,
                    price = pack.localizedPrice ?: "N/A",
                    currency = "N/A",
                ),
            )
        }
    }

    private fun trackMoonPackViewed(packs: List<MoonPack>) {
        packs.forEach { pack ->
            analyticsTracker.track(
                AnalyticsEvent.MoonPackViewed(
                    packId = pack.productId,
                    moons = pack.moonAmount,
                    price = pack.localizedPrice ?: "N/A",
                ),
            )
        }
    }
}

const val STORE_LOAD_ERROR_KEY = "store.load.error"
const val STORE_PURCHASE_CANCELLED_KEY = "store.purchase.cancelled"
const val STORE_PURCHASE_PENDING_KEY = "store.purchase.pending"
const val STORE_PURCHASE_FAILED_KEY = "store.purchase.failed"
const val STORE_PURCHASE_COMPLETED_KEY = "store.purchase.completed"
const val STORE_PURCHASE_CONSUME_FAILED_KEY = "store.purchase.consume_failed"
const val STORE_PURCHASE_COMPLETED_WITH_CONSUME_FAILED_KEY = "store.purchase.completed.consume_failed"
