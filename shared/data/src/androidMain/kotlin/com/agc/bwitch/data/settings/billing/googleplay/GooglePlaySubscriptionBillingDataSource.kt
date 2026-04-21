package com.agc.bwitch.data.settings.billing.googleplay

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.agc.bwitch.data.settings.billing.SubscriptionBillingDataSource
import com.agc.bwitch.domain.settings.SubscriptionPlan
import com.agc.bwitch.domain.settings.SubscriptionPlanType
import com.agc.bwitch.domain.settings.SubscriptionStatus
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
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
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder()
                .enableOneTimeProducts()
                .build(),
        )
        .setListener { result, purchases ->
            val deferred = purchaseFlowResult
            if (deferred == null || deferred.isCompleted) return@setListener

            val hasRelevantPurchasedSubscription = purchases
                .orEmpty()
                .any { purchase ->
                    purchase.purchaseState == Purchase.PurchaseState.PURCHASED &&
                        purchase.products.any { productId ->
                            productId in GooglePlayBillingSubscriptionProducts.knownProducts
                        }
                }

            when {
                result.responseCode == BillingClient.BillingResponseCode.OK && hasRelevantPurchasedSubscription ->
                    deferred.complete(Result.success(Unit))
                result.responseCode == BillingClient.BillingResponseCode.USER_CANCELED ->
                    deferred.complete(Result.failure(PurchaseFlowCancelledException))
                else -> deferred.complete(
                    Result.failure(
                        GooglePlayBillingException(
                            phase = "purchase",
                            responseCode = result.responseCode,
                            debugMessage = result.debugMessage,
                        ),
                    ),
                )
            }
        }
        .build()
    private var purchaseFlowResult: CompletableDeferred<Result<Unit>>? = null

    override suspend fun querySubscriptionStatus(): SubscriptionStatus =
        queryStatusWithConnection()

    override suspend fun querySubscriptionCatalog(): List<SubscriptionPlan> = connectionMutex.withLock {
        ensureReadyConnection()
        querySubscriptionProductDetails()
            .mapNotNull { details -> details.toSubscriptionPlan() }
            .sortedBy { plan ->
                when (plan.type) {
                    SubscriptionPlanType.Monthly -> 0
                    SubscriptionPlanType.Annual -> 1
                    SubscriptionPlanType.Unknown -> 2
                }
            }
    }

    override suspend fun restoreSubscriptionStatus(): SubscriptionStatus =
        queryStatusWithConnection()

    fun launchManageSubscriptions(
        activity: Activity,
        productId: String?,
    ): Result<Unit> = runCatching {
        val withSkuUri = productId?.let { sku ->
            Uri.parse("https://play.google.com/store/account/subscriptions?sku=$sku&package=${activity.packageName}")
        }
        val genericUri = Uri.parse("https://play.google.com/store/account/subscriptions")

        val primaryIntent = Intent(Intent.ACTION_VIEW, withSkuUri ?: genericUri)
        if (primaryIntent.resolveActivity(activity.packageManager) != null) {
            activity.startActivity(primaryIntent)
            return@runCatching
        }

        val fallbackIntent = Intent(Intent.ACTION_VIEW, genericUri)
        if (fallbackIntent.resolveActivity(activity.packageManager) != null) {
            activity.startActivity(fallbackIntent)
            return@runCatching
        }

        throw IllegalStateException("No activity available to manage subscriptions")
    }

    suspend fun launchPurchaseFlow(
        activity: Activity,
        productId: String,
    ): Result<Unit> {
        val deferred = connectionMutex.withLock {
            ensureReadyConnection()

            val productDetails = querySubscriptionProductDetails()
                .firstOrNull { it.productId == productId }
                ?: return Result.failure(
                    GooglePlayBillingException(
                        phase = "queryProductDetailsMissing",
                        responseCode = BillingClient.BillingResponseCode.ITEM_UNAVAILABLE,
                        debugMessage = "Configured product not returned by Play Billing",
                    ),
                )
            val offerToken = productDetails.subscriptionOfferDetails
                ?.firstOrNull()
                ?.offerToken
                ?: return Result.failure(
                    GooglePlayBillingException(
                        phase = "queryProductDetailsMissingOffer",
                        responseCode = BillingClient.BillingResponseCode.ITEM_UNAVAILABLE,
                        debugMessage = "No subscription offer token available for product",
                    ),
                )

            val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
                .setProductDetails(productDetails)
                .setOfferToken(offerToken)
                .build()
            val flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(listOf(productParams))
                .build()

            val purchaseDeferred = CompletableDeferred<Result<Unit>>()
            purchaseFlowResult = purchaseDeferred
            purchaseDeferred.invokeOnCompletion { purchaseFlowResult = null }
            val launchResult = billingClient.launchBillingFlow(activity, flowParams)
            if (launchResult.responseCode != BillingClient.BillingResponseCode.OK) {
                purchaseFlowResult = null
                return Result.failure(
                    GooglePlayBillingException(
                        phase = "launchBillingFlow",
                        responseCode = launchResult.responseCode,
                        debugMessage = launchResult.debugMessage,
                    ),
                )
            }
            purchaseDeferred
        }

        return deferred.await()
    }

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

    private suspend fun querySubscriptionProductDetails():
            List<com.android.billingclient.api.ProductDetails> =
        suspendCancellableCoroutine { continuation ->
            val products = GooglePlayBillingSubscriptionProducts.knownProducts.map { productId ->
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(productId)
                    .setProductType(BillingClient.ProductType.SUBS)
                    .build()
            }

            val params = QueryProductDetailsParams.newBuilder()
                .setProductList(products)
                .build()

            billingClient.queryProductDetailsAsync(params) { billingResult, queryResult ->
                if (continuation.isCompleted) return@queryProductDetailsAsync

                if (billingResult.responseCode != BillingClient.BillingResponseCode.OK) {
                    continuation.resumeWith(
                        Result.failure(
                            GooglePlayBillingException(
                                phase = "queryProductDetails",
                                responseCode = billingResult.responseCode,
                                debugMessage = billingResult.debugMessage,
                            ),
                        ),
                    )
                    return@queryProductDetailsAsync
                }

                val productDetailsList = queryResult.productDetailsList

                if (productDetailsList.isEmpty()) {
                    continuation.resumeWith(
                        Result.failure(
                            IllegalStateException("No ProductDetails found for configured subscriptions"),
                        ),
                    )
                    return@queryProductDetailsAsync
                }

                continuation.resume(productDetailsList)
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

private object PurchaseFlowCancelledException : CancellationException("Purchase flow cancelled by user")

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

private fun com.android.billingclient.api.ProductDetails.toSubscriptionPlan(): SubscriptionPlan? {
    val offer = subscriptionOfferDetails
        ?.firstOrNull { details ->
            details.pricingPhases.pricingPhaseList.any { phase -> phase.billingPeriod.isNotBlank() }
        }
        ?: subscriptionOfferDetails?.firstOrNull()
        ?: return null

    val pricingPhase = offer.pricingPhases.pricingPhaseList
        .firstOrNull { phase -> phase.formattedPrice.isNotBlank() }
        ?: return null

    val resolvedTitle = offer.basePlanId
        ?.takeUnless(String::isBlank)
        ?.replace('_', ' ')
        ?.replaceFirstChar { it.uppercase() }
        ?: title.takeUnless(String::isBlank)
        ?: productId

    return SubscriptionPlan(
        productId = productId,
        title = resolvedTitle,
        formattedPrice = pricingPhase.formattedPrice,
        type = pricingPhase.billingPeriod.toSubscriptionPlanType(),
    )
}

private fun String.toSubscriptionPlanType(): SubscriptionPlanType = when {
    contains("P1Y", ignoreCase = true) -> SubscriptionPlanType.Annual
    contains("P1M", ignoreCase = true) -> SubscriptionPlanType.Monthly
    else -> SubscriptionPlanType.Unknown
}
