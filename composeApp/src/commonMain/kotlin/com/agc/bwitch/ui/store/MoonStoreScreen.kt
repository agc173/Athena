package com.agc.bwitch.ui.store

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.rememberSaveable
import androidx.compose.runtime.setValue
import com.agc.bwitch.localization.appStrings
import com.agc.bwitch.presentation.economy.EconomyViewModel
import com.agc.bwitch.presentation.moons.MoonStoreViewModel
import com.agc.bwitch.presentation.moons.STORE_COMING_SOON_KEY
import org.koin.compose.koinInject

@Composable
fun MoonStoreScreen(
    contentPadding: PaddingValues,
    viewModel: MoonStoreViewModel = koinInject(),
    economyViewModel: EconomyViewModel = koinInject(),
) {
    val strings = appStrings.profile
    val state by viewModel.uiState.collectAsState()
    val economyState by economyViewModel.uiState.collectAsState()
    // TODO(store): esta pantalla ya funciona como hub general de Store; renombrar archivo/composable en una pasada posterior.

    LaunchedEffect(economyState.isLoading, economyState.error, economyState.balance) {
        if (!economyState.isLoading) {
            println("[MoonStoreScreen] Economy backend balance=${economyState.balance}, error=${economyState.error}")
        }
    }
    var rewardedCtaTracked by rememberSaveable { mutableStateOf(false) }
    val rewardedCtaVisible = economyState.rewardedAdsRemaining > 0 &&
        !economyState.isClaimingRewardedAd &&
        !economyState.isLoading
    LaunchedEffect(rewardedCtaVisible, economyState.rewardedAdsRemaining) {
        when {
            rewardedCtaVisible && !rewardedCtaTracked -> {
                economyViewModel.onRewardedAdCtaShown(
                    placement = "moon_store",
                    rewardedAdsRemaining = economyState.rewardedAdsRemaining,
                )
                rewardedCtaTracked = true
            }
            !rewardedCtaVisible -> rewardedCtaTracked = false
        }
    }

    Column(
        modifier = Modifier
            .padding(contentPadding)
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = strings.storeLabel,
            style = MaterialTheme.typography.headlineSmall,
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = strings.storeCurrentBalanceTitle,
                    style = MaterialTheme.typography.titleSmall,
                )
                if (economyState.hasUsableSnapshot) {
                    Text(
                        text = strings.moonCreditsValueFormat.replaceFirst("%d", "${economyState.balance}"),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                } else {
                    Text(
                        text = strings.storeSyncingBalance,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = strings.storeFreeRewardsTodayTitle,
                    style = MaterialTheme.typography.titleSmall,
                )
                if (!economyState.dailyLoginClaimed) {
                    Button(
                        onClick = {
                            println("[MoonStoreScreen] CTA daily login tapped")
                            economyViewModel.claimDailyLogin()
                        },
                        enabled = !economyState.isClaimingDailyLogin && !economyState.isLoading,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (economyState.isClaimingDailyLogin) {
                            CircularProgressIndicator()
                        } else {
                            Text(strings.storeClaimOneMoonCta)
                        }
                    }
                } else {
                    Text(
                        text = strings.storeDailyClaimAlreadyDone,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Button(
                    onClick = {
                        println("[MoonStoreScreen] CTA rewarded ad tapped")
                        economyViewModel.claimRewardedAd()
                    },
                    enabled = rewardedCtaVisible,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (economyState.isClaimingRewardedAd) {
                        CircularProgressIndicator()
                    } else {
                        Text(strings.storeWatchAdRewardCta)
                    }
                }

                Text(
                    text = strings.storeAdsAvailableTodayFormat.replaceFirst("%d", "${economyState.rewardedAdsRemaining}"),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Text(
            text = strings.storePurchasesMonetizationTitle,
            style = MaterialTheme.typography.titleSmall,
        )
        state.packs.forEach { pack ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(text = pack.label, style = MaterialTheme.typography.titleMedium)
                    Text(text = "${pack.moons} ${strings.moonCreditsTitle}")
                    Text(text = pack.displayPrice, color = MaterialTheme.colorScheme.primary)
                    Button(
                        onClick = { viewModel.onBuyPackClicked(pack.id) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(strings.storeSoon)
                    }
                }
            }
        }

        Text(
            text = strings.storeFutureContentTitle,
            style = MaterialTheme.typography.titleSmall,
        )
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = strings.storeSubscriptionTitle,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = strings.storeSubscriptionPlaceholderDescription,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = strings.storeSoon,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = strings.storeTarotDecksTitle,
                    style = MaterialTheme.typography.titleSmall,
                )
                Text(
                    text = strings.storeTarotDecksPlaceholderDescription,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = strings.storeSoon,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }

        if (state.feedbackMessage?.startsWith(STORE_COMING_SOON_KEY) == true) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = strings.storeSoon,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Button(
                        onClick = viewModel::clearFeedback,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(appStrings.settings.close)
                    }
                }
            }
        }
    }
}
