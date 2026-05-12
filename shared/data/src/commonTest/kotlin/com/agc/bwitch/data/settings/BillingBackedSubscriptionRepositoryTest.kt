package com.agc.bwitch.data.settings

import com.agc.bwitch.data.settings.billing.SubscriptionBillingDataSource
import com.agc.bwitch.domain.settings.GooglePlayPurchase
import com.agc.bwitch.domain.settings.GooglePlayPurchaseState
import com.agc.bwitch.domain.settings.PremiumEntitlement
import com.agc.bwitch.domain.settings.PremiumEntitlementRepository
import com.agc.bwitch.domain.settings.RestorePurchasesResult
import com.agc.bwitch.domain.settings.SubscriptionPlan
import com.agc.bwitch.domain.settings.SubscriptionPlanType
import com.agc.bwitch.domain.settings.SubscriptionStatus
import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class BillingBackedSubscriptionRepositoryTest {

    @Test
    fun `getStatus usa backend entitlement y no billing local`() = runTest {
        val repository = repository(
            billing = FakeBillingDataSource(
                isSupported = true,
                statusFromQuery = SubscriptionStatus.ActiveMonthly,
            ),
            entitlements = FakePremiumEntitlementRepository(
                refreshEntitlement = PremiumEntitlement(isActive = false, status = SubscriptionStatus.Inactive),
            ),
        )

        val status = repository.getStatus()

        assertEquals(SubscriptionStatus.Inactive, status)
    }

    @Test
    fun `getStatus no conserva activo persistido cuando backend falla`() = runTest {
        val settings = MapSettings().apply {
            putString("subscription_status", SubscriptionStatus.ActiveMonthly.name)
        }
        val repository = repository(
            settings = settings,
            billing = FakeBillingDataSource(isSupported = true),
            entitlements = FakePremiumEntitlementRepository(refreshError = IllegalStateException("backend failed")),
        )

        val status = repository.getStatus()

        assertEquals(SubscriptionStatus.Unknown, status)
    }

    @Test
    fun `restore envia compras Google Play al backend y restaura solo si entitlement activo`() = runTest {
        val purchase = googlePlayPurchase()
        val entitlements = FakePremiumEntitlementRepository(
            restoreEntitlement = PremiumEntitlement(isActive = true, status = SubscriptionStatus.ActiveMonthly),
        )
        val repository = repository(
            billing = FakeBillingDataSource(isSupported = true, purchases = listOf(purchase)),
            entitlements = entitlements,
        )

        val result = repository.restorePurchases()

        assertEquals(listOf(purchase), entitlements.lastRestorePurchases)
        assertEquals(RestorePurchasesResult.Restored(SubscriptionStatus.ActiveMonthly), result)
    }

    @Test
    fun `restore sin backend active no activa premium aunque billing tenga compra`() = runTest {
        val repository = repository(
            billing = FakeBillingDataSource(isSupported = true, purchases = listOf(googlePlayPurchase())),
            entitlements = FakePremiumEntitlementRepository(
                restoreEntitlement = PremiumEntitlement(isActive = false, status = SubscriptionStatus.Inactive),
            ),
        )

        val result = repository.restorePurchases()

        assertEquals(RestorePurchasesResult.NoPurchasesFound, result)
    }

    @Test
    fun `getCatalog usa billing cuando esta soportado`() = runTest {
        val plan = SubscriptionPlan(
            productId = "bwitch_premium_monthly",
            title = "Monthly",
            formattedPrice = "$4.99",
            type = SubscriptionPlanType.Monthly,
            basePlanId = "monthly",
        )
        val repository = repository(
            billing = FakeBillingDataSource(isSupported = true, catalogFromQuery = listOf(plan)),
            entitlements = FakePremiumEntitlementRepository(),
        )

        val catalog = repository.getCatalog()

        assertEquals(listOf(plan), catalog)
    }

    private fun repository(
        settings: MapSettings = MapSettings(),
        billing: SubscriptionBillingDataSource,
        entitlements: PremiumEntitlementRepository,
    ): BillingBackedSubscriptionRepository = BillingBackedSubscriptionRepository(
        settings = settings,
        billingDataSource = billing,
        premiumEntitlementRepository = entitlements,
        forTests = Unit,
    )
}

private class FakeBillingDataSource(
    override val isSupported: Boolean,
    private val statusFromQuery: SubscriptionStatus = SubscriptionStatus.Inactive,
    private val statusFromRestore: SubscriptionStatus = statusFromQuery,
    private val queryError: Throwable? = null,
    private val restoreError: Throwable? = null,
    private val catalogFromQuery: List<SubscriptionPlan> = emptyList(),
    private val purchases: List<GooglePlayPurchase> = emptyList(),
) : SubscriptionBillingDataSource {
    override suspend fun querySubscriptionStatus(): SubscriptionStatus {
        queryError?.let { throw it }
        return statusFromQuery
    }

    override suspend fun querySubscriptionCatalog(): List<SubscriptionPlan> = catalogFromQuery

    override suspend fun queryGooglePlayPurchases(): List<GooglePlayPurchase> = purchases

    override suspend fun restoreSubscriptionStatus(): SubscriptionStatus {
        restoreError?.let { throw it }
        return statusFromRestore
    }
}

private class FakePremiumEntitlementRepository(
    private val validateEntitlement: PremiumEntitlement = PremiumEntitlement(false, SubscriptionStatus.Inactive),
    private val restoreEntitlement: PremiumEntitlement = PremiumEntitlement(false, SubscriptionStatus.Inactive),
    private val refreshEntitlement: PremiumEntitlement = PremiumEntitlement(false, SubscriptionStatus.Inactive),
    private val refreshError: Throwable? = null,
) : PremiumEntitlementRepository {
    var lastRestorePurchases: List<GooglePlayPurchase> = emptyList()

    override suspend fun validateGooglePlayPurchase(purchase: GooglePlayPurchase): PremiumEntitlement = validateEntitlement

    override suspend fun restoreGooglePlayPurchases(purchases: List<GooglePlayPurchase>): PremiumEntitlement {
        lastRestorePurchases = purchases
        return restoreEntitlement
    }

    override suspend fun refreshEntitlement(): PremiumEntitlement {
        refreshError?.let { throw it }
        return refreshEntitlement
    }
}

private fun googlePlayPurchase(): GooglePlayPurchase = GooglePlayPurchase(
    productId = "bwitch_premium_monthly",
    purchaseToken = "token-123",
    purchaseState = GooglePlayPurchaseState.Purchased,
    isAcknowledged = false,
    orderId = "order-123",
    packageName = "com.agc.bwitch",
)
