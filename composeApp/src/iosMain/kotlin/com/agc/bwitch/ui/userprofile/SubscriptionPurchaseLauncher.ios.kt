package com.agc.bwitch.ui.userprofile

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.agc.bwitch.presentation.userprofile.SubscriptionManagementOutcome
import com.agc.bwitch.presentation.userprofile.SubscriptionPlanSelection
import com.agc.bwitch.presentation.userprofile.SubscriptionPurchaseOutcome

@Composable
actual fun rememberSubscriptionPurchaseLauncher(): SubscriptionPurchaseLauncher = remember {
    object : SubscriptionPurchaseLauncher {
        override suspend fun launch(plan: SubscriptionPlanSelection): SubscriptionPurchaseOutcome =
            SubscriptionPurchaseOutcome.Unsupported

        override suspend fun launch(productId: String): SubscriptionPurchaseOutcome =
            SubscriptionPurchaseOutcome.Unsupported

        override suspend fun acknowledge(purchaseToken: String): Boolean = false
    }
}

@Composable
actual fun rememberSubscriptionManagementLauncher(): SubscriptionManagementLauncher = remember {
    object : SubscriptionManagementLauncher {
        override suspend fun launch(productId: String?): SubscriptionManagementOutcome =
            SubscriptionManagementOutcome.Unsupported
    }
}
