package com.agc.bwitch.presentation.economy

import com.agc.bwitch.domain.economy.EconomyModulePreview
import com.agc.bwitch.domain.economy.EconomyNextSource

data class EconomyGateDecision(
    val shouldOpenPaywall: Boolean,
    val requiredMoons: Int,
    val source: String,
)

fun EconomyModulePreview?.toMoonGateDecision(
    source: String,
    fallbackCost: Int? = null,
): EconomyGateDecision? {
    val preview = this ?: return null
    val isInsufficientMoonsGate =
        preview.nextSource == EconomyNextSource.REJECTED &&
            preview.reasonIfRejected.equals("insufficient_moons", ignoreCase = true) &&
            !preview.canExecute
    if (!isInsufficientMoonsGate) return null

    val requiredMoons = preview.cost
        .takeIf { it > 0 }
        ?: fallbackCost
        ?: return null

    return EconomyGateDecision(
        shouldOpenPaywall = true,
        requiredMoons = requiredMoons,
        source = source,
    )
}

fun runWithEconomyGate(
    preview: EconomyModulePreview?,
    economyViewModel: EconomyViewModel,
    source: String,
    fallbackCost: Int? = null,
    action: () -> Unit,
) {
    val decision = preview.toMoonGateDecision(
        source = source,
        fallbackCost = fallbackCost,
    )
    if (decision?.shouldOpenPaywall == true) {
        economyViewModel.requireLunas(
            cost = decision.requiredMoons,
            source = decision.source,
        ) { _ ->
            action()
        }
        return
    }

    action()
}
