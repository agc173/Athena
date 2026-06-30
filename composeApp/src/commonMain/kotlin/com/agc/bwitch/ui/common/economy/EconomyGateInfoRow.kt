package com.agc.bwitch.ui.common.economy

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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

    val visualStyle = resolveEconomyGateVisualStyle(
        preview = preview,
        label = label,
        freeLabelOverride = freeLabelOverride,
    )

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.small,
        color = visualStyle.containerColor(),
        contentColor = visualStyle.contentColor(),
        border = BorderStroke(1.dp, visualStyle.borderColor()),
    ) {
        Text(
            text = label,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            style = MaterialTheme.typography.labelMedium,
        )
    }
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

private enum class EconomyGateVisualStyle {
    FREE,
    PREMIUM,
    MOON,
    NEUTRAL,
}

private fun resolveEconomyGateVisualStyle(
    preview: EconomyModulePreview?,
    label: String,
    freeLabelOverride: String?,
): EconomyGateVisualStyle {
    if (preview == null && freeLabelOverride != null) return EconomyGateVisualStyle.FREE
    return when (preview?.nextSource) {
        EconomyNextSource.FREE -> EconomyGateVisualStyle.FREE
        EconomyNextSource.PREMIUM -> EconomyGateVisualStyle.PREMIUM
        EconomyNextSource.MOON -> EconomyGateVisualStyle.MOON
        EconomyNextSource.REJECTED -> when {
            preview.reasonIfRejected.equals("insufficient_moons", ignoreCase = true) -> EconomyGateVisualStyle.MOON
            else -> EconomyGateVisualStyle.NEUTRAL
        }
        else -> when {
            label.contains("premium", ignoreCase = true) -> EconomyGateVisualStyle.PREMIUM
            else -> EconomyGateVisualStyle.NEUTRAL
        }
    }
}

@Composable
private fun EconomyGateVisualStyle.containerColor() = when (this) {
    EconomyGateVisualStyle.FREE -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.32f)
    EconomyGateVisualStyle.PREMIUM -> MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.44f)
    EconomyGateVisualStyle.MOON -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.62f)
    EconomyGateVisualStyle.NEUTRAL -> MaterialTheme.colorScheme.surfaceVariant
}

@Composable
private fun EconomyGateVisualStyle.contentColor() = when (this) {
    EconomyGateVisualStyle.FREE -> MaterialTheme.colorScheme.primary
    EconomyGateVisualStyle.PREMIUM -> MaterialTheme.colorScheme.onTertiary
    EconomyGateVisualStyle.MOON -> MaterialTheme.colorScheme.secondary
    EconomyGateVisualStyle.NEUTRAL -> MaterialTheme.colorScheme.onSurfaceVariant
}

@Composable
private fun EconomyGateVisualStyle.borderColor() = when (this) {
    EconomyGateVisualStyle.FREE -> MaterialTheme.colorScheme.primary.copy(alpha = 0.38f)
    EconomyGateVisualStyle.PREMIUM -> MaterialTheme.colorScheme.tertiary.copy(alpha = 0.44f)
    EconomyGateVisualStyle.MOON -> MaterialTheme.colorScheme.secondary.copy(alpha = 0.38f)
    EconomyGateVisualStyle.NEUTRAL -> MaterialTheme.colorScheme.outline.copy(alpha = 0.35f)
}
