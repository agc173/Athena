package com.agc.bwitch.domain.moons

data class MoonBalance(
    val amount: Int,
)

data class MoonPack(
    val id: String,
    val moons: Int,
    val label: String,
    val displayPrice: String,
    val displayOrder: Int,
)

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
