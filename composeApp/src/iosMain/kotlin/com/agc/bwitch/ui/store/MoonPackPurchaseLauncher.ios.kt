package com.agc.bwitch.ui.store

import androidx.compose.runtime.Composable

@Composable
actual fun rememberMoonPackPurchaseLauncher(): MoonPackPurchaseLauncher = object : MoonPackPurchaseLauncher {
    override suspend fun launch(productId: String): MoonPackPurchaseOutcome = MoonPackPurchaseOutcome.Unsupported
    override suspend fun consume(purchaseToken: String): Boolean = false
}
