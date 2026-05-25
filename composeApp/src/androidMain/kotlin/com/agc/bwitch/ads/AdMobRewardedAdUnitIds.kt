package com.agc.bwitch.ads

import android.util.Log
import com.agc.bwitch.BuildConfig

object AdMobRewardedAdUnitIds {
    private const val TAG = "RewardedAds"
    private const val TEST_REWARDED = "ca-app-pub-3940256099942544/5224354917"

    fun rewarded(placement: String): String? {
        val isDebug = BuildConfig.DEBUG
        val adUnitId = if (isDebug) TEST_REWARDED else BuildConfig.ADMOB_REWARDED_AD_UNIT_ID
        val hasId = adUnitId.isNotBlank()
        Log.i(TAG, "resolve placement=$placement debug=$isDebug adUnitConfigured=$hasId")
        if (!hasId) {
            Log.w(TAG, "unavailable placement=$placement reason=blank_ad_unit_id")
            return null
        }
        return adUnitId
    }
}
