package com.agc.bwitch.data.moons.billing.googleplay

import android.app.Activity
import android.content.Context
import android.util.Log
import com.agc.bwitch.data.moons.MoonPackBillingDataSource
import com.agc.bwitch.data.moons.MoonPackProduct
import com.agc.bwitch.domain.settings.GooglePlayPurchase
import com.agc.bwitch.domain.settings.GooglePlayPurchaseState
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.QueryProductDetailsParams
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume

class GooglePlayMoonPackBillingDataSource(private val appContext: Context) : MoonPackBillingDataSource {
    private companion object {
        const val TAG = "AthenaBillingMoons"
    }
    override val isSupported: Boolean = true
    private val mutex = Mutex()
    private var pendingPurchase: CompletableDeferred<Result<GooglePlayPurchase>>? = null
    private var currentActivity: Activity? = null

    fun bindActivity(activity: Activity?) { currentActivity = activity }

    private val billingClient: BillingClient = BillingClient.newBuilder(appContext)
        .enablePendingPurchases(PendingPurchasesParams.newBuilder().enableOneTimeProducts().build())
        .setListener { result, purchases ->
            val deferred = pendingPurchase ?: return@setListener
            if (deferred.isCompleted) return@setListener
            val relevant = purchases.orEmpty().firstOrNull { p ->
                p.products.any { it in GooglePlayMoonPackProducts.knownProducts }
            }
            when {
                result.responseCode == BillingClient.BillingResponseCode.OK && relevant != null -> deferred.complete(Result.success(relevant.toDomain(appContext.packageName)!!))
                result.responseCode == BillingClient.BillingResponseCode.USER_CANCELED -> deferred.complete(Result.failure(CancellationException("cancelled")))
                else -> deferred.complete(Result.failure(IllegalStateException("purchase failed ${result.responseCode}")))
            }
        }.build()

    override suspend fun queryMoonPackProductDetails(): List<MoonPackProduct> = mutex.withLock {
        ensureReady()
        queryMoonPackProductDetailsWithConnection()
            .mapNotNull { detail ->
                detail.oneTimePurchaseOfferDetails?.formattedPrice?.let { formattedPrice ->
                    MoonPackProduct(productId = detail.productId, localizedPrice = formattedPrice)
                }
            }
    }

    override suspend fun launchMoonPackPurchase(productId: String): Result<GooglePlayPurchase> = runCatching {
        val activity = currentActivity ?: throw IllegalStateException("Activity not bound")
        val deferred = mutex.withLock {
            ensureReady()
            val pdetail = queryMoonPackProductDetailsWithConnection()
                .firstOrNull { detail -> detail.productId == productId }
                ?: throw IllegalStateException("Product unavailable")
            val params = BillingFlowParams.ProductDetailsParams.newBuilder().setProductDetails(pdetail).build()
            val flow = BillingFlowParams.newBuilder().setProductDetailsParamsList(listOf(params)).build()
            val purchaseDeferred = CompletableDeferred<Result<GooglePlayPurchase>>()
            pendingPurchase = purchaseDeferred
            purchaseDeferred.invokeOnCompletion { pendingPurchase = null }
            Log.d(
                TAG,
                "launchBillingFlow package=${activity.packageName} productId=$productId type=${BillingClient.ProductType.INAPP}",
            )
            val launch = billingClient.launchBillingFlow(activity, flow)
            if (launch.responseCode != BillingClient.BillingResponseCode.OK) {
                pendingPurchase = null
                Log.w(TAG, "launchBillingFlow failed code=${launch.responseCode} message=${launch.debugMessage}")
                throw IllegalStateException("launch failed ${launch.responseCode}: ${launch.debugMessage}")
            }
            purchaseDeferred
        }
        deferred.await()
    }.getOrElse { Result.failure(it) }

    override suspend fun consumePurchase(purchaseToken: String): Result<Unit> = runCatching {
        mutex.withLock {
            ensureReady()
            suspendCancellableCoroutine { cont ->
                billingClient.consumeAsync(ConsumeParams.newBuilder().setPurchaseToken(purchaseToken).build()) { r, _ ->
                    if (r.responseCode == BillingClient.BillingResponseCode.OK) cont.resume(Unit)
                    else cont.resumeWith(Result.failure(IllegalStateException("consume failed ${r.responseCode}")))
                }
            }
        }
    }

    private suspend fun ensureReady() {
        if (billingClient.isReady) return
        val r = suspendCancellableCoroutine<BillingResult> { cont ->
            billingClient.startConnection(object : BillingClientStateListener {
                override fun onBillingSetupFinished(billingResult: BillingResult) { cont.resume(billingResult) }
                override fun onBillingServiceDisconnected() { cont.resume(disconnected()) }
            })
        }
        if (r.responseCode != BillingClient.BillingResponseCode.OK) error("billing setup failed")
    }

    private suspend fun queryMoonPackProductDetailsWithConnection(): List<com.android.billingclient.api.ProductDetails> =
        suspendCancellableCoroutine { cont ->
            val products = GooglePlayMoonPackProducts.queryOrder.map { productId ->
                QueryProductDetailsParams.Product.newBuilder()
                    .setProductId(productId)
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()
            }
            val queryParams = QueryProductDetailsParams.newBuilder()
                .setProductList(products)
                .build()
            billingClient.queryProductDetailsAsync(queryParams) { result, q ->
                Log.d(
                    TAG,
                    "queryProductDetails package=${appContext.packageName} type=${BillingClient.ProductType.INAPP} requested=${GooglePlayMoonPackProducts.queryOrder} returned=${q.productDetailsList.map { it.productId }} unfetched=${q.unfetchedProductList.map { it.productId }}",
                )
                if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                    cont.resumeWith(Result.failure(IllegalStateException("query details failed ${result.responseCode}: ${result.debugMessage}")))
                } else {
                    cont.resume(q.productDetailsList)
                }
            }
        }

    private fun disconnected(): BillingResult = BillingResult.newBuilder().setResponseCode(BillingClient.BillingResponseCode.SERVICE_DISCONNECTED).build()
}

private fun Purchase.toDomain(packageName: String): GooglePlayPurchase? {
    val productId = products.firstOrNull { it in GooglePlayMoonPackProducts.knownProducts } ?: return null
    return GooglePlayPurchase(
        productId = productId,
        purchaseToken = purchaseToken,
        purchaseState = when (purchaseState) {
            Purchase.PurchaseState.PURCHASED -> GooglePlayPurchaseState.Purchased
            Purchase.PurchaseState.PENDING -> GooglePlayPurchaseState.Pending
            else -> GooglePlayPurchaseState.Unknown
        },
        isAcknowledged = isAcknowledged,
        orderId = orderId,
        packageName = packageName,
    )
}
