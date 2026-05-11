package com.agc.bwitch.domain.settings

sealed interface RestorePurchasesResult {
    /**
     * Local billing found purchase tokens. These are not Premium authority until backend validation succeeds.
     * TODO(PR4): send these tokens to restoreGooglePlayPurchases.
     */
    data class RestorableTokens(val tokens: List<BillingPurchaseToken>) : RestorePurchasesResult

    @Deprecated("Local restore must not grant Premium. Use RestorableTokens and backend validation instead.")
    data class Restored(val status: SubscriptionStatus) : RestorePurchasesResult

    data object NoPurchasesFound : RestorePurchasesResult
}
