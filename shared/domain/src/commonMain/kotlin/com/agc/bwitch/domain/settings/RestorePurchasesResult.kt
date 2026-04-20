package com.agc.bwitch.domain.settings

sealed interface RestorePurchasesResult {
    data class Restored(val status: SubscriptionStatus) : RestorePurchasesResult
    data object NoPurchasesFound : RestorePurchasesResult
}
