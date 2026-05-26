package com.agc.bwitch.data.settings

import com.agc.bwitch.data.settings.billing.SubscriptionBillingDataSource
import com.agc.bwitch.data.storage.SettingsFactory
import com.agc.bwitch.domain.settings.PremiumEntitlementRepository
import com.agc.bwitch.domain.settings.RestorePurchasesResult
import com.agc.bwitch.domain.settings.SubscriptionPlan
import com.agc.bwitch.domain.settings.SubscriptionRepository
import com.agc.bwitch.domain.settings.SubscriptionStatus
import com.agc.bwitch.domain.settings.isActive
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class BillingBackedSubscriptionRepository private constructor(
    private val settings: Settings,
    private val billingDataSource: SubscriptionBillingDataSource,
    private val premiumEntitlementRepository: PremiumEntitlementRepository,
) : SubscriptionRepository {

    constructor(
        settingsFactory: SettingsFactory,
        billingDataSource: SubscriptionBillingDataSource,
        premiumEntitlementRepository: PremiumEntitlementRepository,
    ) : this(
        settings = settingsFactory.create(SETTINGS_NAME),
        billingDataSource = billingDataSource,
        premiumEntitlementRepository = premiumEntitlementRepository,
    )

    internal constructor(
        settings: Settings,
        billingDataSource: SubscriptionBillingDataSource,
        premiumEntitlementRepository: PremiumEntitlementRepository,
        @Suppress("UNUSED_PARAMETER") forTests: Unit,
    ) : this(settings, billingDataSource, premiumEntitlementRepository)

    private val status = MutableStateFlow(SubscriptionStatus.Unknown)

    override suspend fun getStatus(): SubscriptionStatus {
        val resolvedStatus = resolveStatusFromStoreOrFallback()
        println("BWITCH_PREMIUM_DEBUG status_refresh backend=$resolvedStatus")
        status.value = resolvedStatus
        persistCurrentStatus(resolvedStatus)
        return resolvedStatus
    }

    override fun observeStatus(): Flow<SubscriptionStatus> = status

    override suspend fun getCatalog(): List<SubscriptionPlan> {
        if (!billingDataSource.isSupported) return emptyList()
        return runCatching { billingDataSource.querySubscriptionCatalog() }
            .getOrDefault(emptyList())
    }

    override suspend fun restorePurchases(): RestorePurchasesResult {
        val purchases = if (billingDataSource.isSupported) {
            runCatching { billingDataSource.queryGooglePlayPurchases() }.getOrElse { error ->
                println("BWITCH_PREMIUM_DEBUG restore billing_query_failed=${error.message}")
                emptyList()
            }
        } else {
            emptyList()
        }

        println("BWITCH_PREMIUM_DEBUG restore billing_purchases_count=${purchases.size}")

        val resolvedStatus = runCatching {
            premiumEntitlementRepository.restoreGooglePlayPurchases(purchases).status
        }.getOrElse { error ->
            println("BWITCH_PREMIUM_DEBUG restore backend_restore_failed=${error.message}")
            runCatching { premiumEntitlementRepository.refreshEntitlement().status }
                .getOrElse { refreshError ->
                    println("BWITCH_PREMIUM_DEBUG restore backend_refresh_failed=${refreshError.message}")
                    SubscriptionStatus.Unknown
                }
        }

        println("BWITCH_PREMIUM_DEBUG restore resolved_status=$resolvedStatus")

        status.value = resolvedStatus
        persistCurrentStatus(resolvedStatus)

        return if (resolvedStatus.isActive) {
            RestorePurchasesResult.Restored(resolvedStatus)
        } else {
            RestorePurchasesResult.NoPurchasesFound
        }
    }

    private suspend fun resolveStatusFromStoreOrFallback(): SubscriptionStatus {
        return runCatching { premiumEntitlementRepository.refreshEntitlement().status }
            .getOrElse { error ->
                println("BWITCH_PREMIUM_DEBUG refresh backend_failed=${error.message}")
                readCurrentStatus()
            }
    }

    private fun readCurrentStatus(): SubscriptionStatus {
        val persistedStatus = settings.getStringOrNull(SUBSCRIPTION_STATUS_KEY)
            ?: return SubscriptionStatus.Unknown

        val resolved = SubscriptionStatus.entries.firstOrNull { it.name == persistedStatus }
            ?: return SubscriptionStatus.Unknown

        return resolved
    }

    private fun persistCurrentStatus(status: SubscriptionStatus) {
        settings.putString(SUBSCRIPTION_STATUS_KEY, status.name)
    }

    private companion object {
        private const val SETTINGS_NAME = "bwitch_subscription"
        private const val SUBSCRIPTION_STATUS_KEY = "subscription_status"
    }
}
