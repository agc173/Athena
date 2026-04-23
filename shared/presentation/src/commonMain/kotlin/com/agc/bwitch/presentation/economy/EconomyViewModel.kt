package com.agc.bwitch.presentation.economy

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
    val lastClaimResult: EconomyClaimUiResult? = null,
    val error: String? = null,
)

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
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _uiState = MutableStateFlow(EconomyUiState())
    val uiState: StateFlow<EconomyUiState> = _uiState.asStateFlow()

    init {
        loadEconomy()
    }

    fun loadEconomy() {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            val statusDeferred = async { runCatching { economyRepository.getStatus() } }
            val balanceDeferred = async { runCatching { economyRepository.getBalance() } }

            val statusResult = statusDeferred.await()
            val balanceResult = balanceDeferred.await()

            statusResult.exceptionOrNull()?.let { println("[EconomyViewModel] getStatus failed: ${it.message}") }
            balanceResult.exceptionOrNull()?.let { println("[EconomyViewModel] getBalance failed: ${it.message}") }

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
                    error = if (statusResult.isFailure && balanceResult.isFailure) {
                        ECONOMY_LOAD_ERROR_KEY
                    } else {
                        null
                    },
                )
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

    fun claimRewardedAd(placement: String? = REWARDED_AD_DEFAULT_PLACEMENT) {
        val currentState = _uiState.value
        if (currentState.isClaimingRewardedAd || currentState.isLoading) return
        if (currentState.rewardedAdsRemaining <= 0) return

        scope.launch {
            val requestId = generateRequestId()
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
                    adProof = REWARDED_AD_PLACEHOLDER_PROOF,
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
                refreshEconomySnapshot()
            }.onFailure { error ->
                println("[EconomyViewModel] claimRewardedAd failed requestId=$requestId: ${error.message}")
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

const val ECONOMY_LOAD_ERROR_KEY = "economy.load.error"
const val ECONOMY_CLAIM_ERROR_KEY = "economy.claim.error"
const val REWARDED_AD_DEFAULT_PLACEMENT = "moon_store"

// TODO(economy-rewarded-ad): Replace placeholder proof with SDK validated reward token.
const val REWARDED_AD_PLACEHOLDER_PROOF = "client-placeholder-proof"

private fun com.agc.bwitch.domain.economy.EconomyClaimStatus.toUiStatus(): EconomyClaimUiStatus {
    return when (this) {
        com.agc.bwitch.domain.economy.EconomyClaimStatus.CLAIMED -> EconomyClaimUiStatus.CLAIMED
        com.agc.bwitch.domain.economy.EconomyClaimStatus.ALREADY_CLAIMED -> EconomyClaimUiStatus.ALREADY_CLAIMED
        com.agc.bwitch.domain.economy.EconomyClaimStatus.DAILY_LIMIT_REACHED -> EconomyClaimUiStatus.DAILY_LIMIT_REACHED
    }
}
