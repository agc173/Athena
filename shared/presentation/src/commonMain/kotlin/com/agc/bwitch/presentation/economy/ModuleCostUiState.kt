package com.agc.bwitch.presentation.economy

import com.agc.bwitch.domain.economy.EconomyModulePreview
import com.agc.bwitch.domain.economy.EconomyNextSource

data class ModuleCostUiState(
    val label: String,
)

fun EconomyModulePreview.toModuleCostUiStateOrNull(): ModuleCostUiState? {
    return when (nextSource) {
        EconomyNextSource.FREE -> ModuleCostUiState(label = "Gratis hoy")
        EconomyNextSource.PREMIUM -> ModuleCostUiState(label = "Incluido con Premium")
        EconomyNextSource.MOON -> ModuleCostUiState(label = "${cost.coerceAtLeast(0)} 🌙")
        EconomyNextSource.REJECTED -> {
            if (reasonIfRejected.equals("insufficient_moons", ignoreCase = true)) {
                ModuleCostUiState(label = "Faltan lunas")
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

