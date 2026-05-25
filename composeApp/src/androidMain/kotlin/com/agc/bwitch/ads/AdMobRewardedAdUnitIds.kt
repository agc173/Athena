package com.agc.bwitch.ads

import com.agc.bwitch.BuildConfig

object AdMobRewardedAdUnitIds {
    private const val TEST_REWARDED = "ca-app-pub-3940256099942544/5224354917"

    fun rewarded(placement: String): String? {
        return if (BuildConfig.DEBUG) {
            TEST_REWARDED
        } else {
            BuildConfig.ADMOB_REWARDED_AD_UNIT_ID.takeIf { it.isNotBlank() }
        }
    }
}
