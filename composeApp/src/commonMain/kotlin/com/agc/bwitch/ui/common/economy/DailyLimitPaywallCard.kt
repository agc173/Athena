package com.agc.bwitch.ui.common.economy

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.agc.bwitch.localization.EconomyStrings
import com.agc.bwitch.ui.common.designsystem.BWitchCard
import com.agc.bwitch.ui.common.designsystem.BWitchPrimaryButton

@Composable
fun DailyLimitPaywallCard(
    economyStrings: EconomyStrings,
    onOpenStore: () -> Unit,
    modifier: Modifier = Modifier,
    module: String = "unknown",
    placement: String = "daily_limit_paywall",
    reason: String = "daily_limit",
    hasPremiumBenefit: Boolean = false,
    onPaywallShown: ((module: String, placement: String, reason: String) -> Unit)? = null,
    onPaywallActionClicked: ((module: String, placement: String, action: String) -> Unit)? = null,
) {
    val impressionKey = "$module|$placement|$reason"
    var trackedImpressionKey by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(impressionKey) {
        if (trackedImpressionKey != impressionKey) {
            onPaywallShown?.invoke(module, placement, reason)
            trackedImpressionKey = impressionKey
        }
    }

    BWitchCard(modifier = modifier.fillMaxWidth()) {
        Text(
            text = if (hasPremiumBenefit) {
                economyStrings.dailyLimitPaywallPremiumTitle
            } else {
                economyStrings.dailyLimitPaywallTitle
            },
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = if (hasPremiumBenefit) {
                economyStrings.dailyLimitPaywallPremiumBody
            } else {
                economyStrings.dailyLimitPaywallBody
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        BWitchPrimaryButton(
            onClick = {
                onPaywallActionClicked?.invoke(module, placement, "open_store")
                onOpenStore()
            },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                if (hasPremiumBenefit) {
                    economyStrings.dailyLimitPaywallPremiumCta
                } else {
                    economyStrings.dailyLimitPaywallCta
                },
            )
        }
    }
}

fun com.agc.bwitch.domain.economy.EconomyModulePreview?.isDailyLimitRejected(): Boolean {
    val preview = this ?: return false
    val reason = preview.reasonIfRejected.orEmpty().lowercase()
    return !preview.canExecute && (
        reason == "daily_limit" ||
            reason.contains("daily_limit") ||
            reason.contains("pack_limit") ||
            reason.contains("limit_reached")
    )
}

fun com.agc.bwitch.domain.economy.EconomyModulePreview?.hasPremiumBenefit(): Boolean {
    val preview = this ?: return false
    return preview.premiumRemaining != null ||
        preview.nextSource == com.agc.bwitch.domain.economy.EconomyNextSource.PREMIUM
}
