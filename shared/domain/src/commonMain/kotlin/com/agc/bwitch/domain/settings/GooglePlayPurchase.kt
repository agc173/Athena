package com.agc.bwitch.domain.settings

enum class GooglePlayPurchaseState {
    Purchased,
    Pending,
    Unknown,
}

data class GooglePlayPurchase(
    val productId: String,
    val purchaseToken: String,
    val purchaseState: GooglePlayPurchaseState,
    val isAcknowledged: Boolean,
    val orderId: String?,
    val packageName: String,
)
