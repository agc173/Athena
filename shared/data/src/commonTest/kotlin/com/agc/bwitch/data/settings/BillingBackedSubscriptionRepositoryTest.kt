package com.agc.bwitch.data.settings

import com.agc.bwitch.data.settings.billing.SubscriptionBillingDataSource
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
    fun `getStatus usa billing cuando esta soportado`() = runTest {
        val repository = repository(
            billing = FakeBillingDataSource(
                isSupported = true,
                statusFromQuery = SubscriptionStatus.ActiveAnnual,
            ),
        )

        val status = repository.getStatus()

        assertEquals(SubscriptionStatus.ActiveAnnual, status)
    }

    @Test
    fun `getStatus usa fallback persistido cuando billing falla`() = runTest {
        val settings = MapSettings().apply {
            putString("subscription_status", SubscriptionStatus.ActiveMonthly.name)
        }
        val repository = repository(
            settings = settings,
            billing = FakeBillingDataSource(
                isSupported = true,
                queryError = IllegalStateException("setup failed"),
            ),
        )

        val status = repository.getStatus()

        assertEquals(SubscriptionStatus.ActiveMonthly, status)
    }

    @Test
    fun `restore en plataforma no soportada devuelve no purchases si no hay activo`() = runTest {
        val repository = repository(
            billing = FakeBillingDataSource(isSupported = false),
        )

        val result = repository.restorePurchases()

        assertEquals(RestorePurchasesResult.NoPurchasesFound, result)
    }

    @Test
    fun `restore en plataforma no soportada conserva estado activo persistido`() = runTest {
        val settings = MapSettings().apply {
            putString("subscription_status", SubscriptionStatus.ActiveAnnual.name)
        }
        val repository = repository(
            settings = settings,
            billing = FakeBillingDataSource(isSupported = false),
        )

        val result = repository.restorePurchases()

        assertEquals(
            RestorePurchasesResult.Restored(SubscriptionStatus.ActiveAnnual),
            result,
        )
    }

    @Test
    fun `getCatalog usa billing cuando esta soportado`() = runTest {
        val plan = SubscriptionPlan(
            productId = "monthly",
            title = "Monthly",
            formattedPrice = "$4.99",
            type = SubscriptionPlanType.Monthly,
        )
        val repository = repository(
            billing = FakeBillingDataSource(isSupported = true, catalogFromQuery = listOf(plan)),
        )

        val catalog = repository.getCatalog()

        assertEquals(listOf(plan), catalog)
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
    private val statusFromQuery: SubscriptionStatus = SubscriptionStatus.Inactive,
    private val statusFromRestore: SubscriptionStatus = statusFromQuery,
    private val queryError: Throwable? = null,
    private val restoreError: Throwable? = null,
    private val catalogFromQuery: List<SubscriptionPlan> = emptyList(),
) : SubscriptionBillingDataSource {
    override suspend fun querySubscriptionStatus(): SubscriptionStatus {
        queryError?.let { throw it }
        return statusFromQuery
    }

    override suspend fun querySubscriptionCatalog(): List<SubscriptionPlan> = catalogFromQuery

    override suspend fun restoreSubscriptionStatus(): SubscriptionStatus {
        restoreError?.let { throw it }
        return statusFromRestore
    }
}
