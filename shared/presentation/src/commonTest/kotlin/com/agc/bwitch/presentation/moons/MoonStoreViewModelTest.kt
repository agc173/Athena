package com.agc.bwitch.presentation.moons

import com.agc.bwitch.domain.analytics.AnalyticsEvent
import com.agc.bwitch.domain.moons.GetMoonBalanceUseCase
import com.agc.bwitch.domain.moons.GetMoonPacksUseCase
import com.agc.bwitch.domain.moons.MoonBalance
import com.agc.bwitch.domain.moons.MoonPack
import com.agc.bwitch.domain.moons.MoonPackClaimResult
import com.agc.bwitch.domain.moons.MoonPackClaimStatus
import com.agc.bwitch.domain.moons.MoonPackPurchaseRepository
import com.agc.bwitch.domain.moons.MoonPackRepository
import com.agc.bwitch.domain.moons.MoonRepository
import com.agc.bwitch.domain.moons.ObserveMoonBalanceUseCase
import com.agc.bwitch.domain.moons.SpendMoonsResult
import com.agc.bwitch.domain.settings.GooglePlayPurchase
import com.agc.bwitch.domain.settings.GooglePlayPurchaseState
import com.agc.bwitch.presentation.analytics.FakeAnalyticsTracker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
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
    fun `clicking available moon pack starts purchase flow and emits launch effect`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val analytics = FakeAnalyticsTracker()
            val moonRepository = FakeMoonRepository()
            val packRepository = FakeMoonPackRepository()
            val purchaseRepository = FakeMoonPackPurchaseRepository()
            val viewModel = MoonStoreViewModel(
                getMoonPacks = GetMoonPacksUseCase(packRepository),
                getMoonBalance = GetMoonBalanceUseCase(moonRepository),
                observeMoonBalance = ObserveMoonBalanceUseCase(moonRepository),
                moonPackPurchaseRepository = purchaseRepository,
                analyticsTracker = analytics,
            )
            val effects = mutableListOf<MoonStoreUiEffect>()
            val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiEffects.collect { effects += it }
            }

            advanceUntilIdle()
            viewModel.onBuyPackClicked("bwitch_moons_pack_10")
            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.isPurchaseInProgress)
            assertTrue(effects.any { it == MoonStoreUiEffect.LaunchMoonPackPurchase("bwitch_moons_pack_10") })
            assertTrue(analytics.events.any { it is AnalyticsEvent.MoonPackSelected })
            assertTrue(analytics.events.any { it is AnalyticsEvent.MoonPackPurchaseStarted })
            assertTrue(analytics.events.none { it is AnalyticsEvent.MoonPackPurchaseCompleted })
            assertEquals(0, viewModel.uiState.value.balance)

            collectJob.cancel()
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `clicking unavailable moon pack does not start purchase flow`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val analytics = FakeAnalyticsTracker()
            val moonRepository = FakeMoonRepository()
            val packRepository = FakeMoonPackRepository()
            val purchaseRepository = FakeMoonPackPurchaseRepository()
            val viewModel = MoonStoreViewModel(
                getMoonPacks = GetMoonPacksUseCase(packRepository),
                getMoonBalance = GetMoonBalanceUseCase(moonRepository),
                observeMoonBalance = ObserveMoonBalanceUseCase(moonRepository),
                moonPackPurchaseRepository = purchaseRepository,
                analyticsTracker = analytics,
            )
            val effects = mutableListOf<MoonStoreUiEffect>()
            val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiEffects.collect { effects += it }
            }

            advanceUntilIdle()
            viewModel.onBuyPackClicked("unknown_pack")
            advanceUntilIdle()

            assertTrue(!viewModel.uiState.value.isPurchaseInProgress)
            assertTrue(effects.none { it is MoonStoreUiEffect.LaunchMoonPackPurchase })
            assertTrue(analytics.events.none { it is AnalyticsEvent.MoonPackSelected })
            assertTrue(analytics.events.none { it is AnalyticsEvent.MoonPackPurchaseStarted })

            collectJob.cancel()
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `completed purchase emits refresh and preserves completion feedback when consume fails`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val viewModel = MoonStoreViewModel(
                getMoonPacks = GetMoonPacksUseCase(FakeMoonPackRepository()),
                getMoonBalance = GetMoonBalanceUseCase(FakeMoonRepository()),
                observeMoonBalance = ObserveMoonBalanceUseCase(FakeMoonRepository()),
                moonPackPurchaseRepository = FakeMoonPackPurchaseRepository(),
                analyticsTracker = FakeAnalyticsTracker(),
            )
            val effects = mutableListOf<MoonStoreUiEffect>()
            val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                viewModel.uiEffects.collect { effects += it }
            }

            advanceUntilIdle()
            viewModel.onBuyPackClicked("bwitch_moons_pack_10")
            viewModel.onPurchaseCompleted(
                GooglePlayPurchase(
                    productId = "bwitch_moons_pack_10",
                    purchaseToken = "token",
                    purchaseState = GooglePlayPurchaseState.Purchased,
                    isAcknowledged = false,
                    orderId = "order-1",
                    packageName = "com.agc.bwitch",
                ),
            )
            advanceUntilIdle()

            assertTrue(effects.contains(MoonStoreUiEffect.RefreshEconomy))
            assertTrue(effects.contains(MoonStoreUiEffect.ConsumeMoonPackPurchase("token")))
            assertEquals(STORE_PURCHASE_COMPLETED_KEY, viewModel.uiState.value.feedbackMessage)

            viewModel.onConsumeFailed()
            assertEquals(STORE_PURCHASE_COMPLETED_WITH_CONSUME_FAILED_KEY, viewModel.uiState.value.feedbackMessage)
            assertFalse(viewModel.uiState.value.isPurchaseInProgress)

            collectJob.cancel()
        } finally {
            Dispatchers.resetMain()
        }
    }

    private class FakeMoonRepository : MoonRepository {
        private val balance = MutableStateFlow(MoonBalance(0))

        override suspend fun getBalance(): MoonBalance = balance.value
        override fun observeBalance(): Flow<MoonBalance> = balance
        override suspend fun addMoons(amount: Int): MoonBalance = MoonBalance(balance.value.amount + amount)
        override suspend fun spendMoons(amount: Int): SpendMoonsResult = SpendMoonsResult.Success(balance.value)
        override suspend fun hasEnough(amount: Int): Boolean = balance.value.amount >= amount
    }

    private class FakeMoonPackRepository : MoonPackRepository {
        override suspend fun getMoonPacks(): List<MoonPack> = listOf(
            MoonPack(
                productId = "bwitch_moons_pack_10",
                moonAmount = 10,
                label = "Starter",
                localizedPrice = "\$1.99",
                displayOrder = 1,
            ),
        )
    }

    private class FakeMoonPackPurchaseRepository : MoonPackPurchaseRepository {
        override suspend fun claimGooglePlayMoonPackPurchase(purchase: GooglePlayPurchase): MoonPackClaimResult =
            MoonPackClaimResult(
                status = MoonPackClaimStatus.CLAIMED,
                shouldConsume = true,
            )
    }
}
