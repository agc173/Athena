package com.agc.bwitch.domain.settings

sealed interface RestorePurchasesResult {
    /**
     * Local billing found purchase tokens. These are not Premium authority until backend validation succeeds.
     * Presentation must send these tokens to restoreGooglePlayPurchases before showing Premium as active.
     */
    data class RestorableTokens(val tokens: List<BillingPurchaseToken>) : RestorePurchasesResult

    @Deprecated("Local restore must not grant Premium. Use RestorableTokens and backend validation instead.")
    data class Restored(val status: SubscriptionStatus) : RestorePurchasesResult

    data object NoPurchasesFound : RestorePurchasesResult
}
