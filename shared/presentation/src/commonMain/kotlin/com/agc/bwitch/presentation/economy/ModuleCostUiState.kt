package com.agc.bwitch.presentation.economy

import com.agc.bwitch.domain.economy.EconomyModulePreview
import com.agc.bwitch.domain.economy.EconomyNextSource

sealed interface ModuleCostLabel {
    data object FreeToday : ModuleCostLabel
    data object FreeThisWeek : ModuleCostLabel
    data object IncludedWithPremium : ModuleCostLabel
    data class MoonCost(val amount: Int) : ModuleCostLabel
    data object NotEnoughMoons : ModuleCostLabel
}

data class ModuleCostUiState(
    val label: ModuleCostLabel,
)

fun EconomyModulePreview.toModuleCostUiStateOrNull(): ModuleCostUiState? {
    return when (nextSource) {
        EconomyNextSource.FREE -> ModuleCostUiState(label = ModuleCostLabel.FreeToday)
        EconomyNextSource.PREMIUM -> ModuleCostUiState(label = ModuleCostLabel.IncludedWithPremium)
        EconomyNextSource.MOON -> ModuleCostUiState(label = ModuleCostLabel.MoonCost(cost.coerceAtLeast(0)))
        EconomyNextSource.REJECTED -> {
            if (reasonIfRejected.equals("insufficient_moons", ignoreCase = true)) {
                ModuleCostUiState(label = ModuleCostLabel.NotEnoughMoons)
            } else {
                null
            }
        }
        EconomyNextSource.RULE_CONFIGURED_NOT_WIRED,
        EconomyNextSource.UNKNOWN,
        EconomyNextSource.NOT_CONFIGURED,
        EconomyNextSource.COMING_SOON,
        -> null
    }
}
