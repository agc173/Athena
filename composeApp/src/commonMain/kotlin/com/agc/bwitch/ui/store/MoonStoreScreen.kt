package com.agc.bwitch.ui.store

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import com.agc.bwitch.domain.settings.SubscriptionStatus
import com.agc.bwitch.localization.ProfileStrings
import com.agc.bwitch.localization.SettingsStrings
import com.agc.bwitch.localization.appStrings
import com.agc.bwitch.presentation.economy.EconomyViewModel
import com.agc.bwitch.presentation.moons.MoonStoreViewModel
import com.agc.bwitch.domain.moons.MoonPackProductStatus
import com.agc.bwitch.presentation.moons.MoonStoreUiEffect
import com.agc.bwitch.presentation.moons.STORE_PURCHASE_CANCELLED_KEY
import com.agc.bwitch.presentation.moons.STORE_PURCHASE_COMPLETED_KEY
import com.agc.bwitch.presentation.moons.STORE_PURCHASE_COMPLETED_WITH_CONSUME_FAILED_KEY
import com.agc.bwitch.presentation.moons.STORE_PURCHASE_CONSUME_FAILED_KEY
import com.agc.bwitch.presentation.moons.STORE_PURCHASE_FAILED_KEY
import com.agc.bwitch.presentation.moons.STORE_PURCHASE_PENDING_KEY
import com.agc.bwitch.presentation.moons.STORE_LOAD_ERROR_KEY
import com.agc.bwitch.presentation.userprofile.SettingsUiEffect
import com.agc.bwitch.presentation.userprofile.SettingsFeedback
import com.agc.bwitch.presentation.userprofile.SettingsViewModel
import com.agc.bwitch.presentation.userprofile.SubscriptionPrimaryAction
import com.agc.bwitch.presentation.ads.RewardedAdResult
import com.agc.bwitch.presentation.ads.RewardedAdsService
import com.agc.bwitch.ui.common.premium.PremiumCard
import com.agc.bwitch.ui.common.premium.PremiumBenefitsList
import com.agc.bwitch.ui.userprofile.rememberSubscriptionManagementLauncher
import com.agc.bwitch.ui.userprofile.rememberSubscriptionPurchaseLauncher
import com.agc.bwitch.ui.store.rememberMoonPackPurchaseLauncher
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

private const val STORE_REWARDED_AD_UNAVAILABLE_KEY = "store_rewarded_ad_unavailable"
private const val STORE_REWARDED_AD_FAILED_KEY = "store_rewarded_ad_failed"
private const val STORE_REWARDED_AD_CANCELLED_KEY = "store_rewarded_ad_cancelled"

@Composable
fun MoonStoreScreen(
    contentPadding: PaddingValues,
    viewModel: MoonStoreViewModel = koinInject(),
    economyViewModel: EconomyViewModel = koinInject(),
    settingsViewModel: SettingsViewModel = koinInject(),
    rewardedAdsService: RewardedAdsService = koinInject(),
) {
    val strings = appStrings.profile
    val settingsStrings = appStrings.settings
    val state by viewModel.uiState.collectAsState()
    val economyState by economyViewModel.uiState.collectAsState()
    val settingsState by settingsViewModel.uiState.collectAsState()
    val purchaseLauncher = rememberSubscriptionPurchaseLauncher()
    val moonPackPurchaseLauncher = rememberMoonPackPurchaseLauncher()
    val managementLauncher = rememberSubscriptionManagementLauncher()
    val scope = rememberCoroutineScope()
    var isRewardedAdFlowRunning by rememberSaveable { mutableStateOf(false) }
    var rewardedAdFeedbackKey by rememberSaveable { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    // TODO(store): esta pantalla ya funciona como hub general de Store; renombrar archivo/composable en una pasada posterior.

    LaunchedEffect(economyState.isLoading, economyState.error, economyState.balance) {
        if (!economyState.isLoading) {
            println("[MoonStoreScreen] Economy backend balance=${economyState.balance}, error=${economyState.error}")
        }
    }
    LaunchedEffect(Unit) {
        economyViewModel.loadEconomy()
    }

    LaunchedEffect(settingsState.subscriptionPrimaryAction) {
        if (settingsState.subscriptionPrimaryAction == SubscriptionPrimaryAction.Subscribe) {
            settingsViewModel.onPremiumCtaShown("moon_store_subscribe")
        }
    }
    LaunchedEffect(settingsState.feedback) {
        val feedback = settingsState.feedback ?: return@LaunchedEffect
        val message = when (feedback) {
            SettingsFeedback.SubscriptionSubscribeComingSoon -> settingsStrings.subscriptionSubscribeComingSoon
            SettingsFeedback.SubscriptionManageComingSoon -> settingsStrings.subscriptionManageComingSoon
            SettingsFeedback.SubscriptionPurchaseFailed -> settingsStrings.subscriptionPurchaseFailed
            SettingsFeedback.RestorePurchasesSuccess -> settingsStrings.subscriptionRestoreSuccess
            SettingsFeedback.RestorePurchasesNoPurchases -> settingsStrings.subscriptionRestoreNoPurchases
            SettingsFeedback.DeleteAccountRequested -> settingsStrings.deleteAccountRequestedFeedback
            SettingsFeedback.NotificationsPermissionDenied,
            SettingsFeedback.NotificationsUnavailable -> return@LaunchedEffect
        }
        println("BWITCH_PREMIUM_DEBUG restore_feedback=$feedback")
        snackbarHostState.showSnackbar(message)
        settingsViewModel.onFeedbackConsumed()
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

                is SettingsUiEffect.AcknowledgeGooglePlayPurchase -> {
                    purchaseLauncher.acknowledge(effect.purchaseToken)
                }

                SettingsUiEffect.RefreshEconomy -> {
                    economyViewModel.loadEconomy()
                }
                SettingsUiEffect.RequestPushPermissionAndToken -> Unit
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.uiEffects.collect { effect ->
            when (effect) {
                is MoonStoreUiEffect.LaunchMoonPackPurchase -> {
                    when (val outcome = moonPackPurchaseLauncher.launch(effect.productId)) {
                        is MoonPackPurchaseOutcome.Purchased -> viewModel.onPurchaseCompleted(outcome.purchase)
                        is MoonPackPurchaseOutcome.Pending -> viewModel.onPurchasePending(effect.productId)
                        MoonPackPurchaseOutcome.Cancelled -> viewModel.onPurchaseCancelled(effect.productId)
                        MoonPackPurchaseOutcome.Failed, MoonPackPurchaseOutcome.Unsupported -> viewModel.onPurchaseFailed(effect.productId)
                    }
                }
                is MoonStoreUiEffect.ConsumeMoonPackPurchase -> {
                    if (!moonPackPurchaseLauncher.consume(effect.token)) viewModel.onConsumeFailed()
                }
                MoonStoreUiEffect.RefreshEconomy -> economyViewModel.loadEconomy()
            }
        }
    }

    var rewardedCtaTracked by rememberSaveable { mutableStateOf(false) }
    val rewardedCtaVisible = economyState.rewardedAdsRemaining > 0 &&
        !economyState.isClaimingRewardedAd &&
        !isRewardedAdFlowRunning &&
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

    LaunchedEffect(state.feedbackMessage) {
        state.feedbackMessage?.let { feedbackKey ->
            snackbarHostState.showSnackbar(feedbackKey.toLocalizedFeedback(strings))
            viewModel.clearFeedback()
        }
    }
    LaunchedEffect(settingsState.error) {
        settingsState.error?.let { snackbarHostState.showSnackbar(it) }
    }
    LaunchedEffect(rewardedAdFeedbackKey) {
        rewardedAdFeedbackKey?.let { feedbackKey ->
            snackbarHostState.showSnackbar(feedbackKey.toLocalizedFeedback(strings))
            rewardedAdFeedbackKey = null
        }
    }

    Box(modifier = Modifier.fillMaxWidth()) {
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
                        if (isRewardedAdFlowRunning) return@Button
                        println("[MoonStoreScreen] CTA rewarded ad tapped")
                        scope.launch {
                            isRewardedAdFlowRunning = true
                            try {
                                when (rewardedAdsService.showRewardedAd(placement = "moon_store")) {
                                    RewardedAdResult.Completed -> economyViewModel.claimRewardedAd(placement = "moon_store")
                                    RewardedAdResult.Cancelled -> rewardedAdFeedbackKey = STORE_REWARDED_AD_CANCELLED_KEY
                                    is RewardedAdResult.Failed -> rewardedAdFeedbackKey = STORE_REWARDED_AD_FAILED_KEY
                                    RewardedAdResult.Unavailable -> rewardedAdFeedbackKey = STORE_REWARDED_AD_UNAVAILABLE_KEY
                                }
                            } catch (error: Throwable) {
                                println("[MoonStoreScreen] rewarded ad flow failed: ${error.message}")
                                rewardedAdFeedbackKey = STORE_REWARDED_AD_FAILED_KEY
                            } finally {
                                isRewardedAdFlowRunning = false
                            }
                        }
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
                    Text(text = pack.localizedLabel(strings), style = MaterialTheme.typography.titleMedium)
                    Text(text = strings.storeMoonPackMoonsFormat.replaceFirst("%d", "${pack.moonAmount}"))
                    Text(text = pack.localizedPrice ?: "—", color = MaterialTheme.colorScheme.primary)
                    val canShowBuy = pack.status == MoonPackProductStatus.Available
                    Button(
                        onClick = { viewModel.onBuyPackClicked(pack.productId) },
                        enabled = canShowBuy && !state.isLoading && !state.isPurchaseInProgress,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(if (canShowBuy) strings.storeMoonPackBuy else strings.storeMoonPackUnavailable)
                    }
                }
            }
        }

        val premiumCardStatus = settingsState.subscriptionStatus.resolveStoreStatus(economyState)
        println(
            "BWITCH_PREMIUM_DEBUG premium_card_status_displayed=$premiumCardStatus " +
                "settings=${settingsState.subscriptionStatus} economyIsPremium=${economyState.isPremium} " +
                "economyHasSnapshot=${economyState.hasUsableSnapshot}"
        )
        PremiumCard(
            title = strings.storeSubscriptionTitle,
            subtitle = appStrings.premiumBenefits.subtitle,
            statusLabel = premiumCardStatus.toLocalizedLabel(settingsStrings),
            primaryActionLabel = when (premiumCardStatus.toPrimaryAction()) {
                SubscriptionPrimaryAction.Subscribe -> settingsStrings.subscriptionActionSubscribe
                SubscriptionPrimaryAction.Manage -> settingsStrings.subscriptionActionManage
            },
            restoreActionLabel = settingsStrings.restorePurchases,
            onPrimaryActionClick = settingsViewModel::onSubscriptionPrimaryActionClicked,
            onRestoreActionClick = settingsViewModel::onRestorePurchasesClicked,
        )
        PremiumBenefitsList(
            bullets = appStrings.premiumBenefits.bullets,
            disclaimer = appStrings.premiumBenefits.disclaimer,
        )

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

        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = contentPadding.calculateTopPadding() + 8.dp, start = 16.dp, end = 16.dp),
        )
    }
}


private fun String.toLocalizedFeedback(strings: ProfileStrings): String = when (this) {
    STORE_PURCHASE_CANCELLED_KEY -> strings.storePurchaseCancelledFeedback
    STORE_PURCHASE_PENDING_KEY -> strings.storePurchasePendingFeedback
    STORE_PURCHASE_FAILED_KEY -> strings.storePurchaseFailedFeedback
    STORE_PURCHASE_COMPLETED_KEY -> strings.storePurchaseCompletedFeedback
    STORE_PURCHASE_COMPLETED_WITH_CONSUME_FAILED_KEY ->
        "${strings.storePurchaseCompletedFeedback} ${strings.storePurchaseConsumeFailedFeedback}"
    STORE_PURCHASE_CONSUME_FAILED_KEY -> strings.storePurchaseConsumeFailedFeedback
    STORE_LOAD_ERROR_KEY -> strings.storeLoadErrorFeedback
    STORE_REWARDED_AD_UNAVAILABLE_KEY -> strings.storeRewardedAdUnavailableFeedback
    STORE_REWARDED_AD_FAILED_KEY -> strings.storeRewardedAdFailedFeedback
    STORE_REWARDED_AD_CANCELLED_KEY -> strings.storeRewardedAdCancelledFeedback
    else -> this
}

private fun com.agc.bwitch.domain.moons.MoonPack.localizedLabel(strings: ProfileStrings): String = when (productId) {
    "bwitch_moons_pack_10" -> strings.storeMoonPackStarterLabel
    "bwitch_moons_pack_30" -> strings.storeMoonPackMysticLabel
    "bwitch_moons_pack_80" -> strings.storeMoonPackCovenLabel
    else -> label
}


private fun SubscriptionStatus.toLocalizedLabel(strings: SettingsStrings): String = when (this) {
    SubscriptionStatus.Unknown -> strings.subscriptionStatusUnknown
    SubscriptionStatus.Inactive -> strings.subscriptionStatusInactive
    SubscriptionStatus.ActiveMonthly -> strings.subscriptionStatusActiveMonthly
    SubscriptionStatus.ActiveAnnual -> strings.subscriptionStatusActiveAnnual
}

private fun SubscriptionStatus.resolveStoreStatus(
    economyState: com.agc.bwitch.presentation.economy.EconomyUiState,
): SubscriptionStatus = if (economyState.hasUsableSnapshot && economyState.isPremium) {
    SubscriptionStatus.ActiveMonthly
} else {
    this
}

private fun SubscriptionStatus.toPrimaryAction(): SubscriptionPrimaryAction = when (this) {
    SubscriptionStatus.ActiveMonthly,
    SubscriptionStatus.ActiveAnnual -> SubscriptionPrimaryAction.Manage
    SubscriptionStatus.Unknown,
    SubscriptionStatus.Inactive -> SubscriptionPrimaryAction.Subscribe
}
