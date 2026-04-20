package com.agc.bwitch.data.settings

import com.agc.bwitch.data.settings.billing.SubscriptionBillingDataSource
import com.agc.bwitch.data.storage.SettingsFactory
import com.agc.bwitch.domain.settings.RestorePurchasesResult
import com.agc.bwitch.domain.settings.SubscriptionRepository
import com.agc.bwitch.domain.settings.SubscriptionStatus
import com.agc.bwitch.domain.settings.isActive
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

    private val status = MutableStateFlow(readCurrentStatus())

    override suspend fun getStatus(): SubscriptionStatus {
        val resolvedStatus = resolveStatusFromStoreOrFallback()
        status.value = resolvedStatus
        persistCurrentStatus(resolvedStatus)
        return resolvedStatus
    }

    override fun observeStatus(): Flow<SubscriptionStatus> = status

    override suspend fun restorePurchases(): RestorePurchasesResult {
        if (!billingDataSource.isSupported) {
            return if (status.value.isActive) {
                RestorePurchasesResult.Restored(status.value)
            } else {
                RestorePurchasesResult.NoPurchasesFound
            }
        }

        val resolvedStatus = runCatching { billingDataSource.restoreSubscriptionStatus() }
            .getOrElse { readCurrentStatus() }

        status.value = resolvedStatus
        persistCurrentStatus(resolvedStatus)

        return if (resolvedStatus.isActive) {
            RestorePurchasesResult.Restored(resolvedStatus)
        } else {
            RestorePurchasesResult.NoPurchasesFound
        }
    }

    private suspend fun resolveStatusFromStoreOrFallback(): SubscriptionStatus {
        if (!billingDataSource.isSupported) return status.value

        return runCatching { billingDataSource.querySubscriptionStatus() }
            .getOrElse { readCurrentStatus() }
    }

    private fun readCurrentStatus(): SubscriptionStatus {
        val persistedStatus = settings.getStringOrNull(SUBSCRIPTION_STATUS_KEY)
            ?: return SubscriptionStatus.Unknown

        return SubscriptionStatus.entries.firstOrNull { it.name == persistedStatus }
            ?: SubscriptionStatus.Unknown
    }

    private fun persistCurrentStatus(status: SubscriptionStatus) {
        settings.putString(SUBSCRIPTION_STATUS_KEY, status.name)
    }

    private companion object {
        private const val SETTINGS_NAME = "bwitch_subscription"
        private const val SUBSCRIPTION_STATUS_KEY = "subscription_status"
    }
}
