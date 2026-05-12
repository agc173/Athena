package com.agc.bwitch.ui.userprofile

import androidx.compose.runtime.Composable
import com.agc.bwitch.presentation.userprofile.SubscriptionManagementOutcome
import com.agc.bwitch.presentation.userprofile.SubscriptionPlanSelection
import com.agc.bwitch.presentation.userprofile.SubscriptionPurchaseOutcome

@Composable
expect fun rememberSubscriptionPurchaseLauncher(): SubscriptionPurchaseLauncher

@Composable
expect fun rememberSubscriptionManagementLauncher(): SubscriptionManagementLauncher

interface SubscriptionPurchaseLauncher {
    suspend fun launch(plan: SubscriptionPlanSelection): SubscriptionPurchaseOutcome
    suspend fun launch(productId: String): SubscriptionPurchaseOutcome
    suspend fun acknowledge(purchaseToken: String): Boolean
}

interface SubscriptionManagementLauncher {
    suspend fun launch(productId: String?): SubscriptionManagementOutcome
}
