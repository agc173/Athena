package com.agc.bwitch.data.settings

import com.agc.bwitch.data.storage.SettingsFactory
import com.agc.bwitch.domain.settings.RestorePurchasesResult
import com.agc.bwitch.domain.settings.SubscriptionRepository
import com.agc.bwitch.domain.settings.SubscriptionStatus
import com.agc.bwitch.domain.settings.isActive
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class SettingsSubscriptionRepository(
    settingsFactory: SettingsFactory,
) : SubscriptionRepository {

    private val settings: Settings = settingsFactory.create(SETTINGS_NAME)
    private val status = MutableStateFlow(readCurrentStatus())

    override suspend fun getStatus(): SubscriptionStatus = status.value

    override fun observeStatus(): Flow<SubscriptionStatus> = status

    override suspend fun restorePurchases(): RestorePurchasesResult {
        val restorableStatus = readRestorableStatus()
        val activeRestorableStatus = restorableStatus.takeIf { it.isActive }
            ?: return RestorePurchasesResult.NoPurchasesFound

        persistCurrentStatus(activeRestorableStatus)
        status.value = activeRestorableStatus
        return RestorePurchasesResult.Restored(activeRestorableStatus)
    }

    private fun readCurrentStatus(): SubscriptionStatus = readStatus(
        key = SUBSCRIPTION_STATUS_KEY,
        defaultValue = SubscriptionStatus.Inactive,
    )

    private fun readRestorableStatus(): SubscriptionStatus = readStatus(
        key = RESTORABLE_SUBSCRIPTION_STATUS_KEY,
        defaultValue = DEFAULT_RESTORABLE_STATUS,
    )

    private fun readStatus(
        key: String,
        defaultValue: SubscriptionStatus,
    ): SubscriptionStatus {
        val persistedStatus = settings.getStringOrNull(key) ?: return defaultValue
        return SubscriptionStatus.entries.firstOrNull { it.name == persistedStatus }
            ?: SubscriptionStatus.Unknown
    }

    private fun persistCurrentStatus(status: SubscriptionStatus) {
        settings.putString(SUBSCRIPTION_STATUS_KEY, status.name)
    }

    private companion object {
        private const val SETTINGS_NAME = "bwitch_subscription"
        private const val SUBSCRIPTION_STATUS_KEY = "subscription_status"
        private const val RESTORABLE_SUBSCRIPTION_STATUS_KEY = "restorable_subscription_status"
        private val DEFAULT_RESTORABLE_STATUS: SubscriptionStatus = SubscriptionStatus.ActiveMonthly
    }
}
