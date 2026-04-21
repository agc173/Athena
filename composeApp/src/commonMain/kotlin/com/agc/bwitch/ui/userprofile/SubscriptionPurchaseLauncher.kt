package com.agc.bwitch.ui.userprofile

import androidx.compose.runtime.Composable
import com.agc.bwitch.presentation.userprofile.SubscriptionPlanSelection
import com.agc.bwitch.presentation.userprofile.SubscriptionPurchaseOutcome

@Composable
expect fun rememberSubscriptionPurchaseLauncher(): SubscriptionPurchaseLauncher

interface SubscriptionPurchaseLauncher {
    suspend fun launch(plan: SubscriptionPlanSelection): SubscriptionPurchaseOutcome
}
