package com.agc.bwitch.data.settings.billing.googleplay

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.agc.bwitch.data.settings.billing.SubscriptionBillingDataSource
import com.agc.bwitch.domain.settings.BillingProduct
import com.agc.bwitch.domain.settings.BillingPurchaseResult
import com.agc.bwitch.domain.settings.BillingPurchaseToken
import com.agc.bwitch.domain.settings.PurchaseState
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class GooglePlaySubscriptionBillingDataSource(
    appContext: Context,
) : SubscriptionBillingDataSource {

    override val isSupported: Boolean = true

    private val packageName: String = appContext.packageName
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

            when (result.responseCode) {
                BillingClient.BillingResponseCode.OK -> {
                    val purchase = purchases
                        .orEmpty()
                        .firstOrNull { purchase ->
                            purchase.products.any { productId ->
                                productId in GooglePlayBillingSubscriptionProducts.knownProducts
                            }
                        }

                    val token = purchase?.toBillingPurchaseToken(packageName)
                    when (token?.purchaseState) {
                        PurchaseState.Purchased -> deferred.complete(BillingPurchaseResult.Purchased(token))
                        PurchaseState.Pending -> deferred.complete(BillingPurchaseResult.Pending(token))
                        PurchaseState.Unspecified,
                        null,
                        -> deferred.complete(
                            BillingPurchaseResult.Failed(
                                reason = result.debugMessage.takeUnless(String::isBlank)
                                    ?: "No purchase token returned",
                                code = result.responseCode,
                            ),
                        )
                    }
                }
                BillingClient.BillingResponseCode.USER_CANCELED -> deferred.complete(BillingPurchaseResult.Cancelled)
                else -> deferred.complete(
                    BillingPurchaseResult.Failed(
                        reason = result.debugMessage.takeUnless(String::isBlank) ?: "Purchase failed",
                        code = result.responseCode,
                    ),
                )
            }
        }
        .build()
    private var purchaseFlowResult: CompletableDeferred<BillingPurchaseResult>? = null

    override suspend fun getProducts(): List<BillingProduct> = connectionMutex.withLock {
        ensureReadyConnection()
        querySubscriptionProductDetails()
            .mapNotNull { details -> details.toBillingProduct() }
            .sortedBy { product ->
                GooglePlayBillingSubscriptionProducts.queryOrder.indexOf(product.productId).takeIf { it >= 0 }
                    ?: Int.MAX_VALUE
            }
    }

    override suspend fun queryRestorablePurchases(): List<BillingPurchaseToken> = connectionMutex.withLock {
        ensureReadyConnection()
        querySubscriptionPurchases()
            .mapNotNull { purchase -> purchase.toBillingPurchaseToken(packageName) }
    }

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
    ): BillingPurchaseResult {
        if (productId !in GooglePlayBillingSubscriptionProducts.knownProducts) {
            return BillingPurchaseResult.Unsupported
        }

        val deferred = try {
            connectionMutex.withLock {
                ensureReadyConnection()

                val productDetails = querySubscriptionProductDetails()
                    .firstOrNull { it.productId == productId }
                    ?: throw GooglePlayBillingException(
                        phase = "queryProductDetailsMissingProduct",
                        responseCode = BillingClient.BillingResponseCode.ITEM_UNAVAILABLE,
                        debugMessage = "Configured product not returned by Play Billing",
                    )
                val offerToken = productDetails.subscriptionOfferDetails
                    ?.firstOrNull()
                    ?.offerToken
                    ?: throw GooglePlayBillingException(
                        phase = "queryProductDetailsMissingOffer",
                        responseCode = BillingClient.BillingResponseCode.ITEM_UNAVAILABLE,
                        debugMessage = "No subscription offer token available for product",
                    )

                val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
                    .setProductDetails(productDetails)
                    .setOfferToken(offerToken)
                    .build()
                val flowParams = BillingFlowParams.newBuilder()
                    .setProductDetailsParamsList(listOf(productParams))
                    .build()

                val purchaseDeferred = CompletableDeferred<BillingPurchaseResult>()
                purchaseFlowResult = purchaseDeferred
                purchaseDeferred.invokeOnCompletion { purchaseFlowResult = null }
                val launchResult = billingClient.launchBillingFlow(activity, flowParams)
                if (launchResult.responseCode != BillingClient.BillingResponseCode.OK) {
                    purchaseFlowResult = null
                    return BillingPurchaseResult.Failed(
                        reason = launchResult.debugMessage.takeUnless(String::isBlank)
                            ?: "Billing flow launch failed",
                        code = launchResult.responseCode,
                    )
                }
                purchaseDeferred
            }
        } catch (error: GooglePlayBillingException) {
            return BillingPurchaseResult.Failed(reason = error.message.orEmpty(), code = error.responseCode)
        } catch (error: Throwable) {
            return BillingPurchaseResult.Failed(reason = error.message.orEmpty())
        }

        return deferred.await()
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

    private suspend fun querySubscriptionProductDetails(): List<ProductDetails> =
        suspendCancellableCoroutine { continuation ->
            val products = GooglePlayBillingSubscriptionProducts.queryOrder.map { productId ->
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

                continuation.resume(queryResult.productDetailsList)
            }
        }

    private fun disconnectedResult(): BillingResult = BillingResult.newBuilder()
        .setResponseCode(BillingClient.BillingResponseCode.SERVICE_DISCONNECTED)
        .setDebugMessage("Billing service disconnected")
        .build()
}

internal class GooglePlayBillingException(
    phase: String,
    val responseCode: Int,
    debugMessage: String,
) : IllegalStateException(
    "Play Billing $phase failed (code=$responseCode): $debugMessage",
)

private fun ProductDetails.toBillingProduct(): BillingProduct? {
    val offer = subscriptionOfferDetails
        ?.firstOrNull { details ->
            details.pricingPhases.pricingPhaseList.any { phase ->
                phase.formattedPrice.isNotBlank() && phase.billingPeriod.isNotBlank()
            }
        }
        ?: subscriptionOfferDetails?.firstOrNull { details ->
            details.pricingPhases.pricingPhaseList.any { phase -> phase.formattedPrice.isNotBlank() }
        }
        ?: return null

    val pricingPhase = offer.pricingPhases.pricingPhaseList
        .firstOrNull { phase ->
            phase.formattedPrice.isNotBlank() && phase.billingPeriod.isNotBlank()
        }
        ?: offer.pricingPhases.pricingPhaseList.firstOrNull { phase -> phase.formattedPrice.isNotBlank() }
        ?: return null

    val resolvedTitle = offer.basePlanId
        ?.takeUnless(String::isBlank)
        ?.replace('_', ' ')
        ?.replaceFirstChar { it.uppercase() }
        ?: title.takeUnless(String::isBlank)
        ?: productId

    return BillingProduct(
        productId = productId,
        basePlanId = offer.basePlanId?.takeUnless(String::isBlank),
        offerToken = offer.offerToken.takeUnless(String::isBlank),
        title = resolvedTitle,
        formattedPrice = pricingPhase.formattedPrice,
        priceAmountMicros = pricingPhase.priceAmountMicros,
        priceCurrencyCode = pricingPhase.priceCurrencyCode.takeUnless(String::isBlank),
        billingPeriod = pricingPhase.billingPeriod.takeUnless(String::isBlank),
    )
}

private fun Purchase.toBillingPurchaseToken(packageName: String): BillingPurchaseToken? {
    val productId = products.firstOrNull { it in GooglePlayBillingSubscriptionProducts.knownProducts }
        ?: return null
    val token = purchaseToken.takeUnless(String::isBlank) ?: return null

    return BillingPurchaseToken(
        productId = productId,
        purchaseToken = token,
        purchaseState = purchaseState.toDomainPurchaseState(),
        acknowledged = isAcknowledged,
        packageName = packageName,
    )
}

private fun Int.toDomainPurchaseState(): PurchaseState = when (this) {
    Purchase.PurchaseState.PURCHASED -> PurchaseState.Purchased
    Purchase.PurchaseState.PENDING -> PurchaseState.Pending
    else -> PurchaseState.Unspecified
}
