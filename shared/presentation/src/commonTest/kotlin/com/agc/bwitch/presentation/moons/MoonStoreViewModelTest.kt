package com.agc.bwitch.presentation.moons

import com.agc.bwitch.domain.analytics.AnalyticsEvent
import com.agc.bwitch.domain.moons.GetMoonBalanceUseCase
import com.agc.bwitch.domain.moons.GetMoonPacksUseCase
import com.agc.bwitch.domain.moons.MoonBalance
import com.agc.bwitch.domain.moons.MoonPack
import com.agc.bwitch.domain.moons.MoonPackRepository
import com.agc.bwitch.domain.moons.MoonRepository
import com.agc.bwitch.domain.moons.ObserveMoonBalanceUseCase
import com.agc.bwitch.domain.moons.SpendMoonsResult
import com.agc.bwitch.presentation.analytics.FakeAnalyticsTracker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MoonStoreViewModelTest {

    @Test
    fun `moon pack click remains coming soon and does not start real pack purchase`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val moonRepository = FakeMoonRepository()
            val analytics = FakeAnalyticsTracker()
            val viewModel = MoonStoreViewModel(
                getMoonPacks = GetMoonPacksUseCase(FakeMoonPackRepository()),
                getMoonBalance = GetMoonBalanceUseCase(moonRepository),
                observeMoonBalance = ObserveMoonBalanceUseCase(moonRepository),
                analyticsTracker = analytics,
            )
            advanceUntilIdle()

            viewModel.onBuyPackClicked("starter")
            advanceUntilIdle()

            assertEquals("$STORE_COMING_SOON_KEY:starter", viewModel.uiState.value.feedbackMessage)
            assertTrue(analytics.events.any { it is AnalyticsEvent.MoonPackPurchaseFailed })
            assertFalse(analytics.events.any { it is AnalyticsEvent.MoonPackPurchaseStarted })
            assertFalse(analytics.events.any { it is AnalyticsEvent.MoonPackPurchaseCompleted })
        } finally {
            Dispatchers.resetMain()
        }
    }

    private class FakeMoonRepository : MoonRepository {
        private val balance = MutableStateFlow(MoonBalance(3))
        override suspend fun getBalance(): MoonBalance = balance.value
        override fun observeBalance(): Flow<MoonBalance> = balance
        override suspend fun addMoons(amount: Int): MoonBalance = MoonBalance(balance.value.amount + amount).also { balance.value = it }
        override suspend fun spendMoons(amount: Int): SpendMoonsResult = SpendMoonsResult.Success(MoonBalance(balance.value.amount - amount))
        override suspend fun hasEnough(amount: Int): Boolean = balance.value.amount >= amount
    }

    private class FakeMoonPackRepository : MoonPackRepository {
        override suspend fun getMoonPacks(): List<MoonPack> = listOf(
            MoonPack(
                id = "starter",
                moons = 10,
                label = "Starter",
                displayPrice = "Próximamente",
                displayOrder = 0,
            ),
        )
    }
}
