package com.agc.bwitch.data.moons.billing.googleplay

import android.app.Activity
import android.content.Context
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
import android.util.Log
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.coroutines.resume

class GooglePlayMoonPackBillingDataSource(private val appContext: Context) : MoonPackBillingDataSource {
    private val tag = "MoonPackBilling"
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
            Log.i(tag, "purchase callback received code=${result.responseCode} purchases=${purchases?.size ?: 0}")
            when {
                result.responseCode == BillingClient.BillingResponseCode.OK && relevant != null -> {
                    Log.i(tag, "purchase callback purchased product=${relevant.products}")
                    deferred.complete(Result.success(relevant.toDomain(appContext.packageName)!!))
                }
                result.responseCode == BillingClient.BillingResponseCode.USER_CANCELED -> {
                    Log.i(tag, "purchase callback cancelled")
                    deferred.complete(Result.failure(CancellationException("cancelled")))
                }
                else -> {
                    Log.w(tag, "purchase callback failed code=${result.responseCode} message=${result.debugMessage}")
                    deferred.complete(Result.failure(IllegalStateException("purchase failed ${result.responseCode}")))
                }
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
            val launch = billingClient.launchBillingFlow(activity, flow)
            if (launch.responseCode != BillingClient.BillingResponseCode.OK) throw IllegalStateException("launch failed")
            purchaseDeferred
        }
        deferred.await()
    }.getOrElse { Result.failure(it) }



    override suspend fun queryUnconsumedMoonPackPurchases(): Result<List<GooglePlayPurchase>> = runCatching {
        mutex.withLock {
            ensureReady()
            suspendCancellableCoroutine { cont ->
                val params = QueryPurchasesParams.newBuilder()
                    .setProductType(BillingClient.ProductType.INAPP)
                    .build()
                billingClient.queryPurchasesAsync(params) { result, purchases ->
                    if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                        Log.w(tag, "query purchases failed code=${result.responseCode} message=${result.debugMessage}")
                        cont.resumeWith(Result.failure(IllegalStateException("query purchases failed ${result.responseCode}")))
                    } else {
                        val pending = purchases.orEmpty().mapNotNull { it.toDomain(appContext.packageName) }
                            .filter { it.purchaseState == GooglePlayPurchaseState.Purchased }
                        Log.i(tag, "query purchases success pending=${pending.size}")
                        cont.resume(pending)
                    }
                }
            }
        }
    }

    override suspend fun consumePurchase(purchaseToken: String): Result<Unit> = runCatching {
        mutex.withLock {
            ensureReady()
            suspendCancellableCoroutine { cont ->
                Log.i(tag, "consume started tokenHash=${purchaseToken.hashCode()}")
                billingClient.consumeAsync(ConsumeParams.newBuilder().setPurchaseToken(purchaseToken).build()) { r, _ ->
                    if (r.responseCode == BillingClient.BillingResponseCode.OK) {
                        Log.i(tag, "consume success")
                        cont.resume(Unit)
                    } else {
                        Log.w(tag, "consume failed code=${r.responseCode} message=${r.debugMessage}")
                        cont.resumeWith(Result.failure(IllegalStateException("consume failed ${r.responseCode}")))
                    }
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
                if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                    cont.resumeWith(Result.failure(IllegalStateException("query details failed ${result.responseCode}")))
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
