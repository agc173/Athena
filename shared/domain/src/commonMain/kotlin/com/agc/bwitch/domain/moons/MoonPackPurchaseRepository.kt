package com.agc.bwitch.domain.moons

import com.agc.bwitch.domain.settings.GooglePlayPurchase

enum class MoonPackClaimStatus { CLAIMED, ALREADY_CLAIMED }

data class MoonPackClaimResult(val status: MoonPackClaimStatus, val shouldConsume: Boolean)

interface MoonPackPurchaseRepository {
    suspend fun claimGooglePlayMoonPackPurchase(purchase: GooglePlayPurchase): MoonPackClaimResult
}
