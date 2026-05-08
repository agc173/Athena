package com.agc.bwitch.ui.common.economy

import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.agc.bwitch.localization.EconomyStrings
import com.agc.bwitch.ui.common.designsystem.BWitchCard
import com.agc.bwitch.ui.common.designsystem.BWitchPrimaryButton

@Composable
fun DailyLimitPaywallCard(
    economyStrings: EconomyStrings,
    onOpenStore: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BWitchCard(modifier = modifier.fillMaxWidth()) {
        Text(
            text = economyStrings.dailyLimitPaywallTitle,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = economyStrings.dailyLimitPaywallBody,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        BWitchPrimaryButton(
            onClick = onOpenStore,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(economyStrings.dailyLimitPaywallCta)
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
