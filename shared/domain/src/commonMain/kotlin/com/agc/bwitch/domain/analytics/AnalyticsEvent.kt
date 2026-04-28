package com.agc.bwitch.domain.analytics

sealed interface AnalyticsEvent {
    val name: String
    fun params(): Map<String, String>

    data class EconomyBalanceViewed(
        val balance: Int,
        val isPremium: Boolean,
    ) : AnalyticsEvent {
        override val name: String = "economy_balance_viewed"
        override fun params(): Map<String, String> = mapOf(
            "balance" to balance.toString(),
            "is_premium" to isPremium.toString(),
        )
    }

    data class MoonEarned(
        val source: String,
        val amount: Int?,
        val balanceAfter: Int?,
    ) : AnalyticsEvent {
        override val name: String = "moon_earned"
        override fun params(): Map<String, String> = buildMap {
            put("source", source)
            amount?.let { put("amount", it.toString()) }
            balanceAfter?.let { put("balance_after", it.toString()) }
        }
    }

    data class MoonSpent(
        val module: String,
        val cost: Int,
        val balanceAfter: Int,
    ) : AnalyticsEvent {
        override val name: String = "moon_spent"
        override fun params(): Map<String, String> = mapOf(
            "module" to module,
            "cost" to cost.toString(),
            "balance_after" to balanceAfter.toString(),
        )
    }

    data class RewardedAdCtaShown(
        val placement: String,
        val rewardedAdsRemaining: Int,
    ) : AnalyticsEvent {
        override val name: String = "rewarded_ad_cta_shown"
        override fun params(): Map<String, String> = mapOf(
            "placement" to placement,
            "rewarded_ads_remaining" to rewardedAdsRemaining.toString(),
        )
    }

    data class RewardedAdStarted(
        val placement: String,
    ) : AnalyticsEvent {
        override val name: String = "rewarded_ad_started"
        override fun params(): Map<String, String> = mapOf("placement" to placement)
    }

    data class RewardedAdCompleted(
        val placement: String,
        val reward: Int,
        val balanceAfter: Int,
    ) : AnalyticsEvent {
        override val name: String = "rewarded_ad_completed"
        override fun params(): Map<String, String> = mapOf(
            "placement" to placement,
            "reward" to reward.toString(),
            "balance_after" to balanceAfter.toString(),
        )
    }

    data class RewardedAdFailed(
        val placement: String,
        val reason: String,
    ) : AnalyticsEvent {
        override val name: String = "rewarded_ad_failed"
        override fun params(): Map<String, String> = mapOf(
            "placement" to placement,
            "reason" to reason,
        )
    }

    data class ContentUnlockAttempt(
        val module: String,
        val cost: Int,
        val hasEnoughMoons: Boolean?,
        val isPremium: Boolean,
    ) : AnalyticsEvent {
        override val name: String = "content_unlock_attempt"
        override fun params(): Map<String, String> = buildMap {
            put("module", module)
            put("cost", cost.toString())
            hasEnoughMoons?.let { put("has_enough_moons", it.toString()) }
            put("is_premium", isPremium.toString())
        }
    }

    data class ContentUnlocked(
        val module: String,
        val method: String,
        val costCharged: Int,
        val balanceAfter: Int?,
    ) : AnalyticsEvent {
        override val name: String = "content_unlocked"
        override fun params(): Map<String, String> = buildMap {
            put("module", module)
            put("method", method)
            put("cost_charged", costCharged.toString())
            balanceAfter?.let { put("balance_after", it.toString()) }
        }
    }

    data class ContentUnlockFailed(
        val module: String,
        val reason: String,
    ) : AnalyticsEvent {
        override val name: String = "content_unlock_failed"
        override fun params(): Map<String, String> = mapOf(
            "module" to module,
            "reason" to reason,
        )
    }

    data class PremiumCtaShown(
        val placement: String,
    ) : AnalyticsEvent {
        override val name: String = "premium_cta_shown"
        override fun params(): Map<String, String> = mapOf("placement" to placement)
    }

    data class PremiumCtaClicked(
        val placement: String,
    ) : AnalyticsEvent {
        override val name: String = "premium_cta_clicked"
        override fun params(): Map<String, String> = mapOf("placement" to placement)
    }

    data class PremiumPurchaseStarted(
        val productId: String,
    ) : AnalyticsEvent {
        override val name: String = "premium_purchase_started"
        override fun params(): Map<String, String> = mapOf("product_id" to productId)
    }

    data class PremiumPurchaseCompleted(
        val productId: String,
        val price: String?,
        val currency: String?,
    ) : AnalyticsEvent {
        override val name: String = "premium_purchase_completed"
        override fun params(): Map<String, String> = buildMap {
            put("product_id", productId)
            price?.let { put("price", it) }
            currency?.let { put("currency", it) }
        }
    }

    data class PremiumPurchaseFailed(
        val productId: String,
        val reason: String,
    ) : AnalyticsEvent {
        override val name: String = "premium_purchase_failed"
        override fun params(): Map<String, String> = mapOf(
            "product_id" to productId,
            "reason" to reason,
        )
    }

    data class MoonPackViewed(
        val packId: String,
        val moons: Int,
        val price: String,
    ) : AnalyticsEvent {
        override val name: String = "moon_pack_viewed"
        override fun params(): Map<String, String> = mapOf(
            "pack_id" to packId,
            "moons" to moons.toString(),
            "price" to price,
        )
    }

    data class MoonPackSelected(
        val packId: String,
        val moons: Int,
        val price: String,
    ) : AnalyticsEvent {
        override val name: String = "moon_pack_selected"
        override fun params(): Map<String, String> = mapOf(
            "pack_id" to packId,
            "moons" to moons.toString(),
            "price" to price,
        )
    }

    data class MoonPackPurchaseStarted(
        val packId: String,
    ) : AnalyticsEvent {
        override val name: String = "moon_pack_purchase_started"
        override fun params(): Map<String, String> = mapOf("pack_id" to packId)
    }

    data class MoonPackPurchaseCompleted(
        val packId: String,
        val moons: Int,
        val price: String,
        val currency: String,
    ) : AnalyticsEvent {
        override val name: String = "moon_pack_purchase_completed"
        override fun params(): Map<String, String> = mapOf(
            "pack_id" to packId,
            "moons" to moons.toString(),
            "price" to price,
            "currency" to currency,
        )
    }

    data class MoonPackPurchaseFailed(
        val packId: String,
        val reason: String,
    ) : AnalyticsEvent {
        override val name: String = "moon_pack_purchase_failed"
        override fun params(): Map<String, String> = mapOf(
            "pack_id" to packId,
            "reason" to reason,
        )
    }

    data class ModuleLimitReached(
        val module: String,
        val isPremium: Boolean,
    ) : AnalyticsEvent {
        override val name: String = "module_limit_reached"
        override fun params(): Map<String, String> = mapOf(
            "module" to module,
            "is_premium" to isPremium.toString(),
        )
    }

    data class PaywallShown(
        val placement: String,
        val module: String,
        val reason: String,
    ) : AnalyticsEvent {
        override val name: String = "paywall_shown"
        override fun params(): Map<String, String> = mapOf(
            "placement" to placement,
            "module" to module,
            "reason" to reason,
        )
    }

    data class PaywallActionClicked(
        val placement: String,
        val module: String,
        val action: String,
    ) : AnalyticsEvent {
        override val name: String = "paywall_action_clicked"
        override fun params(): Map<String, String> = mapOf(
            "placement" to placement,
            "module" to module,
            "action" to action,
        )
    }

    data class ModuleUsed(
        val module: String,
        val action: String,
    ) : AnalyticsEvent {
        override val name: String = "module_used"
        override fun params(): Map<String, String> = mapOf(
            "module" to module,
            "action" to action,
        )
    }
}
