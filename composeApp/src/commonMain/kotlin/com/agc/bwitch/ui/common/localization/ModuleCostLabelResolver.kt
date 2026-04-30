package com.agc.bwitch.ui.common.localization

import com.agc.bwitch.localization.EconomyStrings
import com.agc.bwitch.presentation.economy.ModuleCostLabel

fun ModuleCostLabel.resolve(strings: EconomyStrings): String {
    return when (this) {
        ModuleCostLabel.FreeToday -> strings.freeToday
        ModuleCostLabel.FreeThisWeek -> strings.freeThisWeek
        ModuleCostLabel.IncludedWithPremium -> strings.includedWithPremium
        is ModuleCostLabel.MoonCost -> strings.moonCostFormat.replace("%d", amount.toString())
        ModuleCostLabel.NotEnoughMoons -> strings.notEnoughMoons
    }
}
