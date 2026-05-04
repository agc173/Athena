package com.agc.bwitch.ui.store

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.agc.bwitch.localization.appStrings
import com.agc.bwitch.presentation.economy.EconomyUiState

@Composable
fun MoonPaywallDialog(
    economyState: EconomyUiState,
    requiredMoons: Int,
    source: String? = null,
    onDismiss: () -> Unit,
    onClaimDaily: () -> Unit,
    onClaimRewardedAd: () -> Unit,
    onOpenStore: () -> Unit,
    onRewardedAdCtaShown: () -> Unit,
) {
    val strings = appStrings
    var rewardedCtaTracked by rememberSaveable { mutableStateOf(false) }
    val rewardedCtaVisible = economyState.rewardedAdsRemaining > 0 && !economyState.isClaimingRewardedAd
    LaunchedEffect(rewardedCtaVisible) {
        when {
            rewardedCtaVisible && !rewardedCtaTracked -> {
                onRewardedAdCtaShown()
                rewardedCtaTracked = true
            }
            !rewardedCtaVisible -> rewardedCtaTracked = false
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(text = contextualPaywallTitle(source, strings.profile.moonPaywallTitle))
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = strings.profile.moonCreditsValueFormat.replaceFirst("%d", "${economyState.balance}"),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = contextualPaywallMessage(source, requiredMoons, strings.profile.moonPaywallNeedFormat),
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedButton(
                    onClick = onClaimDaily,
                    enabled = !economyState.dailyLoginClaimed && !economyState.isClaimingDailyLogin,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(strings.profile.moonPaywallClaimDailyCta)
                }
                OutlinedButton(
                    onClick = onClaimRewardedAd,
                    enabled = rewardedCtaVisible,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(strings.profile.moonPaywallWatchAdCta)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onOpenStore) {
                Text(strings.profile.moonPaywallOpenStoreCta)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(strings.profile.moonPaywallDismissCta)
            }
        },
        modifier = Modifier.padding(8.dp),
    )
}

private fun contextualPaywallTitle(source: String?, fallback: String): String = when (source?.lowercase()) {
    "synastry", "pendulum" -> appStrings.economy.paywallFreeUsedTitle
    else -> fallback
}

private fun contextualPaywallMessage(source: String?, requiredMoons: Int, fallbackFormat: String): String = when (source?.lowercase()) {
    "synastry" -> appStrings.economy.synastryPaywallFreeUsedMessage
        .replaceFirst("%d", "3")
        .replaceFirst("%d", "$requiredMoons")
    "pendulum" -> appStrings.economy.pendulumPaywallFreeUsedMessage
        .replaceFirst("%d", "10")
        .replaceFirst("%d", "$requiredMoons")
    else -> fallbackFormat.replaceFirst("%d", "$requiredMoons")
}
