package com.agc.bwitch.presentation.economy

import com.agc.bwitch.domain.analytics.AnalyticsEvent
import com.agc.bwitch.domain.analytics.AnalyticsTracker
import com.agc.bwitch.domain.analytics.NoOpAnalyticsTracker
import com.agc.bwitch.domain.economy.EconomyClaimStatus
import com.agc.bwitch.domain.economy.EconomyModulePreview
import com.agc.bwitch.domain.economy.EconomyRepository
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EconomyUiState(
    val isLoading: Boolean = true,
    val hasUsableSnapshot: Boolean = false,
    val balance: Int = 0,
    val isPremium: Boolean = false,
    val dailyLoginClaimed: Boolean = false,
    val rewardedAdsRemaining: Int = 0,
    val isClaimingDailyLogin: Boolean = false,
    val isClaimingRewardedAd: Boolean = false,
    val modulePreviews: List<EconomyModulePreview> = emptyList(),
    val hasLoadedModulePreviews: Boolean = false,
    val lastClaimResult: EconomyClaimUiResult? = null,
    val error: String? = null,
) {
    val hasStorePendingClaim: Boolean
        get() = !dailyLoginClaimed || rewardedAdsRemaining > 0
}

data class EconomyClaimUiResult(
    val action: EconomyClaimAction,
    val status: EconomyClaimUiStatus,
)

enum class EconomyClaimAction {
    DAILY_LOGIN,
    REWARDED_AD,
}

enum class EconomyClaimUiStatus {
    CLAIMED,
    ALREADY_CLAIMED,
    DAILY_LIMIT_REACHED,
}

class EconomyViewModel(
    private val economyRepository: EconomyRepository,
    private val analyticsTracker: AnalyticsTracker = NoOpAnalyticsTracker,
    private val autoLoadOnInit: Boolean = false,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _uiState = MutableStateFlow(EconomyUiState())
    val uiState: StateFlow<EconomyUiState> = _uiState.asStateFlow()
    private val _moonPaywallRequest = MutableStateFlow<MoonPaywallRequest?>(null)
    val moonPaywallRequest: StateFlow<MoonPaywallRequest?> = _moonPaywallRequest.asStateFlow()
    private var pendingMoonAction: PendingMoonAction? = null
    private var lastTrackedBalanceSnapshot: EconomyTrackedSnapshot? = null
    private var moonPaywallImpressionCounter: Long = 0L

    init {
        if (autoLoadOnInit) {
            loadEconomy()
        }

        scope.launch {
            uiState
                .map { it.hasUsableSnapshot to it.balance }
                .distinctUntilChanged()
                .collectLatest { (hasUsableSnapshot, balance) ->
                    evaluatePendingMoonAction(
                        hasUsableSnapshot = hasUsableSnapshot,
                        balance = balance,
                    )

                    val request = _moonPaywallRequest.value ?: return@collectLatest
                    if (hasUsableSnapshot && balance >= request.requiredMoons) {
                        _moonPaywallRequest.value = null
                    }
                }
        }
    }

    fun requireLunas(
        cost: Int,
        source: String? = null,
        onSuccess: (MoonUnlockFlowContext) -> Unit,
    ): Boolean {
        val currentState = _uiState.value
        val hasEnoughMoons = currentState.hasUsableSnapshot && currentState.balance >= cost
        val directBalanceOrigin = if (currentState.isPremium) {
            UNLOCK_FLOW_ORIGIN_PREMIUM
        } else if (hasEnoughMoons) {
            UNLOCK_FLOW_ORIGIN_DIRECT_BALANCE
        } else {
            UNLOCK_FLOW_ORIGIN_UNKNOWN
        }
        analyticsTracker.track(
            AnalyticsEvent.ContentUnlockAttempt(
                module = normalizeMoonPaywallModule(source),
                cost = cost,
                hasEnoughMoons = hasEnoughMoons,
                isPremium = currentState.isPremium,
                unlockFlowOrigin = directBalanceOrigin,
            ),
        )
        if (hasEnoughMoons) {
            onSuccess(
                MoonUnlockFlowContext(
                    source = source,
                    unlockFlowOrigin = directBalanceOrigin,
                    paywallImpressionId = null,
                    lastPaywallAction = null,
                ),
            )
            return true
        }

        pendingMoonAction = PendingMoonAction(
            requiredMoons = cost,
            source = source,
            onSuccess = onSuccess,
        )
        if (currentState.hasUsableSnapshot) {
            _moonPaywallRequest.value = newMoonPaywallRequest(
                requiredMoons = cost,
                source = source,
            )
        } else if (!currentState.isLoading) {
            loadEconomy()
        }
        return false
    }

    fun onMoonPaywallShown(request: MoonPaywallRequest) {
        analyticsTracker.track(
            AnalyticsEvent.PaywallShown(
                placement = MOON_PAYWALL_PLACEMENT,
                module = normalizeMoonPaywallModule(request.source),
                reason = MOON_PAYWALL_REASON_INSUFFICIENT_MOONS,
                paywallImpressionId = request.impressionId,
            ),
        )
    }

    fun onMoonPaywallActionClicked(
        request: MoonPaywallRequest,
        action: String,
    ) {
        pendingMoonAction = pendingMoonAction?.let { pending ->
            if (pending.requiredMoons == request.requiredMoons && pending.source == request.source) {
                pending.copy(
                    paywallImpressionId = request.impressionId,
                    lastPaywallAction = action,
                )
            } else {
                pending
            }
        }
        analyticsTracker.track(
            AnalyticsEvent.PaywallActionClicked(
                placement = MOON_PAYWALL_PLACEMENT,
                module = normalizeMoonPaywallModule(request.source),
                action = action,
                paywallImpressionId = request.impressionId,
            ),
        )
    }

    fun onDailyLimitPaywallShown(
        module: String,
        placement: String,
        reason: String,
    ) {
        analyticsTracker.track(
            AnalyticsEvent.PaywallShown(
                placement = placement,
                module = module,
                reason = reason,
            ),
        )
    }

    fun onDailyLimitPaywallActionClicked(
        module: String,
        placement: String,
        action: String,
    ) {
        analyticsTracker.track(
            AnalyticsEvent.PaywallActionClicked(
                placement = placement,
                module = module,
                action = action,
            ),
        )
    }

    fun onRewardedAdCtaShown(
        placement: String,
        rewardedAdsRemaining: Int?,
        paywallImpressionId: String? = null,
    ) {
        val remaining = rewardedAdsRemaining ?: return
        if (remaining < 0) return
        analyticsTracker.track(
            AnalyticsEvent.RewardedAdCtaShown(
                placement = placement,
                rewardedAdsRemaining = remaining,
                paywallImpressionId = paywallImpressionId,
            ),
        )
    }

    fun dismissMoonPaywall() {
        _moonPaywallRequest.value = null
        pendingMoonAction = null
    }

    private fun evaluatePendingMoonAction(
        hasUsableSnapshot: Boolean,
        balance: Int,
    ) {
        val action = pendingMoonAction ?: return
        if (!hasUsableSnapshot) return

        if (balance >= action.requiredMoons) {
            pendingMoonAction = null
            _moonPaywallRequest.value = null
            val unlockFlowOrigin = when {
                action.lastPaywallAction == PAYWALL_ACTION_WATCH_AD -> UNLOCK_FLOW_ORIGIN_PAYWALL_REWARDED
                action.lastPaywallAction != null -> UNLOCK_FLOW_ORIGIN_UNKNOWN
                else -> UNLOCK_FLOW_ORIGIN_UNKNOWN
            }
            action.onSuccess(
                MoonUnlockFlowContext(
                    source = action.source,
                    unlockFlowOrigin = unlockFlowOrigin,
                    paywallImpressionId = action.paywallImpressionId,
                    lastPaywallAction = action.lastPaywallAction,
                ),
            )
            return
        }

        if (_moonPaywallRequest.value == null) {
            _moonPaywallRequest.value = newMoonPaywallRequest(
                requiredMoons = action.requiredMoons,
                source = action.source,
            )
        }
    }

    private fun newMoonPaywallRequest(
        requiredMoons: Int,
        source: String?,
    ): MoonPaywallRequest {
        moonPaywallImpressionCounter += 1
        return MoonPaywallRequest(
            requiredMoons = requiredMoons,
            source = source,
            impressionId = "moon-paywall-${moonPaywallImpressionCounter}",
        )
    }

    fun loadEconomy() {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val statusDeferred = async { runCatching { economyRepository.getStatus() } }
            val balanceDeferred = async { runCatching { economyRepository.getBalance() } }
            val previewsDeferred = async {
                runCatching {
                    economyRepository.getModulePreviews(DEBUG_MONETIZABLE_MODULES)
                }
            }

            val statusResult = statusDeferred.await()
            val balanceResult = balanceDeferred.await()
            val previewsResult = previewsDeferred.await()

            statusResult.exceptionOrNull()?.let { println("BWITCH_ECONOMY_DEBUG getStatus failed=$it message=${it.message}") }
            balanceResult.exceptionOrNull()?.let { println("BWITCH_ECONOMY_DEBUG getBalance failed=$it message=${it.message}") }
            previewsResult.exceptionOrNull()?.let { println("BWITCH_ECONOMY_DEBUG getModulePreviews failed=$it message=${it.message}") }

            _uiState.update { state ->
                val status = statusResult.getOrNull()
                val balance = balanceResult.getOrNull()
                val hasUsableSnapshot = state.hasUsableSnapshot || status != null || balance != null

                state.copy(
                    isLoading = false,
                    hasUsableSnapshot = hasUsableSnapshot,
                    balance = balance?.balance ?: status?.balance ?: state.balance,
                    isPremium = status?.isPremium ?: state.isPremium,
                    dailyLoginClaimed = balance?.dailyLoginClaimed ?: state.dailyLoginClaimed,
                    rewardedAdsRemaining = balance?.rewardedAdsRemaining ?: state.rewardedAdsRemaining,
                    modulePreviews = previewsResult.getOrNull() ?: state.modulePreviews,
                    hasLoadedModulePreviews = state.hasLoadedModulePreviews || previewsResult.isSuccess,
                    error = if (statusResult.isFailure && balanceResult.isFailure) {
                        ECONOMY_LOAD_ERROR_KEY
                    } else {
                        null
                    },
                )
            }
            println(
                "BWITCH_PREMIUM_DEBUG economy_isPremium=${_uiState.value.isPremium} " +
                    "hasSnapshot=${_uiState.value.hasUsableSnapshot} balance=${_uiState.value.balance}"
            )
            _uiState.value.takeIf { it.hasUsableSnapshot }?.let { snapshot ->
                val trackedSnapshot = EconomyTrackedSnapshot(
                    balance = snapshot.balance,
                    isPremium = snapshot.isPremium,
                    rewardedAdsRemaining = snapshot.rewardedAdsRemaining,
                    dailyLoginClaimed = snapshot.dailyLoginClaimed,
                )
                if (trackedSnapshot != lastTrackedBalanceSnapshot) {
                    analyticsTracker.track(
                        AnalyticsEvent.EconomyBalanceViewed(
                            balance = snapshot.balance,
                            isPremium = snapshot.isPremium,
                        ),
                    )
                    lastTrackedBalanceSnapshot = trackedSnapshot
                }
            }
        }
    }

    fun claimDailyLogin() {
        val currentState = _uiState.value
        if (currentState.isClaimingDailyLogin || currentState.isLoading) return

        scope.launch {
            val requestId = generateRequestId()
            println("[EconomyViewModel] claimDailyLogin start requestId=$requestId")
            _uiState.update {
                it.copy(
                    isClaimingDailyLogin = true,
                    error = null,
                )
            }

            runCatching {
                economyRepository.claimDailyLogin(requestId = requestId)
            }.onSuccess { result ->
                println(
                    "[EconomyViewModel] claimDailyLogin success requestId=$requestId " +
                        "result=${result.result} balance=${result.balance}"
                )
                _uiState.update {
                    it.copy(
                        isClaimingDailyLogin = false,
                        hasUsableSnapshot = true,
                        balance = result.balance,
                        dailyLoginClaimed = result.dailyLoginClaimed,
                        rewardedAdsRemaining = result.rewardedAdsRemaining,
                        lastClaimResult = EconomyClaimUiResult(
                            action = EconomyClaimAction.DAILY_LOGIN,
                            status = result.result.toUiStatus(),
                        ),
                        error = null,
                    )
                }
                val currentBalance = _uiState.value.balance
                if (result.result == EconomyClaimStatus.CLAIMED) {
                    val rewardAmount = (currentBalance - currentState.balance).takeIf { it > 0 }
                    analyticsTracker.track(
                        AnalyticsEvent.MoonEarned(
                            source = "daily_login",
                            amount = rewardAmount,
                            balanceAfter = currentBalance.takeIf { rewardAmount != null },
                        ),
                    )
                }
                refreshEconomySnapshot()
            }.onFailure { error ->
                println("[EconomyViewModel] claimDailyLogin failed requestId=$requestId: ${error.message}")
                _uiState.update {
                    it.copy(
                        isClaimingDailyLogin = false,
                        error = ECONOMY_CLAIM_ERROR_KEY,
                    )
                }
            }
        }
    }

    fun claimRewardedAd(
        placement: String? = REWARDED_AD_DEFAULT_PLACEMENT,
        adProof: String = REWARDED_AD_CALLBACK_PROOF,
        paywallImpressionId: String? = null,
    ) {
        val currentState = _uiState.value
        if (currentState.isClaimingRewardedAd || currentState.isLoading) return
        if (currentState.rewardedAdsRemaining <= 0) return

        scope.launch {
            val requestId = generateRequestId()
            val safePlacement = placement ?: REWARDED_AD_DEFAULT_PLACEMENT
            analyticsTracker.track(
                AnalyticsEvent.RewardedAdStarted(
                    placement = safePlacement,
                    paywallImpressionId = paywallImpressionId,
                ),
            )
            println("[EconomyViewModel] claimRewardedAd start requestId=$requestId placement=$placement")
            _uiState.update {
                it.copy(
                    isClaimingRewardedAd = true,
                    error = null,
                )
            }

            runCatching {
                economyRepository.claimRewardedAd(
                    requestId = requestId,
                    adProof = adProof,
                    placement = placement,
                )
            }.onSuccess { result ->
                println(
                    "[EconomyViewModel] claimRewardedAd success requestId=$requestId " +
                        "result=${result.result} balance=${result.balance}"
                )
                _uiState.update {
                    it.copy(
                        isClaimingRewardedAd = false,
                        hasUsableSnapshot = true,
                        balance = result.balance,
                        dailyLoginClaimed = result.dailyLoginClaimed,
                        rewardedAdsRemaining = result.rewardedAdsRemaining,
                        lastClaimResult = EconomyClaimUiResult(
                            action = EconomyClaimAction.REWARDED_AD,
                            status = result.result.toUiStatus(),
                        ),
                        error = null,
                    )
                }
                val previousBalance = currentState.balance
                val currentBalance = _uiState.value.balance
                if (result.result == EconomyClaimStatus.CLAIMED) {
                    analyticsTracker.track(
                        AnalyticsEvent.RewardedAdCompleted(
                            placement = safePlacement,
                            reward = (currentBalance - previousBalance).coerceAtLeast(0),
                            balanceAfter = currentBalance,
                            paywallImpressionId = paywallImpressionId,
                        ),
                    )
                    analyticsTracker.track(
                        AnalyticsEvent.MoonEarned(
                            source = "rewarded_ad:$safePlacement",
                            amount = (currentBalance - previousBalance).takeIf { it > 0 },
                            balanceAfter = currentBalance.takeIf { currentBalance > previousBalance },
                        ),
                    )
                } else {
                    analyticsTracker.track(
                        AnalyticsEvent.RewardedAdFailed(
                            placement = safePlacement,
                            reason = result.result.name.lowercase(),
                            paywallImpressionId = paywallImpressionId,
                        ),
                    )
                }
                refreshEconomySnapshot()
            }.onFailure { error ->
                println("[EconomyViewModel] claimRewardedAd failed requestId=$requestId: ${error.message}")
                analyticsTracker.track(
                    AnalyticsEvent.RewardedAdFailed(
                        placement = safePlacement,
                        reason = error.message ?: "unknown",
                        paywallImpressionId = paywallImpressionId,
                    ),
                )
                _uiState.update {
                    it.copy(
                        isClaimingRewardedAd = false,
                        error = ECONOMY_CLAIM_ERROR_KEY,
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun generateRequestId(): String = Uuid.random().toString()

    private data class EconomyTrackedSnapshot(
        val balance: Int,
        val isPremium: Boolean,
        val rewardedAdsRemaining: Int,
        val dailyLoginClaimed: Boolean,
    )

    private suspend fun refreshEconomySnapshot() {
        val statusResult = runCatching { economyRepository.getStatus() }
        val balanceResult = runCatching { economyRepository.getBalance() }

        statusResult.exceptionOrNull()?.let { println("[EconomyViewModel] refresh getStatus failed: ${it.message}") }
        balanceResult.exceptionOrNull()?.let { println("[EconomyViewModel] refresh getBalance failed: ${it.message}") }

        val status = statusResult.getOrNull()
        val balance = balanceResult.getOrNull()

        if (status != null || balance != null) {
            println(
                "[EconomyViewModel] refresh snapshot " +
                    "statusBalance=${status?.balance} balanceBalance=${balance?.balance}"
            )
        }

        _uiState.update { state ->
            val hasUsableSnapshot = state.hasUsableSnapshot || status != null || balance != null
            state.copy(
                hasUsableSnapshot = hasUsableSnapshot,
                balance = balance?.balance ?: status?.balance ?: state.balance,
                isPremium = status?.isPremium ?: state.isPremium,
                dailyLoginClaimed = balance?.dailyLoginClaimed ?: state.dailyLoginClaimed,
                rewardedAdsRemaining = balance?.rewardedAdsRemaining ?: state.rewardedAdsRemaining,
            )
        }
    }
}

data class MoonPaywallRequest(
    val requiredMoons: Int,
    val source: String? = null,
    val impressionId: String = "",
)

private data class PendingMoonAction(
    val requiredMoons: Int,
    val source: String?,
    val paywallImpressionId: String? = null,
    val lastPaywallAction: String? = null,
    val onSuccess: (MoonUnlockFlowContext) -> Unit,
)

data class MoonUnlockFlowContext(
    val source: String?,
    val unlockFlowOrigin: String,
    val paywallImpressionId: String? = null,
    val lastPaywallAction: String? = null,
)

const val ECONOMY_LOAD_ERROR_KEY = "economy.load.error"
const val ECONOMY_CLAIM_ERROR_KEY = "economy.claim.error"
const val REWARDED_AD_DEFAULT_PLACEMENT = "moon_store"
val DEBUG_MONETIZABLE_MODULES = listOf(
    "ORACLE_1Q",
    "TAROT_1",
    "TAROT_3",
    "HOROSCOPE_FUTURE_DAY",
    "BIRTH_ESSENCE",
    "BASIC_NATAL_CHART",
    "SYNASTRY",
    "PENDULUM",
)
private const val MOON_PAYWALL_PLACEMENT = "moon_paywall"
private const val MOON_PAYWALL_REASON_INSUFFICIENT_MOONS = "insufficient_moons"
private const val PAYWALL_ACTION_WATCH_AD = "watch_ad"
const val UNLOCK_FLOW_ORIGIN_DIRECT_BALANCE = "direct_balance"
const val UNLOCK_FLOW_ORIGIN_PAYWALL_REWARDED = "paywall_rewarded"
const val UNLOCK_FLOW_ORIGIN_PREMIUM = "premium"
const val UNLOCK_FLOW_ORIGIN_UNKNOWN = "unknown"

// TODO(monetization-ssv): replace with server-verified proof/SSV token.
const val REWARDED_AD_CALLBACK_PROOF = "android-admob-reward-callback-v1"

private fun normalizeMoonPaywallModule(source: String?): String = source.orEmpty().ifBlank { "unknown" }

private fun com.agc.bwitch.domain.economy.EconomyClaimStatus.toUiStatus(): EconomyClaimUiStatus {
    return when (this) {
        EconomyClaimStatus.CLAIMED -> EconomyClaimUiStatus.CLAIMED
        com.agc.bwitch.domain.economy.EconomyClaimStatus.ALREADY_CLAIMED -> EconomyClaimUiStatus.ALREADY_CLAIMED
        com.agc.bwitch.domain.economy.EconomyClaimStatus.DAILY_LIMIT_REACHED -> EconomyClaimUiStatus.DAILY_LIMIT_REACHED
    }
}
