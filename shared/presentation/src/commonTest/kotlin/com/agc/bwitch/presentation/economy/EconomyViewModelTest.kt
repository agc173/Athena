package com.agc.bwitch.presentation.economy

import com.agc.bwitch.domain.analytics.AnalyticsEvent
import com.agc.bwitch.domain.economy.EconomyBalance
import com.agc.bwitch.domain.economy.EconomyClaimResult
import com.agc.bwitch.domain.economy.EconomyClaimStatus
import com.agc.bwitch.domain.economy.EconomyRepository
import com.agc.bwitch.domain.economy.EconomyStatus
import com.agc.bwitch.presentation.analytics.FakeAnalyticsTracker
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
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

    @Test
    fun `claim rewarded ad emits analytics completed event`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repository = FakeEconomyRepository()
            val analytics = FakeAnalyticsTracker()
            val viewModel = EconomyViewModel(
                economyRepository = repository,
                analyticsTracker = analytics,
            )

            advanceUntilIdle()
            viewModel.claimRewardedAd("economy_screen")
            advanceUntilIdle()

            assertTrue(analytics.events.any { it is com.agc.bwitch.domain.analytics.AnalyticsEvent.RewardedAdCompleted })
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `economy balance viewed is deduplicated for equal snapshots`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val analytics = FakeAnalyticsTracker()
            val viewModel = EconomyViewModel(
                economyRepository = StableEconomyRepository(),
                analyticsTracker = analytics,
            )
            advanceUntilIdle()
            viewModel.loadEconomy()
            advanceUntilIdle()

            val viewedEvents = analytics.events.filterIsInstance<com.agc.bwitch.domain.analytics.AnalyticsEvent.EconomyBalanceViewed>()
            assertEquals(1, viewedEvents.size)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `moon paywall shown emits normalized analytics payload`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val analytics = FakeAnalyticsTracker()
            val viewModel = EconomyViewModel(
                economyRepository = StableEconomyRepository(),
                analyticsTracker = analytics,
            )
            advanceUntilIdle()

            viewModel.onMoonPaywallShown(
                MoonPaywallRequest(
                    requiredMoons = 3,
                    source = "",
                    impressionId = "imp-001",
                ),
            )

            val event = analytics.events.last() as AnalyticsEvent.PaywallShown
            assertEquals("moon_paywall", event.placement)
            assertEquals("unknown", event.module)
            assertEquals("insufficient_moons", event.reason)
            assertEquals("imp-001", event.paywallImpressionId)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `moon paywall action clicked emits normalized analytics payload`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val analytics = FakeAnalyticsTracker()
            val viewModel = EconomyViewModel(
                economyRepository = StableEconomyRepository(),
                analyticsTracker = analytics,
            )
            advanceUntilIdle()

            viewModel.onMoonPaywallActionClicked(
                request = MoonPaywallRequest(
                    requiredMoons = 5,
                    source = "horoscope_daily_unlock",
                    impressionId = "imp-002",
                ),
                action = "watch_ad",
            )

            val event = analytics.events.last() as AnalyticsEvent.PaywallActionClicked
            assertEquals("moon_paywall", event.placement)
            assertEquals("horoscope_daily_unlock", event.module)
            assertEquals("watch_ad", event.action)
            assertEquals("imp-002", event.paywallImpressionId)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `claim rewarded ad includes paywall impression id on started and completed`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val analytics = FakeAnalyticsTracker()
            val viewModel = EconomyViewModel(
                economyRepository = FakeEconomyRepository(),
                analyticsTracker = analytics,
            )
            advanceUntilIdle()

            viewModel.claimRewardedAd(
                placement = "contextual_paywall",
                paywallImpressionId = "imp-003",
            )
            advanceUntilIdle()

            val started = analytics.events.filterIsInstance<AnalyticsEvent.RewardedAdStarted>().last()
            val completed = analytics.events.filterIsInstance<AnalyticsEvent.RewardedAdCompleted>().last()
            assertEquals("imp-003", started.paywallImpressionId)
            assertEquals("imp-003", completed.paywallImpressionId)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `claim rewarded ad includes paywall impression id on failed`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val analytics = FakeAnalyticsTracker()
            val viewModel = EconomyViewModel(
                economyRepository = StableEconomyRepository(),
                analyticsTracker = analytics,
            )
            advanceUntilIdle()

            viewModel.claimRewardedAd(
                placement = "contextual_paywall",
                paywallImpressionId = "imp-004",
            )
            advanceUntilIdle()

            val failed = analytics.events.filterIsInstance<AnalyticsEvent.RewardedAdFailed>().last()
            assertEquals("imp-004", failed.paywallImpressionId)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `claim rewarded ad keeps legacy payload when paywall impression id is omitted`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val analytics = FakeAnalyticsTracker()
            val viewModel = EconomyViewModel(
                economyRepository = StableEconomyRepository(),
                analyticsTracker = analytics,
            )
            advanceUntilIdle()

            viewModel.claimRewardedAd(placement = "moon_store")
            advanceUntilIdle()

            val started = analytics.events.filterIsInstance<AnalyticsEvent.RewardedAdStarted>().last()
            val failed = analytics.events.filterIsInstance<AnalyticsEvent.RewardedAdFailed>().last()
            assertEquals(null, started.paywallImpressionId)
            assertEquals(null, failed.paywallImpressionId)
            assertTrue("paywall_impression_id" !in started.params().keys)
            assertTrue("paywall_impression_id" !in failed.params().keys)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `rewarded ad cta shown emits analytics only when remaining is known`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val analytics = FakeAnalyticsTracker()
            val viewModel = EconomyViewModel(
                economyRepository = StableEconomyRepository(),
                analyticsTracker = analytics,
            )
            advanceUntilIdle()

            viewModel.onRewardedAdCtaShown(
                placement = "moon_store",
                rewardedAdsRemaining = null,
            )
            viewModel.onRewardedAdCtaShown(
                placement = "moon_store",
                rewardedAdsRemaining = 2,
            )

            val events = analytics.events.filterIsInstance<AnalyticsEvent.RewardedAdCtaShown>()
            assertEquals(1, events.size)
            assertEquals("moon_store", events.first().placement)
            assertEquals(2, events.first().rewardedAdsRemaining)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `moon paywall request gets unique impression id for equal source and cost`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val viewModel = EconomyViewModel(
                economyRepository = StableEconomyRepository(),
            )
            advanceUntilIdle()

            viewModel.requireLunas(cost = 99, source = "horoscope_daily_unlock") { _ -> }
            val firstRequest = viewModel.moonPaywallRequest.value
            viewModel.dismissMoonPaywall()
            viewModel.requireLunas(cost = 99, source = "horoscope_daily_unlock") { _ -> }
            val secondRequest = viewModel.moonPaywallRequest.value

            assertTrue(firstRequest != null)
            assertTrue(secondRequest != null)
            assertTrue(firstRequest.impressionId.isNotBlank())
            assertTrue(secondRequest.impressionId.isNotBlank())
            assertTrue(firstRequest.impressionId != secondRequest.impressionId)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `require lunas direct balance emits direct_balance origin`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val analytics = FakeAnalyticsTracker()
            val viewModel = EconomyViewModel(
                economyRepository = StableEconomyRepository(),
                analyticsTracker = analytics,
            )
            var callbackContext: MoonUnlockFlowContext? = null
            advanceUntilIdle()

            val unlocked = viewModel.requireLunas(cost = 5, source = "horoscope_daily_unlock") { context ->
                callbackContext = context
            }
            advanceUntilIdle()

            assertTrue(unlocked)
            assertEquals(UNLOCK_FLOW_ORIGIN_DIRECT_BALANCE, callbackContext?.unlockFlowOrigin)
            val attempt = analytics.events.filterIsInstance<AnalyticsEvent.ContentUnlockAttempt>().last()
            assertEquals(UNLOCK_FLOW_ORIGIN_DIRECT_BALANCE, attempt.unlockFlowOrigin)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `pending unlock after rewarded ad emits paywall_rewarded with impression id`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val viewModel = EconomyViewModel(
                economyRepository = FakeEconomyRepository(),
            )
            var callbackContext: MoonUnlockFlowContext? = null
            advanceUntilIdle()

            val unlocked = viewModel.requireLunas(cost = 11, source = "horoscope_daily_unlock") { context ->
                callbackContext = context
            }
            assertTrue(!unlocked)
            val request = viewModel.moonPaywallRequest.value
            assertNotNull(request)

            viewModel.onMoonPaywallActionClicked(request, action = "watch_ad")
            viewModel.claimRewardedAd(
                placement = "contextual_paywall",
                paywallImpressionId = request.impressionId,
            )
            advanceUntilIdle()

            assertEquals(null, viewModel.moonPaywallRequest.value)
            assertEquals(UNLOCK_FLOW_ORIGIN_PAYWALL_REWARDED, callbackContext?.unlockFlowOrigin)
            assertEquals(request.impressionId, callbackContext?.paywallImpressionId)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `pending unlock without paywall action emits unknown origin`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val viewModel = EconomyViewModel(
                economyRepository = FakeEconomyRepository(),
            )
            var callbackContext: MoonUnlockFlowContext? = null
            advanceUntilIdle()

            val unlocked = viewModel.requireLunas(cost = 11, source = "horoscope_daily_unlock") { context ->
                callbackContext = context
            }
            assertTrue(!unlocked)

            viewModel.claimDailyLogin()
            advanceUntilIdle()

            assertEquals(UNLOCK_FLOW_ORIGIN_UNKNOWN, callbackContext?.unlockFlowOrigin)
            assertEquals(null, callbackContext?.paywallImpressionId)
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

    private class StableEconomyRepository : EconomyRepository {
        override suspend fun getBalance(): EconomyBalance = EconomyBalance(
            balance = 20,
            dailyLoginClaimed = false,
            rewardedAdsClaimed = 0,
            rewardedAdsRemaining = 1,
        )

        override suspend fun getStatus(): EconomyStatus = EconomyStatus(
            balance = 20,
            isPremium = false,
            todayDateIso = "2026-04-23",
        )

        override suspend fun claimDailyLogin(requestId: String): EconomyClaimResult = EconomyClaimResult(
            result = EconomyClaimStatus.ALREADY_CLAIMED,
            balance = 20,
            dailyLoginClaimed = true,
            rewardedAdsClaimed = 0,
            rewardedAdsRemaining = 1,
        )

        override suspend fun claimRewardedAd(requestId: String, adProof: String, placement: String?): EconomyClaimResult =
            EconomyClaimResult(
                result = EconomyClaimStatus.ALREADY_CLAIMED,
                balance = 20,
                dailyLoginClaimed = false,
                rewardedAdsClaimed = 1,
                rewardedAdsRemaining = 0,
            )
    }
}
