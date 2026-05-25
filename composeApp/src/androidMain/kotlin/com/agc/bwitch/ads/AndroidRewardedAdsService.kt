package com.agc.bwitch.ads

import android.app.Activity
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
        val activity = currentActivity ?: return RewardedAdResult.Unavailable
        val adUnitId = AdMobRewardedAdUnitIds.rewarded(placement)
            ?.takeIf { it.isNotBlank() }
            ?: return RewardedAdResult.Unavailable

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
                        resumeOnce(RewardedAdResult.Failed("load_failed:${loadAdError.code}"))
                    }

                    override fun onAdLoaded(rewardedAd: RewardedAd) {
                        var rewardEarned = false
                        rewardedAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                            override fun onAdShowedFullScreenContent() = Unit

                            override fun onAdFailedToShowFullScreenContent(adError: com.google.android.gms.ads.AdError) {
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
}
