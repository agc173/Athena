package com.agc.bwitch.domain.analytics

import kotlin.test.Test
import kotlin.test.assertEquals

class AnalyticsEventTest {

    @Test
    fun paywallShown_params_includeOptionalFieldsWhenPresent() {
        val event = AnalyticsEvent.PaywallShown(
            placement = "moon_paywall",
            module = "tarot_extra_reading",
            reason = "insufficient_moons",
            paywallImpressionId = "moon-paywall-1",
        )

        assertEquals(
            mapOf(
                "placement" to "moon_paywall",
                "module" to "tarot_extra_reading",
                "reason" to "insufficient_moons",
                "paywall_impression_id" to "moon-paywall-1",
            ),
            event.params(),
        )
    }

    @Test
    fun paywallShown_params_omitOptionalFieldsWhenNull_andKeepLegacyPayload() {
        val event = AnalyticsEvent.PaywallShown(
            placement = "moon_paywall",
            module = "tarot_extra_reading",
            reason = "insufficient_moons",
        )

        assertEquals(
            mapOf(
                "placement" to "moon_paywall",
                "module" to "tarot_extra_reading",
                "reason" to "insufficient_moons",
            ),
            event.params(),
        )
    }

    @Test
    fun rewardedAdCompleted_params_includeOptionalPaywallImpressionId() {
        val event = AnalyticsEvent.RewardedAdCompleted(
            placement = "contextual_paywall",
            reward = 1,
            balanceAfter = 10,
            paywallImpressionId = "moon-paywall-2",
        )

        assertEquals(
            mapOf(
                "placement" to "contextual_paywall",
                "reward" to "1",
                "balance_after" to "10",
                "paywall_impression_id" to "moon-paywall-2",
            ),
            event.params(),
        )
    }

    @Test
    fun contentUnlockEvents_params_omitOptionalFieldsWhenNull() {
        val attempt = AnalyticsEvent.ContentUnlockAttempt(
            module = "horoscope_daily",
            cost = 1,
            hasEnoughMoons = true,
            isPremium = false,
        )
        val unlocked = AnalyticsEvent.ContentUnlocked(
            module = "horoscope_daily",
            method = "moons",
            costCharged = 1,
            balanceAfter = 4,
        )
        val failed = AnalyticsEvent.ContentUnlockFailed(
            module = "horoscope_daily",
            reason = "insufficient_moons",
        )

        assertEquals(
            mapOf(
                "module" to "horoscope_daily",
                "cost" to "1",
                "has_enough_moons" to "true",
                "is_premium" to "false",
            ),
            attempt.params(),
        )
        assertEquals(
            mapOf(
                "module" to "horoscope_daily",
                "method" to "moons",
                "cost_charged" to "1",
                "balance_after" to "4",
            ),
            unlocked.params(),
        )
        assertEquals(
            mapOf(
                "module" to "horoscope_daily",
                "reason" to "insufficient_moons",
            ),
            failed.params(),
        )
    }

    @Test
    fun contentUnlockEvents_params_includeOptionalFieldsWhenPresent() {
        val attempt = AnalyticsEvent.ContentUnlockAttempt(
            module = "horoscope_daily",
            cost = 1,
            hasEnoughMoons = null,
            isPremium = true,
            unlockFlowOrigin = "paywall_rewarded",
            paywallImpressionId = "moon-paywall-3",
        )
        val unlocked = AnalyticsEvent.ContentUnlocked(
            module = "horoscope_daily",
            method = "premium",
            costCharged = 0,
            balanceAfter = null,
            unlockFlowOrigin = "premium",
            paywallImpressionId = "moon-paywall-3",
        )
        val failed = AnalyticsEvent.ContentUnlockFailed(
            module = "horoscope_daily",
            reason = "backend_error",
            unlockFlowOrigin = "paywall_store",
            paywallImpressionId = "moon-paywall-4",
        )

        assertEquals(
            mapOf(
                "module" to "horoscope_daily",
                "cost" to "1",
                "is_premium" to "true",
                "unlock_flow_origin" to "paywall_rewarded",
                "paywall_impression_id" to "moon-paywall-3",
            ),
            attempt.params(),
        )
        assertEquals(
            mapOf(
                "module" to "horoscope_daily",
                "method" to "premium",
                "cost_charged" to "0",
                "unlock_flow_origin" to "premium",
                "paywall_impression_id" to "moon-paywall-3",
            ),
            unlocked.params(),
        )
        assertEquals(
            mapOf(
                "module" to "horoscope_daily",
                "reason" to "backend_error",
                "unlock_flow_origin" to "paywall_store",
                "paywall_impression_id" to "moon-paywall-4",
            ),
            failed.params(),
        )
    }

    @Test
    fun premiumEvents_params_includeOptionalOriginPlacementWhenPresent() {
        val shown = AnalyticsEvent.PremiumCtaShown(
            placement = "settings_subscribe",
            originPlacement = "moon_paywall",
        )
        val clicked = AnalyticsEvent.PremiumCtaClicked(
            placement = "settings_catalog",
            originPlacement = "moon_paywall",
        )
        val started = AnalyticsEvent.PremiumPurchaseStarted(
            productId = "premium_monthly",
            originPlacement = "moon_paywall",
        )

        assertEquals(
            mapOf(
                "placement" to "settings_subscribe",
                "origin_placement" to "moon_paywall",
            ),
            shown.params(),
        )
        assertEquals(
            mapOf(
                "placement" to "settings_catalog",
                "origin_placement" to "moon_paywall",
            ),
            clicked.params(),
        )
        assertEquals(
            mapOf(
                "product_id" to "premium_monthly",
                "origin_placement" to "moon_paywall",
            ),
            started.params(),
        )
    }

    @Test
    fun premiumEvents_params_keepLegacyPayloadWhenOptionalOriginPlacementIsNull() {
        val shown = AnalyticsEvent.PremiumCtaShown(placement = "settings_subscribe")
        val clicked = AnalyticsEvent.PremiumCtaClicked(placement = "settings_catalog")
        val started = AnalyticsEvent.PremiumPurchaseStarted(productId = "premium_monthly")

        assertEquals(mapOf("placement" to "settings_subscribe"), shown.params())
        assertEquals(mapOf("placement" to "settings_catalog"), clicked.params())
        assertEquals(mapOf("product_id" to "premium_monthly"), started.params())
    }
}
