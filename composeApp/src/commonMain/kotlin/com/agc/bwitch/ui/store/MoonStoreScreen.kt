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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
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
    val safeDailyClaimLabel = "${strings.storeOpen} +1 ${strings.moonCreditsTitle}"
    val safeRewardedClaimLabel = "${appStrings.oracle.retryCta} +1 ${strings.moonCreditsTitle}"

    val visibleBalance = if (!economyState.isLoading && economyState.error == null) {
        economyState.balance
    } else {
        state.balance
    }

    LaunchedEffect(economyState.isLoading, economyState.error, economyState.balance) {
        if (!economyState.isLoading) {
            println("[MoonStoreScreen] Economy backend balance=${economyState.balance}, error=${economyState.error}")
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
            text = strings.moonCreditsValueFormat.replaceFirst("%d", "$visibleBalance"),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary,
        )

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (!economyState.dailyLoginClaimed) {
                    Button(
                        onClick = economyViewModel::claimDailyLogin,
                        enabled = !economyState.isClaimingDailyLogin && !economyState.isLoading,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        if (economyState.isClaimingDailyLogin) {
                            CircularProgressIndicator()
                        } else {
                            Text(safeDailyClaimLabel)
                        }
                    }
                }

                Button(
                    onClick = economyViewModel::claimRewardedAd,
                    enabled = !economyState.isClaimingRewardedAd &&
                        !economyState.isLoading &&
                        economyState.rewardedAdsRemaining > 0,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (economyState.isClaimingRewardedAd) {
                        CircularProgressIndicator()
                    } else {
                        Text(safeRewardedClaimLabel)
                    }
                }

                Text(
                    text = "${appStrings.oracle.adUnlockRemainingLabel}: ${economyState.rewardedAdsRemaining}",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

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
