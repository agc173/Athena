package com.agc.bwitch.presentation.economy

import com.agc.bwitch.domain.economy.EconomyBalance
import com.agc.bwitch.domain.economy.EconomyClaimResult
import com.agc.bwitch.domain.economy.EconomyClaimStatus
import com.agc.bwitch.domain.economy.EconomyRepository
import com.agc.bwitch.domain.economy.EconomyStatus
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class EconomyViewModelTest {

    @Test
    fun `claim daily login keeps claimed balance when status snapshot is stale`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repository = FakeEconomyRepository()
            val viewModel = EconomyViewModel(economyRepository = repository)

            advanceUntilIdle()
            assertEquals(10, viewModel.uiState.value.balance)

            viewModel.claimDailyLogin()
            advanceUntilIdle()

            assertEquals(11, viewModel.uiState.value.balance)
            assertEquals(true, viewModel.uiState.value.dailyLoginClaimed)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private class FakeEconomyRepository : EconomyRepository {
        private var statusCalls: Int = 0
        private var balanceCalls: Int = 0

        override suspend fun getBalance(): EconomyBalance {
            balanceCalls += 1
            val balance = if (balanceCalls == 1) 10 else 11
            return EconomyBalance(
                balance = balance,
                dailyLoginClaimed = balance >= 11,
                rewardedAdsClaimed = 0,
                rewardedAdsRemaining = 1,
            )
        }

        override suspend fun getStatus(): EconomyStatus {
            statusCalls += 1
            val staleBalance = if (statusCalls == 1) 10 else 10
            return EconomyStatus(
                balance = staleBalance,
                isPremium = false,
                todayDateIso = "2026-04-23",
            )
        }

        override suspend fun claimDailyLogin(requestId: String): EconomyClaimResult {
            return EconomyClaimResult(
                result = EconomyClaimStatus.CLAIMED,
                balance = 11,
                dailyLoginClaimed = true,
                rewardedAdsClaimed = 0,
                rewardedAdsRemaining = 1,
            )
        }

        override suspend fun claimRewardedAd(
            requestId: String,
            adProof: String,
            placement: String?,
        ): EconomyClaimResult {
            return EconomyClaimResult(
                result = EconomyClaimStatus.CLAIMED,
                balance = 11,
                dailyLoginClaimed = true,
                rewardedAdsClaimed = 1,
                rewardedAdsRemaining = 0,
            )
        }
    }
}
