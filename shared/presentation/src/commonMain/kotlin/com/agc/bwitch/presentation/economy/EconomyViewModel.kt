package com.agc.bwitch.presentation.economy

import com.agc.bwitch.domain.economy.EconomyRepository
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
    val balance: Int = 0,
    val isPremium: Boolean = false,
    val rewardedAdsRemaining: Int = 0,
    val error: String? = null,
)

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

                state.copy(
                    isLoading = false,
                    balance = status?.balance ?: balance?.balance ?: state.balance,
                    isPremium = status?.isPremium ?: state.isPremium,
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
}

const val ECONOMY_LOAD_ERROR_KEY = "economy.load.error"
