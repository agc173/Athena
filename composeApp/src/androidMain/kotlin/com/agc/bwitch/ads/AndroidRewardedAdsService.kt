package com.agc.bwitch.ads

import android.app.Activity
import android.util.Log
import com.agc.bwitch.BuildConfig
import com.agc.bwitch.presentation.ads.RewardedAdResult
import com.agc.bwitch.presentation.ads.RewardedAdsService
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class AndroidRewardedAdsService : RewardedAdsService {
    @Volatile
    private var currentActivity: Activity? = null

    private val cachedAdsByPlacement = mutableMapOf<String, CachedRewardedAd>()
    private val loadingPlacements = mutableSetOf<String>()
    private val requestedPreloads = mutableSetOf<String>()

    fun bindActivity(activity: Activity?) {
        currentActivity = activity
        if (activity != null) {
            val placements = synchronized(this) { requestedPreloads.toList() }
            placements.forEach { preloadRewardedAd(it) }
        }
    }

    override fun preloadRewardedAd(placement: String) {
        synchronized(this) { requestedPreloads += placement }
        val activity = currentActivity ?: run {
            Log.i(TAG, "Rewarded ad preload deferred: placement=$placement reason=no_activity")
            return
        }
        val adUnitId = AdMobRewardedAdUnitIds.rewarded(placement)
        if (adUnitId.isNullOrBlank()) {
            Log.w(TAG, "Rewarded ad preload skipped: placement=$placement reason=no_ad_unit")
            return
        }
        synchronized(this) {
            if (cachedAdsByPlacement[placement]?.matches(adUnitId) == true) {
                Log.i(TAG, "Rewarded ad preload skipped: placement=$placement reason=already_ready")
                return
            }
            if (!loadingPlacements.add(placement)) {
                Log.i(TAG, "Rewarded ad preload skipped: placement=$placement reason=already_loading")
                return
            }
        }

        Log.i(TAG, "Rewarded ad load start: placement=$placement adUnit=${adUnitId.redactedAdUnitId()}")
        RewardedAd.load(
            activity,
            adUnitId,
            AdRequest.Builder().build(),
            object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    synchronized(this@AndroidRewardedAdsService) {
                        loadingPlacements.remove(placement)
                    }
                    Log.w(TAG, "Rewarded ad load failure: placement=$placement code=${loadAdError.code}")
                }

                override fun onAdLoaded(rewardedAd: RewardedAd) {
                    synchronized(this@AndroidRewardedAdsService) {
                        loadingPlacements.remove(placement)
                        cachedAdsByPlacement[placement] = CachedRewardedAd(adUnitId, rewardedAd)
                    }
                    Log.i(TAG, "Rewarded ad load success: placement=$placement adUnit=${adUnitId.redactedAdUnitId()}")
                }
            },
        )
    }

    override suspend fun showRewardedAd(placement: String): RewardedAdResult {
        val buildType = if (BuildConfig.DEBUG) "debug" else "release"
        val activity = currentActivity
        if (activity == null) {
            Log.w(TAG, "Rewarded ad unavailable: placement=$placement build=$buildType reason=no_activity")
            return RewardedAdResult.Unavailable
        }

        val adUnitId = AdMobRewardedAdUnitIds.rewarded(placement)
        if (adUnitId.isNullOrBlank()) {
            Log.w(TAG, "Rewarded ad unavailable: placement=$placement build=$buildType adUnitEmpty=true")
            return RewardedAdResult.Unavailable
        }

        val rewardedAd = synchronized(this) {
            cachedAdsByPlacement.remove(placement)?.takeIf { it.matches(adUnitId) }?.ad
        }
        if (rewardedAd == null) {
            Log.i(TAG, "Rewarded ad not ready on show: placement=$placement build=$buildType; starting preload")
            preloadRewardedAd(placement)
            return RewardedAdResult.Unavailable
        }

        Log.i(TAG, "Rewarded ad show start: placement=$placement build=$buildType adUnit=${adUnitId.redactedAdUnitId()}")
        return suspendCancellableCoroutine { continuation ->
            var resumed = false
            fun resumeOnce(result: RewardedAdResult) {
                if (!resumed && continuation.isActive) {
                    resumed = true
                    preloadRewardedAd(placement)
                    continuation.resume(result)
                }
            }

            var rewardEarned = false
            rewardedAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                    Log.i(TAG, "Rewarded ad shown: placement=$placement")
                }

                override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                    Log.w(TAG, "Rewarded ad show failure: placement=$placement code=${adError.code}")
                    resumeOnce(RewardedAdResult.Failed("show_failed:${adError.code}"))
                }

                override fun onAdDismissedFullScreenContent() {
                    Log.i(TAG, "Rewarded ad dismissed: placement=$placement rewardEarned=$rewardEarned")
                    resumeOnce(if (rewardEarned) RewardedAdResult.Completed else RewardedAdResult.Cancelled)
                }
            }
            rewardedAd.show(activity) { rewardItem ->
                rewardEarned = true
                Log.i(TAG, "Rewarded ad reward earned: placement=$placement type=${rewardItem.type} amount=${rewardItem.amount}")
            }
        }
    }

    private fun String.redactedAdUnitId(): String {
        val slashIndex = indexOf('/')
        if (slashIndex <= 0 || slashIndex == lastIndex) return "<redacted>"

        val appIdPrefix = take(slashIndex).takeLast(4)
        val unitIdSuffix = substring(slashIndex + 1).takeLast(4)
        return "...$appIdPrefix/...$unitIdSuffix"
    }

    private data class CachedRewardedAd(
        val adUnitId: String,
        val ad: RewardedAd,
    ) {
        fun matches(adUnitId: String): Boolean = this.adUnitId == adUnitId
    }

    private companion object {
        const val TAG = "AndroidRewardedAds"
    }
}
