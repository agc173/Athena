package com.agc.bwitch.data.settings

import com.agc.bwitch.data.settings.billing.SubscriptionBillingDataSource
import com.agc.bwitch.domain.settings.BillingProduct
import com.agc.bwitch.domain.settings.BillingPurchaseToken
import com.agc.bwitch.domain.settings.KnownSubscriptionProducts
import com.agc.bwitch.domain.settings.PurchaseState
import com.agc.bwitch.domain.settings.RestorePurchasesResult
import com.agc.bwitch.domain.settings.SubscriptionPlanType
import com.agc.bwitch.domain.settings.SubscriptionStatus
import com.russhwolf.settings.MapSettings
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class BillingBackedSubscriptionRepositoryTest {

    @Test
    fun `billing local purchased no implica premium activo`() = runTest {
        val token = BillingPurchaseToken(
            productId = KnownSubscriptionProducts.MONTHLY,
            purchaseToken = "token-1",
            purchaseState = PurchaseState.Purchased,
            acknowledged = false,
            packageName = "com.bwitch.app",
        )
        val repository = repository(
            billing = FakeBillingDataSource(
                isSupported = true,
                restorableTokens = listOf(token),
            ),
        )

        val status = repository.getStatus()
        val restore = repository.restorePurchases()

        assertEquals(SubscriptionStatus.Unknown, status)
        assertIs<RestorePurchasesResult.RestorableTokens>(restore)
        assertEquals(listOf(token), restore.tokens)
    }

    @Test
    fun `cache activa local no se usa como autoridad premium`() = runTest {
        val settings = MapSettings().apply {
            putString("subscription_status", SubscriptionStatus.ActiveMonthly.name)
        }
        val repository = repository(
            settings = settings,
            billing = FakeBillingDataSource(isSupported = false),
        )

        val status = repository.getStatus()
        val restore = repository.restorePurchases()

        assertEquals(SubscriptionStatus.Unknown, status)
        assertEquals(RestorePurchasesResult.NoPurchasesFound, restore)
    }

    @Test
    fun `restore devuelve tokens no entitlement`() = runTest {
        val tokens = listOf(
            BillingPurchaseToken(
                productId = KnownSubscriptionProducts.MONTHLY,
                purchaseToken = "token-restore",
                purchaseState = PurchaseState.Purchased,
                acknowledged = true,
                packageName = "com.bwitch.app",
            ),
        )
        val repository = repository(
            billing = FakeBillingDataSource(isSupported = true, restorableTokens = tokens),
        )

        val result = repository.restorePurchases()

        assertIs<RestorePurchasesResult.RestorableTokens>(result)
        assertEquals(tokens, result.tokens)
    }

    @Test
    fun `unsupported no devuelve premium activo`() = runTest {
        val repository = repository(
            billing = FakeBillingDataSource(isSupported = false),
        )

        assertEquals(SubscriptionStatus.Unknown, repository.getStatus())
        assertEquals(emptyList(), repository.getCatalog())
        assertEquals(RestorePurchasesResult.NoPurchasesFound, repository.restorePurchases())
    }

    @Test
    fun `catalogo solo muestra monthly en v1`() = runTest {
        assertEquals(listOf(KnownSubscriptionProducts.MONTHLY), KnownSubscriptionProducts.ordered)
        assertEquals(setOf(KnownSubscriptionProducts.MONTHLY), KnownSubscriptionProducts.all)
    }

    @Test
    fun `getCatalog mapea BillingProduct a SubscriptionPlan`() = runTest {
        val product = BillingProduct(
            productId = KnownSubscriptionProducts.MONTHLY,
            basePlanId = "monthly-base",
            offerToken = "offer-token",
            title = "Monthly",
            formattedPrice = "$4.99",
            priceAmountMicros = 4_990_000,
            priceCurrencyCode = "USD",
            billingPeriod = "P1M",
        )
        val repository = repository(
            billing = FakeBillingDataSource(isSupported = true, products = listOf(product)),
        )

        val catalog = repository.getCatalog()

        assertEquals(1, catalog.size)
        assertEquals(KnownSubscriptionProducts.MONTHLY, catalog.single().productId)
        assertEquals("Monthly", catalog.single().title)
        assertEquals("$4.99", catalog.single().formattedPrice)
        assertEquals(SubscriptionPlanType.Monthly, catalog.single().type)
    }

    private fun repository(
        settings: MapSettings = MapSettings(),
        billing: SubscriptionBillingDataSource,
    ): BillingBackedSubscriptionRepository = BillingBackedSubscriptionRepository(
        settings = settings,
        billingDataSource = billing,
        forTests = Unit,
    )
}

private class FakeBillingDataSource(
    override val isSupported: Boolean,
    private val products: List<BillingProduct> = emptyList(),
    private val restorableTokens: List<BillingPurchaseToken> = emptyList(),
) : SubscriptionBillingDataSource {
    override suspend fun getProducts(): List<BillingProduct> = products

    override suspend fun queryRestorablePurchases(): List<BillingPurchaseToken> = restorableTokens
}
