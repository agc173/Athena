package com.agc.bwitch.ui.store

import com.agc.bwitch.domain.settings.GooglePlayPurchase

sealed interface MoonPackPurchaseOutcome {
    data class Purchased(val purchase: GooglePlayPurchase) : MoonPackPurchaseOutcome
    data class Pending(val purchase: GooglePlayPurchase) : MoonPackPurchaseOutcome
    data object Cancelled : MoonPackPurchaseOutcome
    data object Failed : MoonPackPurchaseOutcome
    data object Unsupported : MoonPackPurchaseOutcome
}

interface MoonPackPurchaseLauncher {
    suspend fun launch(productId: String): MoonPackPurchaseOutcome
    suspend fun consume(purchaseToken: String): Boolean
}

expect fun rememberMoonPackPurchaseLauncher(): MoonPackPurchaseLauncher
