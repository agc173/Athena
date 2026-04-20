package com.agc.bwitch.data.settings.billing.googleplay

import android.content.Context
import com.agc.bwitch.data.settings.billing.SubscriptionBillingDataSource
import com.agc.bwitch.domain.settings.SubscriptionStatus
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class GooglePlaySubscriptionBillingDataSource(
    appContext: Context,
) : SubscriptionBillingDataSource {

    override val isSupported: Boolean = true

    private val connectionMutex = Mutex()

    private val billingClient: BillingClient = BillingClient.newBuilder(appContext)
        .enablePendingPurchases()
        .setListener { _, _ ->
            // No-op en esta iteración: solo hacemos consulta/restauración pasiva.
        }
        .build()

    override suspend fun querySubscriptionStatus(): SubscriptionStatus =
        queryStatusWithConnection()

    override suspend fun restoreSubscriptionStatus(): SubscriptionStatus =
        queryStatusWithConnection()

    private suspend fun queryStatusWithConnection(): SubscriptionStatus = connectionMutex.withLock {
        ensureReadyConnection()
        val purchases = querySubscriptionPurchases()
        purchases.toSubscriptionStatus()
    }

    private suspend fun ensureReadyConnection() {
        if (billingClient.isReady) return

        val result = suspendCancellableCoroutine { continuation ->
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(result: BillingResult) {
                    if (continuation.isCompleted) return
                    continuation.resume(result)
                }

                override fun onBillingServiceDisconnected() {
                    if (continuation.isCompleted) return
                    continuation.resume(disconnectedResult())
                }
            })
        }

        if (result.responseCode != BillingClient.BillingResponseCode.OK) {
            throw GooglePlayBillingException(
                phase = "setup",
                responseCode = result.responseCode,
                debugMessage = result.debugMessage,
            )
        }
    }

    private suspend fun querySubscriptionPurchases(): List<Purchase> =
        suspendCancellableCoroutine { continuation ->
            val params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.SUBS)
                .build()

            billingClient.queryPurchasesAsync(params) { result, purchases ->
                if (continuation.isCompleted) return@queryPurchasesAsync
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    continuation.resume(purchases)
                } else {
                    continuation.resumeWith(
                        Result.failure(
                            GooglePlayBillingException(
                                phase = "queryPurchases",
                                responseCode = result.responseCode,
                                debugMessage = result.debugMessage,
                            ),
                        ),
                    )
                }
            }
        }

    private fun disconnectedResult(): BillingResult = BillingResult.newBuilder()
        .setResponseCode(BillingClient.BillingResponseCode.SERVICE_DISCONNECTED)
        .setDebugMessage("Billing service disconnected")
        .build()
}

internal class GooglePlayBillingException(
    phase: String,
    responseCode: Int,
    debugMessage: String,
) : IllegalStateException(
    "Play Billing $phase failed (code=$responseCode): $debugMessage",
)

private fun List<Purchase>.toSubscriptionStatus(): SubscriptionStatus {
    val activePurchases = this.filter { purchase ->
        // Foundation real: PURCHASED ya representa entitlement activo.
        // No exigimos `isAcknowledged` para evitar falsos negativos temporales
        // mientras aún no implementamos flujo completo de compra/ack backend.
        purchase.purchaseState == Purchase.PurchaseState.PURCHASED
    }

    if (activePurchases.isEmpty()) return SubscriptionStatus.Inactive

    val activeProductIds = activePurchases.flatMap { it.products }.toSet()

    return when {
        GooglePlayBillingSubscriptionProducts.ANNUAL in activeProductIds -> SubscriptionStatus.ActiveAnnual
        GooglePlayBillingSubscriptionProducts.MONTHLY in activeProductIds -> SubscriptionStatus.ActiveMonthly
        activeProductIds.intersect(GooglePlayBillingSubscriptionProducts.knownProducts).isNotEmpty() -> SubscriptionStatus.ActiveMonthly
        else -> SubscriptionStatus.Inactive
    }
}
