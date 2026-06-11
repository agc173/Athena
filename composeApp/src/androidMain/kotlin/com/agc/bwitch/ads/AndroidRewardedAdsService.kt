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

    fun bindActivity(activity: Activity?) {
        currentActivity = activity
    }

    override suspend fun showRewardedAd(placement: String): RewardedAdResult {
        val buildType = if (BuildConfig.DEBUG) "debug" else "release"
        val activity = currentActivity
        if (activity == null) {
            Log.w(
                TAG,
                "Rewarded ad unavailable: placement=$placement build=$buildType reason=no_activity"
            )
            return RewardedAdResult.Unavailable
        }

        val adUnitId = AdMobRewardedAdUnitIds.rewarded(placement)
        if (adUnitId.isNullOrBlank()) {
            Log.w(
                TAG,
                "Rewarded ad unavailable: placement=$placement build=$buildType adUnitEmpty=true"
            )
            return RewardedAdResult.Unavailable
        }

        Log.i(
            TAG,
            "Rewarded ad load requested: placement=$placement build=$buildType " +
                "adUnitEmpty=false adUnit=${adUnitId.redactedAdUnitId()}"
        )

        return suspendCancellableCoroutine { continuation ->
            var resumed = false
            fun resumeOnce(result: RewardedAdResult) {
                if (!resumed && continuation.isActive) {
                    resumed = true
                    continuation.resume(result)
                }
            }

            RewardedAd.load(
                activity,
                adUnitId,
                AdRequest.Builder().build(),
                object : RewardedAdLoadCallback() {
                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        Log.w(
                            TAG,
                            "Rewarded ad load failed: placement=$placement build=$buildType " +
                                "adUnit=${adUnitId.redactedAdUnitId()} code=${loadAdError.code}"
                        )
                        resumeOnce(RewardedAdResult.Failed("load_failed:${loadAdError.code}"))
                    }

                    override fun onAdLoaded(rewardedAd: RewardedAd) {
                        Log.i(
                            TAG,
                            "Rewarded ad loaded: placement=$placement build=$buildType " +
                                "adUnit=${adUnitId.redactedAdUnitId()}"
                        )
                        var rewardEarned = false
                        rewardedAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                            override fun onAdShowedFullScreenContent() = Unit

                            override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
                                Log.w(
                                    TAG,
                                    "Rewarded ad show failed: placement=$placement build=$buildType " +
                                        "adUnit=${adUnitId.redactedAdUnitId()} code=${adError.code}"
                                )
                                resumeOnce(RewardedAdResult.Failed("show_failed:${adError.code}"))
                            }

                            override fun onAdDismissedFullScreenContent() {
                                if (rewardEarned) {
                                    resumeOnce(RewardedAdResult.Completed)
                                } else {
                                    resumeOnce(RewardedAdResult.Cancelled)
                                }
                            }
                        }
                        rewardedAd.show(activity) {
                            rewardEarned = true
                        }
                    }
                },
            )
        }
    }

    private fun String.redactedAdUnitId(): String {
        val slashIndex = indexOf('/')
        if (slashIndex <= 0 || slashIndex == lastIndex) return "<redacted>"

        val appIdPrefix = take(slashIndex).takeLast(4)
        val unitIdSuffix = substring(slashIndex + 1).takeLast(4)
        return "...$appIdPrefix/...$unitIdSuffix"
    }

    private companion object {
        const val TAG = "AndroidRewardedAds"
    }
}
