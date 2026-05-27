package com.agc.bwitch.notifications

import android.content.Intent
import android.util.Log
import com.agc.bwitch.presentation.navigation.Destination
import com.agc.bwitch.presentation.navigation.Navigator

object PushIntentRouter {
    const val EXTRA_PUSH_ROUTE = "push_route"
    const val EXTRA_PUSH_TYPE = "push_type"
    const val EXTRA_PUSH_CAMPAIGN_ID = "push_campaign_id"

    private const val TAG = "PushIntentRouter"

    fun handle(intent: Intent?, navigator: Navigator) {
        if (intent == null) return

        val route = intent.getStringExtra(EXTRA_PUSH_ROUTE)
        if (route.isNullOrBlank()) return

        val type = intent.getStringExtra(EXTRA_PUSH_TYPE)
        val campaignId = intent.getStringExtra(EXTRA_PUSH_CAMPAIGN_ID)
        Log.i(TAG, "Push intent received (route=$route, type=$type, campaignId=$campaignId)")

        when (route.lowercase()) {
            "horoscope" -> {
                navigator.navigate(Destination.HoroscopeDaily())
                Log.i(TAG, "Push navigation triggered (destination=HoroscopeDaily)")
            }
            else -> Log.i(TAG, "Push route ignored (route=$route)")
        }

        intent.removeExtra(EXTRA_PUSH_ROUTE)
        intent.removeExtra(EXTRA_PUSH_TYPE)
        intent.removeExtra(EXTRA_PUSH_CAMPAIGN_ID)
    }
}
