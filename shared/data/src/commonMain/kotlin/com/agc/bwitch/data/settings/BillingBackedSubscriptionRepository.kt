package com.agc.bwitch.data.settings

import com.agc.bwitch.data.settings.billing.SubscriptionBillingDataSource
import com.agc.bwitch.data.storage.SettingsFactory
import com.agc.bwitch.domain.settings.BillingProduct
import com.agc.bwitch.domain.settings.RestorePurchasesResult
import com.agc.bwitch.domain.settings.SubscriptionPlan
import com.agc.bwitch.domain.settings.SubscriptionPlanType
import com.agc.bwitch.domain.settings.SubscriptionRepository
import com.agc.bwitch.domain.settings.SubscriptionStatus
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class BillingBackedSubscriptionRepository private constructor(
    private val settings: Settings,
    private val billingDataSource: SubscriptionBillingDataSource,
) : SubscriptionRepository {

    constructor(
        settingsFactory: SettingsFactory,
        billingDataSource: SubscriptionBillingDataSource,
    ) : this(
        settings = settingsFactory.create(SETTINGS_NAME),
        billingDataSource = billingDataSource,
    )

    internal constructor(
        settings: Settings,
        billingDataSource: SubscriptionBillingDataSource,
        @Suppress("UNUSED_PARAMETER") forTests: Unit,
    ) : this(settings, billingDataSource)

    private val status = MutableStateFlow(readNonAuthoritativeStatus())

    override suspend fun getStatus(): SubscriptionStatus {
        val currentStatus = readNonAuthoritativeStatus()
        status.value = currentStatus
        return currentStatus
    }

    override fun observeStatus(): Flow<SubscriptionStatus> = status

    override suspend fun getCatalog(): List<SubscriptionPlan> {
        if (!billingDataSource.isSupported) return emptyList()
        return runCatching { billingDataSource.getProducts().map { product -> product.toSubscriptionPlan() } }
            .getOrDefault(emptyList())
    }

    override suspend fun restorePurchases(): RestorePurchasesResult {
        if (!billingDataSource.isSupported) return RestorePurchasesResult.NoPurchasesFound

        val tokens = runCatching { billingDataSource.queryRestorablePurchases() }
            .getOrDefault(emptyList())

        return if (tokens.isEmpty()) {
            RestorePurchasesResult.NoPurchasesFound
        } else {
            RestorePurchasesResult.RestorableTokens(tokens)
        }
    }

    /**
     * Legacy local subscription_status is retained only as informational migration state.
     * Active values from this cache are deliberately not surfaced as Premium authority.
     */
    private fun readNonAuthoritativeStatus(): SubscriptionStatus {
        val persistedStatus = settings.getStringOrNull(SUBSCRIPTION_STATUS_KEY)
            ?: return SubscriptionStatus.Unknown

        val parsed = SubscriptionStatus.entries.firstOrNull { it.name == persistedStatus }
            ?: return SubscriptionStatus.Unknown

        return when (parsed) {
            SubscriptionStatus.ActiveMonthly,
            SubscriptionStatus.ActiveAnnual,
            -> SubscriptionStatus.Unknown
            SubscriptionStatus.Unknown,
            SubscriptionStatus.Inactive,
            -> parsed
        }
    }

    private companion object {
        private const val SETTINGS_NAME = "bwitch_subscription"
        private const val SUBSCRIPTION_STATUS_KEY = "subscription_status"
    }
}

private fun BillingProduct.toSubscriptionPlan(): SubscriptionPlan = SubscriptionPlan(
    productId = productId,
    title = title.takeUnless(String::isBlank) ?: productId,
    formattedPrice = formattedPrice,
    type = when {
        billingPeriod?.contains("P1M", ignoreCase = true) == true -> SubscriptionPlanType.Monthly
        billingPeriod?.contains("P1Y", ignoreCase = true) == true -> SubscriptionPlanType.Annual
        else -> SubscriptionPlanType.Unknown
    },
)
