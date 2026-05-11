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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.setValue
import com.agc.bwitch.domain.settings.KnownSubscriptionProducts
import com.agc.bwitch.domain.settings.PremiumSubscriptionStatus
import com.agc.bwitch.domain.settings.isActive
import com.agc.bwitch.localization.ProfileStrings
import com.agc.bwitch.localization.appStrings
import com.agc.bwitch.presentation.economy.EconomyViewModel
import com.agc.bwitch.presentation.moons.MoonStoreViewModel
import com.agc.bwitch.presentation.moons.STORE_COMING_SOON_KEY
import com.agc.bwitch.presentation.userprofile.SettingsUiEffect
import com.agc.bwitch.presentation.userprofile.SettingsViewModel
import com.agc.bwitch.presentation.userprofile.SubscriptionPlanSelection
import com.agc.bwitch.presentation.userprofile.SubscriptionPrimaryAction
import com.agc.bwitch.ui.premium.PremiumCard
import com.agc.bwitch.ui.userprofile.rememberSubscriptionManagementLauncher
import com.agc.bwitch.ui.userprofile.rememberSubscriptionPurchaseLauncher
import kotlinx.coroutines.flow.collect
import org.koin.compose.koinInject

@Composable
fun MoonStoreScreen(
    contentPadding: PaddingValues,
    viewModel: MoonStoreViewModel = koinInject(),
    economyViewModel: EconomyViewModel = koinInject(),
    settingsViewModel: SettingsViewModel = koinInject(),
) {
    val strings = appStrings.profile
    val premiumStrings = appStrings.premium
    val state by viewModel.uiState.collectAsState()
    val economyState by economyViewModel.uiState.collectAsState()
    val settingsState by settingsViewModel.uiState.collectAsState()
    val purchaseLauncher = rememberSubscriptionPurchaseLauncher()
    val managementLauncher = rememberSubscriptionManagementLauncher()
    val monthlyPlan = settingsState.subscriptionCatalog.firstOrNull { it.productId == KnownSubscriptionProducts.MONTHLY }
        ?: settingsState.subscriptionCatalog.firstOrNull { it.type == SubscriptionPlanSelection.Monthly }
    var premiumPaywallTracked by rememberSaveable { mutableStateOf(false) }
    // TODO(store): esta pantalla ya funciona como hub general de Store; renombrar archivo/composable en una pasada posterior.

    LaunchedEffect(economyState.isLoading, economyState.error, economyState.balance) {
        if (!economyState.isLoading) {
            println("[MoonStoreScreen] Economy backend balance=${economyState.balance}, error=${economyState.error}")
        }
    }
    LaunchedEffect(Unit) {
        economyViewModel.loadEconomy()
    }
    LaunchedEffect(
        settingsState.subscriptionStatus,
        settingsState.premiumSubscriptionStatus,
        settingsState.subscriptionPrimaryAction,
    ) {
        val shouldTrackPaywall = !settingsState.subscriptionStatus.isActive &&
            settingsState.premiumSubscriptionStatus != PremiumSubscriptionStatus.Unknown
        if (shouldTrackPaywall && !premiumPaywallTracked) {
            settingsViewModel.onPremiumPaywallShown(placement = "moon_store", originPlacement = "moon_store")
            if (settingsState.subscriptionPrimaryAction == SubscriptionPrimaryAction.Subscribe &&
                settingsState.premiumSubscriptionStatus != PremiumSubscriptionStatus.Pending
            ) {
                settingsViewModel.onPremiumCtaShown("moon_store_subscribe", originPlacement = "moon_store")
            }
            premiumPaywallTracked = true
        }
    }
    LaunchedEffect(monthlyPlan?.productId, monthlyPlan?.formattedPrice) {
        val plan = monthlyPlan ?: return@LaunchedEffect
        settingsViewModel.onPremiumProductLoaded(
            productId = plan.productId,
            price = plan.formattedPrice,
            originPlacement = "moon_store",
        )
    }
    LaunchedEffect(Unit) {
        settingsViewModel.uiEffects.collect { effect ->
            when (effect) {
                is SettingsUiEffect.LaunchSubscriptionPurchase -> {
                    val outcome = purchaseLauncher.launch(effect.plan)
                    settingsViewModel.onSubscriptionPurchaseCompleted(outcome)
                }
                is SettingsUiEffect.LaunchSubscriptionPurchaseWithProduct -> {
                    val outcome = purchaseLauncher.launch(effect.productId)
                    settingsViewModel.onSubscriptionPurchaseCompleted(outcome)
                }
                is SettingsUiEffect.LaunchManageSubscription -> {
                    val outcome = managementLauncher.launch(effect.productId)
                    settingsViewModel.onSubscriptionManagementCompleted(outcome)
                }
                SettingsUiEffect.RefreshEconomySnapshot -> {
                    economyViewModel.refreshEconomySnapshot()
                }
            }
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

        PremiumCard(
            strings = premiumStrings,
            priceLabel = monthlyPlan?.formattedPrice,
            statusLabel = when {
                settingsState.subscriptionStatus.isActive -> premiumStrings.active
                settingsState.premiumSubscriptionStatus == PremiumSubscriptionStatus.Pending -> premiumStrings.pending
                settingsState.premiumSubscriptionStatus == PremiumSubscriptionStatus.Unknown -> appStrings.settings.subscriptionStatusUnknown
                else -> appStrings.settings.subscriptionStatusInactive
            },
            isActive = settingsState.subscriptionStatus.isActive,
            isPending = settingsState.premiumSubscriptionStatus == PremiumSubscriptionStatus.Pending,
            isLoading = settingsState.isLoading,
            onSubscribeClick = {
                val productId = monthlyPlan?.productId
                if (productId != null) {
                    settingsViewModel.onCatalogSubscriptionSelected(productId, originPlacement = "moon_store")
                } else {
                    settingsViewModel.onSubscribeClicked(originPlacement = "moon_store")
                }
            },
            onRestoreClick = settingsViewModel::onRestorePurchasesClicked,
            onManageClick = { settingsViewModel.onSubscriptionPrimaryActionClicked(originPlacement = "moon_store") },
        )

        state.packs.forEach { pack ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(text = pack.localizedLabel(strings), style = MaterialTheme.typography.titleMedium)
                    Text(text = strings.storeMoonPackMoonsFormat.replaceFirst("%d", "${pack.moons}"))
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


private fun com.agc.bwitch.domain.moons.MoonPack.localizedLabel(strings: ProfileStrings): String = when (id) {
    "starter" -> strings.storeMoonPackStarterLabel
    "mystic" -> strings.storeMoonPackMysticLabel
    "coven" -> strings.storeMoonPackCovenLabel
    else -> label
}
