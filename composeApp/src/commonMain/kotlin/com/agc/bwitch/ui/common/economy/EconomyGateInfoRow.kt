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
    val currentPreview = preview ?: return null
    return when (currentPreview.nextSource) {
        EconomyNextSource.FREE -> freeLabelOverride ?: economyStrings.freeToday
        EconomyNextSource.PREMIUM -> economyStrings.includedWithPremium
        EconomyNextSource.MOON -> currentPreview.buildMoonCostLabel(
            economyStrings = economyStrings,
            fallbackCost = fallbackCost,
            packUsesLabel = packUsesLabel,
        )
        EconomyNextSource.REJECTED -> when {
            currentPreview.reasonIfRejected.equals("daily_limit", ignoreCase = true) -> economyStrings.dailyLimitReached
            currentPreview.reasonIfRejected.equals("insufficient_moons", ignoreCase = true) -> currentPreview.buildMoonCostLabel(
                economyStrings = economyStrings,
                fallbackCost = fallbackCost,
                packUsesLabel = packUsesLabel,
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
): String? {
    val amount = cost.takeIf { it > 0 } ?: fallbackCost ?: return null
    val packUses = moonPackUsesPerMoon?.takeIf { it > 1 }
    return if (packUses != null) {
        (packUsesLabel ?: economyStrings.synastryMoonPackCostFormat)
            .replaceFirst("%d", amount.toString())
            .replaceFirst("%d", packUses.toString())
    } else {
        economyStrings.moonCostFormat.replaceFirst("%d", amount.toString())
    }
}
