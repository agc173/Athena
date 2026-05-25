package com.agc.bwitch.domain.moons

data class MoonBalance(
    val amount: Int,
)

data class MoonPack(
    val productId: String,
    val moonAmount: Int,
    val label: String,
    val localizedPrice: String?,
    val displayOrder: Int,
    val status: MoonPackProductStatus = MoonPackProductStatus.Loading,
)

enum class MoonPackProductStatus {
    Loading,
    Available,
    Unavailable,
}

enum class MoonUnlockFeature {
    TarotExtraReading,
    HoroscopeFutureDay,
}

object MoonUnlockCostCatalog {
    private val costs = mapOf(
        MoonUnlockFeature.TarotExtraReading to 3,
        MoonUnlockFeature.HoroscopeFutureDay to 1,
    )

    fun costFor(feature: MoonUnlockFeature): Int = costs.getValue(feature)
}

sealed interface SpendMoonsResult {
    data class Success(val updatedBalance: MoonBalance) : SpendMoonsResult
    data class InsufficientBalance(
        val currentBalance: MoonBalance,
        val required: Int,
    ) : SpendMoonsResult
}
