package com.agc.bwitch.ui.common.economy

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.agc.bwitch.domain.economy.EconomyModulePreview
import com.agc.bwitch.domain.economy.EconomyNextSource
import com.agc.bwitch.localization.EconomyStrings

@Composable
fun EconomyGateInfoRow(
    preview: EconomyModulePreview?,
    economyStrings: EconomyStrings,
    modifier: Modifier = Modifier,
    fallbackCost: Int? = null,
    packUsesLabel: String? = null,
    freeLabelOverride: String? = null,
) {
    val label = resolveEconomyGateLabel(
        preview = preview,
        economyStrings = economyStrings,
        fallbackCost = fallbackCost,
        packUsesLabel = packUsesLabel,
        freeLabelOverride = freeLabelOverride,
    ) ?: return

    Text(
        text = label,
        modifier = modifier,
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

fun resolveEconomyGateLabel(
    preview: EconomyModulePreview?,
    economyStrings: EconomyStrings,
    fallbackCost: Int? = null,
    packUsesLabel: String? = null,
    freeLabelOverride: String? = null,
): String? {
    val currentPreview = preview ?: return freeLabelOverride
    val packUses = currentPreview.moonPackUsesPerMoon?.takeIf { it > 1 }
    val packRemaining = currentPreview.moonRemaining?.takeIf { it > 0 }
    if (packRemaining != null && packUses != null) {
        val template = if (packRemaining == 1) economyStrings.packRemainingSingular else economyStrings.packRemainingPlural
        return template
            .replaceFirst("%d", packRemaining.toString())
            .replaceFirst("%d", packUses.toString())
    }

    return when (currentPreview.nextSource) {
        EconomyNextSource.FREE -> {
            val freeRemaining = currentPreview.freeRemaining
            when {
                freeLabelOverride != null -> freeLabelOverride
                freeRemaining == 1 -> economyStrings.freeRemainingTodaySingular
                freeRemaining != null && freeRemaining > 1 -> economyStrings.freeRemainingTodayPlural.replaceFirst("%d", freeRemaining.toString())
                else -> economyStrings.freeToday
            }
        }
        EconomyNextSource.PREMIUM -> economyStrings.includedWithPremium
        EconomyNextSource.MOON -> currentPreview.buildMoonCostLabel(
            economyStrings = economyStrings,
            fallbackCost = fallbackCost,
            packUsesLabel = packUsesLabel,
            packUsesFirst = true,
        )
        EconomyNextSource.REJECTED -> when {
            currentPreview.reasonIfRejected.equals("daily_limit", ignoreCase = true) -> economyStrings.dailyLimitReached
            currentPreview.reasonIfRejected.equals("insufficient_moons", ignoreCase = true) -> currentPreview.buildMoonCostLabel(
                economyStrings = economyStrings,
                fallbackCost = fallbackCost,
                packUsesLabel = packUsesLabel,
                packUsesFirst = true,
            )
            else -> null
        }
        EconomyNextSource.RULE_CONFIGURED_NOT_WIRED,
        EconomyNextSource.UNKNOWN,
        EconomyNextSource.NOT_CONFIGURED,
        EconomyNextSource.COMING_SOON,
        -> null
    }
}

private fun EconomyModulePreview.buildMoonCostLabel(
    economyStrings: EconomyStrings,
    fallbackCost: Int?,
    packUsesLabel: String?,
    packUsesFirst: Boolean = false,
): String? {
    val amount = cost.takeIf { it > 0 } ?: fallbackCost ?: return null
    val packUses = moonPackUsesPerMoon?.takeIf { it > 1 }
    return if (packUses != null) {
        if (packUsesLabel != null && packUsesFirst) {
            packUsesLabel
                .replaceFirst("%d", packUses.toString())
                .replaceFirst("%d", amount.toString())
        } else {
            (packUsesLabel ?: economyStrings.synastryMoonPackCostFormat)
                .replaceFirst("%d", amount.toString())
                .replaceFirst("%d", packUses.toString())
        }
    } else {
        economyStrings.moonCostFormat.replaceFirst("%d", amount.toString())
    }
}
