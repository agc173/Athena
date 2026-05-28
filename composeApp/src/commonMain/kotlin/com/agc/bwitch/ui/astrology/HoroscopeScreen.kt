package com.agc.bwitch.ui.astrology

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import bwitch.composeapp.generated.resources.Res
import bwitch.composeapp.generated.resources.zodiac_aquarius_art
import bwitch.composeapp.generated.resources.zodiac_aries_art
import bwitch.composeapp.generated.resources.zodiac_cancer_art
import bwitch.composeapp.generated.resources.zodiac_capricorn_art
import bwitch.composeapp.generated.resources.zodiac_gemini_art
import bwitch.composeapp.generated.resources.zodiac_leo_art
import bwitch.composeapp.generated.resources.zodiac_libra_art
import bwitch.composeapp.generated.resources.zodiac_pisces_art
import bwitch.composeapp.generated.resources.zodiac_sagittarius_art
import bwitch.composeapp.generated.resources.zodiac_scorpio_art
import bwitch.composeapp.generated.resources.zodiac_taurus_art
import bwitch.composeapp.generated.resources.zodiac_virgo_art
import com.agc.bwitch.domain.astrology.horoscope.ZodiacSign
import com.agc.bwitch.domain.astrology.horoscope.DailyHoroscope
import com.agc.bwitch.domain.astrology.horoscope.MonthlyHoroscope
import com.agc.bwitch.domain.astrology.horoscope.WeeklyHoroscope
import com.agc.bwitch.localization.AppStrings
import com.agc.bwitch.localization.appStrings
import com.agc.bwitch.domain.model.DeckCardUnlockReward
import com.agc.bwitch.presentation.astrology.horoscope.HoroscopeUiEffect
import com.agc.bwitch.platform.share.ShareResult
import com.agc.bwitch.platform.share.ShareTextPayload
import com.agc.bwitch.platform.share.rememberShareLauncher
import com.agc.bwitch.presentation.astrology.horoscope.HoroscopeFeedbackMessage
import com.agc.bwitch.presentation.astrology.horoscope.HoroscopeMonthPeriod
import com.agc.bwitch.presentation.astrology.horoscope.HoroscopeOverlayUi
import com.agc.bwitch.presentation.astrology.horoscope.HoroscopeTab
import com.agc.bwitch.presentation.astrology.horoscope.HoroscopeUiState
import com.agc.bwitch.presentation.astrology.horoscope.HoroscopeViewModel
import com.agc.bwitch.presentation.ads.RewardedAdResult
import com.agc.bwitch.presentation.ads.RewardedAdsService
import com.agc.bwitch.presentation.astrology.horoscope.HoroscopeWeekPeriod
import com.agc.bwitch.presentation.economy.EconomyViewModel
import com.agc.bwitch.presentation.economy.MoonUnlockFlowContext
import com.agc.bwitch.presentation.economy.UNLOCK_FLOW_ORIGIN_DIRECT_BALANCE
import com.agc.bwitch.presentation.economy.UNLOCK_FLOW_ORIGIN_PREMIUM
import com.agc.bwitch.ui.common.designsystem.BWitchPrimaryButton
import com.agc.bwitch.ui.common.designsystem.BWitchSecondaryButton
import com.agc.bwitch.ui.tarot.DeckCardUnlockRewardDialog
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject

@Composable
fun HoroscopeScreen(
    contentPadding: PaddingValues,
    preselectedSign: ZodiacSign? = null,
    onOpenCollection: () -> Unit = {},
    modifier: Modifier = Modifier,
    viewModel: HoroscopeViewModel = koinInject(),
    economyViewModel: EconomyViewModel = koinInject(),
    rewardedAdsService: RewardedAdsService = koinInject(),
) {
    val strings = appStrings
    val state by viewModel.uiState.collectAsState()
    val economyState by economyViewModel.uiState.collectAsState()
    val shareLauncher = rememberShareLauncher()
    val shareScope = rememberCoroutineScope()
    var shareErrorMessage by remember { mutableStateOf<String?>(null) }
    var rewardDialogRewards by remember { mutableStateOf<List<DeckCardUnlockReward>>(emptyList()) }
    var constellationRewardMessage by remember { mutableStateOf<String?>(null) }
    var isRewardedAdFlowRunning by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(preselectedSign) { preselectedSign?.let(viewModel::onSelectSign) }
    LaunchedEffect(economyState.hasUsableSnapshot, economyState.isPremium) {
        if (economyState.hasUsableSnapshot) {
            viewModel.onPremiumAccessChanged(economyState.isPremium)
        }
    }
    LaunchedEffect(viewModel) {
        viewModel.uiEffects.collect { effect ->
            when (effect) {
                is HoroscopeUiEffect.ShowDeckCardUnlockRewards -> rewardDialogRewards = effect.rewards
                is HoroscopeUiEffect.ConstellationProgressRewarded -> {
                    constellationRewardMessage = buildConstellationRewardMessage(effect)
                    delay(2200)
                    constellationRewardMessage = null
                }
            }
        }
    }
    Scaffold(modifier = modifier) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            HoroscopeScreenContent(
                modifier = Modifier.padding(innerPadding).padding(contentPadding),
                state = state,
                strings = strings,
                onSelectTab = viewModel::onSelectTab,
                onSelectDate = viewModel::onSelectDate,
                onSelectWeek = viewModel::onSelectWeek,
                onSelectMonth = viewModel::onSelectMonth,
                onOpenSign = viewModel::onOpenSign,
                canEarnMoonsWithRewardedAd = economyState.rewardedAdsRemaining > 0 && !isRewardedAdFlowRunning,
                onEarnMoonsWithRewardedAd = {
                if (isRewardedAdFlowRunning) return@HoroscopeScreenContent
                shareScope.launch {
                    isRewardedAdFlowRunning = true
                    try {
                        when (rewardedAdsService.showRewardedAd(placement = "horoscope_period_lock_overlay")) {
                            RewardedAdResult.Completed -> economyViewModel.claimRewardedAd("horoscope_period_lock_overlay")
                            RewardedAdResult.Cancelled,
                            is RewardedAdResult.Failed,
                            RewardedAdResult.Unavailable,
                            -> Unit
                        }
                    } catch (error: Throwable) {
                        println("[HoroscopeScreen] rewarded ad flow failed: ${error.message}")
                    } finally {
                        isRewardedAdFlowRunning = false
                    }
                }
                },
                onRewardedAdCtaShown = {
                economyViewModel.onRewardedAdCtaShown(
                    placement = "horoscope_period_lock_overlay",
                    rewardedAdsRemaining = economyState.rewardedAdsRemaining,
                )
                },
                onUnlock = {
                val hasEnoughBalance = economyState.hasUsableSnapshot && economyState.balance >= state.futureDayCost
                if (hasEnoughBalance) {
                    viewModel.onUnlockSelectedDay(
                        MoonUnlockFlowContext(
                            source = "horoscope_daily_unlock",
                            unlockFlowOrigin = if (economyState.isPremium) {
                                UNLOCK_FLOW_ORIGIN_PREMIUM
                            } else {
                                UNLOCK_FLOW_ORIGIN_DIRECT_BALANCE
                            },
                        ),
                    )
                } else {
                    viewModel.onUnlockDeferredToPaywall()
                    economyViewModel.requireLunas(
                        cost = state.futureDayCost,
                        source = "horoscope_daily_unlock",
                    ) { context -> viewModel.onUnlockSelectedDay(context) }
                }
                },
                onUnlockWeek = {
                val hasEnoughBalance = economyState.hasUsableSnapshot && economyState.balance >= state.weeklyCost
                if (hasEnoughBalance) {
                    viewModel.onUnlockSelectedWeek(
                        MoonUnlockFlowContext(
                            source = "horoscope_weekly_unlock",
                            unlockFlowOrigin = if (economyState.isPremium) {
                                UNLOCK_FLOW_ORIGIN_PREMIUM
                            } else {
                                UNLOCK_FLOW_ORIGIN_DIRECT_BALANCE
                            },
                        ),
                    )
                } else {
                    viewModel.onUnlockWeekDeferredToPaywall()
                    economyViewModel.requireLunas(
                        cost = state.weeklyCost,
                        source = "horoscope_weekly_unlock",
                    ) { context -> viewModel.onUnlockSelectedWeek(context) }
                }
                },
                onUnlockMonth = {
                val hasEnoughBalance = economyState.hasUsableSnapshot && economyState.balance >= state.monthlyCost
                if (hasEnoughBalance) {
                    viewModel.onUnlockSelectedMonth(
                        MoonUnlockFlowContext(
                            source = "horoscope_monthly_unlock",
                            unlockFlowOrigin = if (economyState.isPremium) {
                                UNLOCK_FLOW_ORIGIN_PREMIUM
                            } else {
                                UNLOCK_FLOW_ORIGIN_DIRECT_BALANCE
                            },
                        ),
                    )
                } else {
                    viewModel.onUnlockMonthDeferredToPaywall()
                    economyViewModel.requireLunas(
                        cost = state.monthlyCost,
                        source = "horoscope_monthly_unlock",
                    ) { context -> viewModel.onUnlockSelectedMonth(context) }
                }
                },
                onCloseOverlay = viewModel::onCloseOverlay,
                onOverlaySignChanged = viewModel::onOverlaySignChanged,
                onShareOverlay = { overlay ->
                shareErrorMessage = null
                val shareText = overlay.toShareText(strings)
                if (shareText.isBlank()) return@HoroscopeScreenContent
                shareScope.launch {
                    val result = shareLauncher.shareText(
                        ShareTextPayload(
                            text = shareText,
                            title = strings.horoscope.shareCta,
                        ),
                    )
                    if (result is ShareResult.Error) {
                        shareErrorMessage = result.message ?: strings.birthChart.shareFailedFallback
                    }
                }
                },
                shareErrorMessage = shareErrorMessage,
            )
            ConstellationRewardOverlay(
                message = constellationRewardMessage,
                modifier = Modifier.align(Alignment.TopCenter).padding(top = 22.dp),
            )
        }
    }
    if (rewardDialogRewards.isNotEmpty()) {
        DeckCardUnlockRewardDialog(
            strings = strings,
            rewards = rewardDialogRewards,
            onDismiss = { rewardDialogRewards = emptyList() },
            onOpenCollection = {
                rewardDialogRewards = emptyList()
                onOpenCollection()
            },
        )
    }
}

@Composable
private fun ConstellationRewardOverlay(
    message: String?,
    modifier: Modifier = Modifier,
) {
    if (message.isNullOrBlank()) return
    val pulse = rememberInfiniteTransition(label = "constellation_reward_pulse")
    val scale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 550, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "constellation_reward_scale",
    )
    Surface(
        modifier = modifier
            .padding(horizontal = 16.dp)
            .scale(scale),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.92f),
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
            textAlign = TextAlign.Center,
        )
    }
}

private fun buildConstellationRewardMessage(
    effect: HoroscopeUiEffect.ConstellationProgressRewarded,
): String {
    val base = "Una estrella se ha despertado"
    val signPart = effect.sign?.name?.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
    return if (signPart.isNullOrBlank()) base else "$base · $signPart avanza"
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HoroscopeScreenContent(
    modifier: Modifier,
    state: HoroscopeUiState,
    strings: AppStrings,
    onSelectTab: (HoroscopeTab) -> Unit,
    onSelectDate: (String) -> Unit,
    onSelectWeek: (HoroscopeWeekPeriod) -> Unit,
    onSelectMonth: (HoroscopeMonthPeriod) -> Unit,
    onOpenSign: (ZodiacSign) -> Unit,
    canEarnMoonsWithRewardedAd: Boolean,
    onEarnMoonsWithRewardedAd: () -> Unit,
    onRewardedAdCtaShown: () -> Unit,
    onUnlock: () -> Unit,
    onUnlockWeek: () -> Unit,
    onUnlockMonth: () -> Unit,
    onCloseOverlay: () -> Unit,
    onOverlaySignChanged: (ZodiacSign) -> Unit,
    onShareOverlay: (HoroscopeOverlayUi) -> Unit,
    shareErrorMessage: String?,
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        val tabs = HoroscopeTab.values()
        TabRow(selectedTabIndex = tabs.indexOf(state.selectedTab).coerceAtLeast(0)) {
            tabs.forEach { tab ->
                Tab(
                    selected = state.selectedTab == tab,
                    onClick = { onSelectTab(tab) },
                    text = {
                        Text(
                            when (tab) {
                                HoroscopeTab.Daily -> strings.horoscope.dailyTab
                                HoroscopeTab.Weekly -> strings.horoscope.weeklyTab
                                HoroscopeTab.Monthly -> strings.horoscope.monthlyTab
                            }
                        )
                    },
                    enabled = true,
                )
            }
        }

        when (state.selectedTab) {
            HoroscopeTab.Daily -> PeriodTabSelector(
                options = state.days.map { day ->
                    SelectorOption(
                        id = day.dateIso,
                        label = if (day.isToday) strings.horoscope.todayLabel else day.shortLabel,
                    )
                },
                selectedId = state.selectedDateIso,
                onSelect = onSelectDate,
            )

            HoroscopeTab.Weekly -> {
                PeriodTabSelector(
                    options = listOf(
                        SelectorOption(HoroscopeWeekPeriod.ThisWeek.name, strings.horoscope.thisWeek),
                        SelectorOption(HoroscopeWeekPeriod.NextWeek.name, strings.horoscope.nextWeek),
                    ),
                    selectedId = state.selectedWeek.name,
                    onSelect = { selected ->
                        onSelectWeek(
                            if (selected == HoroscopeWeekPeriod.ThisWeek.name) {
                                HoroscopeWeekPeriod.ThisWeek
                            } else {
                                HoroscopeWeekPeriod.NextWeek
                            }
                        )
                    },
                )
            }

            HoroscopeTab.Monthly -> {
                val monthLanguageCode = DEFAULT_MONTH_LANGUAGE_CODE
                PeriodTabSelector(
                    options = listOf(
                        SelectorOption(HoroscopeMonthPeriod.ThisMonth.name, monthNameFromKey(state.currentMonthKey, monthLanguageCode)),
                        SelectorOption(HoroscopeMonthPeriod.NextMonth.name, monthNameFromKey(state.nextMonthKey, monthLanguageCode)),
                    ),
                    selectedId = state.selectedMonth.name,
                    onSelect = { selected ->
                        onSelectMonth(
                            if (selected == HoroscopeMonthPeriod.ThisMonth.name) {
                                HoroscopeMonthPeriod.ThisMonth
                            } else {
                                HoroscopeMonthPeriod.NextMonth
                            }
                        )
                    },
                )
            }
        }

        val dailyLocked = state.selectedTab == HoroscopeTab.Daily &&
            state.days.firstOrNull { it.dateIso == state.selectedDateIso }?.isLocked == true
        val weeklyLocked = state.selectedTab == HoroscopeTab.Weekly && state.isWeekLocked
        val monthlyLocked = state.selectedTab == HoroscopeTab.Monthly && state.isMonthLocked
        val periodLocked = dailyLocked || weeklyLocked || monthlyLocked
        val unlockedButMissingContent = state.selectedTab != HoroscopeTab.Daily &&
            !periodLocked &&
            !state.isContentAvailable

        Box(
            modifier = Modifier.fillMaxWidth(),
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(if (periodLocked || unlockedButMissingContent) 0.35f else 1f),
            ) {
                val spacing = 10.dp
                val columns = if (maxWidth < 320.dp) 2 else 3
                val cardSize = ((maxWidth - spacing * (columns - 1)) / columns).coerceIn(88.dp, 112.dp)

                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(spacing),
                    verticalArrangement = Arrangement.spacedBy(spacing),
                    maxItemsInEachRow = columns,
                ) {
                    ZodiacSign.values().forEach { sign ->
                        ZodiacSignCard(
                            sign = sign,
                            strings = strings,
                            enabled = !periodLocked && !unlockedButMissingContent,
                            onClick = { if (!periodLocked && !unlockedButMissingContent) onOpenSign(sign) },
                            modifier = Modifier.size(cardSize),
                        )
                    }
                }
            }

            if (periodLocked) {
                PeriodLockOverlayCard(
                    modifier = Modifier.align(Alignment.Center),
                    strings = strings,
                    tab = state.selectedTab,
                    cost = when (state.selectedTab) {
                        HoroscopeTab.Daily -> state.futureDayCost
                        HoroscopeTab.Weekly -> state.weeklyCost
                        HoroscopeTab.Monthly -> state.monthlyCost
                    },
                    onUnlock = when (state.selectedTab) {
                        HoroscopeTab.Daily -> onUnlock
                        HoroscopeTab.Weekly -> onUnlockWeek
                        HoroscopeTab.Monthly -> onUnlockMonth
                    },
                    canEarnMoonsWithRewardedAd = canEarnMoonsWithRewardedAd,
                    onEarnMoonsWithRewardedAd = onEarnMoonsWithRewardedAd,
                    onRewardedAdCtaShown = onRewardedAdCtaShown,
                    isLoading = state.isUnlocking,
                    canUnlock = state.selectedTab == HoroscopeTab.Daily || state.isContentAvailable,
                    errorMessage = state.lockCardMessage?.toLocalizedMessage(strings),
                )
            } else if (unlockedButMissingContent) {
                PreparingContentCard(
                    modifier = Modifier.align(Alignment.Center),
                    strings = strings,
                    isLoading = state.isCheckingContentAvailability,
                )
            }
        }

    }

    state.overlay?.let { overlay ->
        HoroscopeOverlayDialog(
            overlay = overlay,
            state = state,
            strings = strings,
            onCloseOverlay = onCloseOverlay,
            onUnlock = onUnlock,
            onOverlaySignChanged = onOverlaySignChanged,
            onShareOverlay = onShareOverlay,
        )
    }

    shareErrorMessage?.let {
        Text(
            text = it,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
        )
    }
}

@Composable
private fun HoroscopeOverlayDialog(
    overlay: HoroscopeOverlayUi,
    state: HoroscopeUiState,
    strings: AppStrings,
    onCloseOverlay: () -> Unit,
    onUnlock: () -> Unit,
    onOverlaySignChanged: (ZodiacSign) -> Unit,
    onShareOverlay: (HoroscopeOverlayUi) -> Unit,
) {
    if (overlay is HoroscopeOverlayUi.DailyOverlay && overlay.isLocked) {
        LockedDailyHoroscopeDialog(
            overlay = overlay,
            strings = strings,
            isUnlocking = state.isUnlocking,
            unlockCost = state.futureDayCost,
            onCloseOverlay = onCloseOverlay,
            onUnlock = onUnlock,
        )
    } else {
        HoroscopeContentDialog(
            overlay = overlay,
            strings = strings,
            onCloseOverlay = onCloseOverlay,
            onOverlaySignChanged = onOverlaySignChanged,
            onShareOverlay = onShareOverlay,
        )
    }
}

@Composable
private fun LockedDailyHoroscopeDialog(
    overlay: HoroscopeOverlayUi.DailyOverlay,
    strings: AppStrings,
    isUnlocking: Boolean,
    unlockCost: Int,
    onCloseOverlay: () -> Unit,
    onUnlock: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCloseOverlay,
        title = { Text(strings.horoscope.lockedTitle) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("${overlay.sign.localizedLabel(strings)} · ${overlay.dateIso}\n${strings.horoscope.unlockMessage}")
                overlay.unlockErrorMessage?.let { unlockError ->
                    Text(
                        text = unlockError.toLocalizedMessage(strings),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        },
        confirmButton = {
            BWitchPrimaryButton(onClick = onUnlock, enabled = !isUnlocking) {
                Text(strings.horoscope.unlockForMoonFormat.replaceFirst("%d", "$unlockCost"))
            }
        },
        dismissButton = { BWitchSecondaryButton(onClick = onCloseOverlay) { Text(strings.horoscope.closeCta) } },
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HoroscopeContentDialog(
    overlay: HoroscopeOverlayUi,
    strings: AppStrings,
    onCloseOverlay: () -> Unit,
    onOverlaySignChanged: (ZodiacSign) -> Unit,
    onShareOverlay: (HoroscopeOverlayUi) -> Unit,
) {
    val signs = remember { ZodiacSign.values().toList() }
    val initialPage = signs.indexOf(overlay.sign).coerceAtLeast(0)
    val pagerState = rememberPagerState(initialPage = initialPage) { signs.size }
    val visibleSign = signs.getOrNull(pagerState.currentPage) ?: overlay.sign
    val visibleOverlay = overlay.forVisibleSign(visibleSign)

    LaunchedEffect(pagerState, signs) {
        snapshotFlow { pagerState.currentPage }
            .distinctUntilChanged()
            .collect { page ->
                signs.getOrNull(page)?.let(onOverlaySignChanged)
            }
    }

    LaunchedEffect(overlay.sign, signs) {
        val targetPage = signs.indexOf(overlay.sign)
        if (targetPage >= 0 && pagerState.currentPage != targetPage) {
            pagerState.scrollToPage(targetPage)
        }
    }

    BasicAlertDialog(
        onDismissRequest = onCloseOverlay,
        modifier = Modifier
            .fillMaxWidth(0.97f)
            .widthIn(max = 720.dp),
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 6.dp,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                HoroscopeOverlayHeader(
                    sign = visibleSign,
                    periodLabel = overlay.periodLabel,
                    strings = strings,
                    canShare = !visibleOverlay.isLoading && visibleOverlay.toShareText(strings).isNotBlank(),
                    onShare = { onShareOverlay(visibleOverlay) },
                )
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(520.dp),
                ) { page ->
                    val pageOverlay = overlay.forVisibleSign(signs[page])
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(520.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        HoroscopeOverlayBody(
                            overlay = pageOverlay,
                            strings = strings,
                        )
                    }
                }
            }
        }
    }
}

private fun HoroscopeOverlayUi.forVisibleSign(sign: ZodiacSign): HoroscopeOverlayUi {
    if (this.sign == sign) return this
    return when (this) {
        is HoroscopeOverlayUi.DailyOverlay -> copy(sign = sign, isLoading = true, horoscope = null)
        is HoroscopeOverlayUi.WeeklyOverlay -> copy(sign = sign, isLoading = true, horoscope = null)
        is HoroscopeOverlayUi.MonthlyOverlay -> copy(sign = sign, isLoading = true, horoscope = null)
    }
}

@Composable
private fun HoroscopeOverlayHeader(
    sign: ZodiacSign,
    periodLabel: String,
    strings: AppStrings,
    canShare: Boolean,
    onShare: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(6.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Image(
                    painter = painterResource(sign.artResource()),
                    contentDescription = sign.localizedLabel(strings),
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(48.dp),
                )
                Text(
                    text = sign.localizedLabel(strings),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            Text(
                text = periodLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        TextButton(
            onClick = onShare,
            enabled = canShare,
        ) {
            Text(strings.horoscope.shareCta)
        }
    }
}

private fun HoroscopeOverlayUi.toShareText(strings: AppStrings): String = when (this) {
    is HoroscopeOverlayUi.DailyOverlay -> horoscope?.toShareText(strings, periodLabel)
    is HoroscopeOverlayUi.WeeklyOverlay -> horoscope?.toShareText(strings, periodLabel)
    is HoroscopeOverlayUi.MonthlyOverlay -> horoscope?.toShareText(strings, periodLabel)
}.orEmpty()

private fun DailyHoroscope.toShareText(strings: AppStrings, periodLabel: String): String {
    if (text.isBlank() && shareText.isNullOrBlank()) return ""
    val sections = mutableListOf<String>()
    sections.add(sign.localizedLabel(strings))
    sections.add(periodLabel)
    shareText?.takeIf { it.isNotBlank() }?.let(sections::add)
    val meta = buildList {
        mood.takeIf { it.isNotBlank() }?.let { add("${strings.horoscope.moodLabel}: $it") }
        luckyNumber.takeIf { it > 0 }?.let { add("${strings.horoscope.luckyNumberLabel}: $it") }
        luckyColor.takeIf { it.isNotBlank() }?.let { add("${strings.horoscope.luckyColorLabel}: $it") }
    }
    if (meta.isNotEmpty()) sections.add(meta.joinToString("\n"))
    text.takeIf { it.isNotBlank() }?.let(sections::add)
    return sections.joinToString("\n\n")
}

private fun WeeklyHoroscope.toShareText(strings: AppStrings, periodLabel: String): String {
    val sections = mutableListOf(sign.localizedLabel(strings), periodLabel)
    shareText?.takeIf { it.isNotBlank() }?.let(sections::add)
    title.takeIf { it.isNotBlank() }?.let(sections::add)
    sections.add("${strings.horoscope.weekOverviewTitle}\n$overview")
    sections.add("${strings.horoscope.loveAndRelationshipsTitle}\n$loveAndRelationships")
    sections.add("${strings.horoscope.workAndMoneyTitle}\n$workAndMoney")
    sections.add("${strings.horoscope.spiritualEnergyTitle}\n$spiritualEnergy")
    sections.add("${strings.horoscope.weeklyAdviceTitle}\n$weeklyAdvice")
    sections.add("${strings.horoscope.mantraTitle}\n$mantra")
    return sections.joinToString("\n\n")
}

private fun MonthlyHoroscope.toShareText(strings: AppStrings, periodLabel: String): String {
    val sections = mutableListOf(sign.localizedLabel(strings), periodLabel)
    shareText?.takeIf { it.isNotBlank() }?.let(sections::add)
    title.takeIf { it.isNotBlank() }?.let(sections::add)
    sections.add("${strings.horoscope.monthThemeTitle}\n$monthTheme")
    sections.add("${strings.horoscope.loveAndRelationshipsTitle}\n$loveAndRelationships")
    sections.add("${strings.horoscope.workAndMoneyTitle}\n$workAndMoney")
    sections.add("${strings.horoscope.personalGrowthTitle}\n$personalGrowth")
    sections.add("${strings.horoscope.ritualSuggestionTitle}\n$ritualSuggestion")
    sections.add("${strings.horoscope.mantraTitle}\n$mantra")
    return sections.joinToString("\n\n")
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HoroscopeOverlayBody(
    overlay: HoroscopeOverlayUi,
    strings: AppStrings,
) {
    if (overlay.isLoading) {
        Text(strings.horoscope.loading)
    } else {
        when (overlay) {
            is HoroscopeOverlayUi.DailyOverlay -> {
                val horoscope = overlay.horoscope
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    HoroscopeMetaChip(label = strings.horoscope.moodLabel, value = horoscope?.mood ?: "-")
                    HoroscopeMetaChip(label = strings.horoscope.luckyNumberLabel, value = horoscope?.luckyNumber?.toString() ?: "-")
                    HoroscopeMetaChip(label = strings.horoscope.luckyColorLabel, value = horoscope?.luckyColor ?: "-")
                }
                Text(
                    text = horoscope?.text ?: strings.horoscope.noContentYet,
                    style = MaterialTheme.typography.bodyLarge,
                    lineHeight = MaterialTheme.typography.bodyLarge.lineHeight * 1.2f,
                )
            }
            is HoroscopeOverlayUi.WeeklyOverlay -> {
                val horoscope = overlay.horoscope
                Text(
                    text = horoscope?.title ?: strings.horoscope.noContentYet,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                OverlaySection(strings.horoscope.weekOverviewTitle, horoscope?.overview)
                OverlaySection(strings.horoscope.loveAndRelationshipsTitle, horoscope?.loveAndRelationships)
                OverlaySection(strings.horoscope.workAndMoneyTitle, horoscope?.workAndMoney)
                OverlaySection(strings.horoscope.spiritualEnergyTitle, horoscope?.spiritualEnergy)
                OverlaySection(strings.horoscope.weeklyAdviceTitle, horoscope?.weeklyAdvice)
                OverlaySection(strings.horoscope.mantraTitle, horoscope?.mantra)
            }
            is HoroscopeOverlayUi.MonthlyOverlay -> {
                val horoscope = overlay.horoscope
                Text(
                    text = horoscope?.title ?: strings.horoscope.noContentYet,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                )
                OverlaySection(strings.horoscope.monthThemeTitle, horoscope?.monthTheme)
                OverlaySection(strings.horoscope.loveAndRelationshipsTitle, horoscope?.loveAndRelationships)
                OverlaySection(strings.horoscope.workAndMoneyTitle, horoscope?.workAndMoney)
                OverlaySection(strings.horoscope.personalGrowthTitle, horoscope?.personalGrowth)
                OverlaySection(strings.horoscope.ritualSuggestionTitle, horoscope?.ritualSuggestion)
                OverlaySection(strings.horoscope.mantraTitle, horoscope?.mantra)
            }
        }
    }
}

@Composable
private fun OverlaySection(
    label: String,
    content: String?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        Text(
            text = content ?: "-",
            style = MaterialTheme.typography.bodyMedium,
        )
    }
}

@Composable
private fun HoroscopeMetaChip(
    label: String,
    value: String,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
        }
    }
}

@Composable
private fun PreparingContentCard(
    modifier: Modifier = Modifier,
    strings: AppStrings,
    isLoading: Boolean,
) {
    Card(modifier = modifier.fillMaxWidth(0.9f)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                text = strings.horoscope.loading,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = strings.horoscope.noContentYet,
                style = MaterialTheme.typography.bodySmall,
            )
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.width(20.dp))
            }
        }
    }
}

@Composable
private fun ZodiacSignCard(
    sign: ZodiacSign,
    strings: AppStrings,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = sign.localizedLabel(strings)
    Column(
        modifier = modifier
            .aspectRatio(1f)
            .clickable(
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .semantics {
                role = Role.Button
                contentDescription = label
            }
            .alpha(if (enabled) 1f else 0.42f)
            .padding(2.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(0.94f),
            contentAlignment = Alignment.Center,
        ) {
            Image(
                painter = painterResource(sign.artResource()),
                contentDescription = label,
                contentScale = ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Text(
            text = label,
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun PeriodLockOverlayCard(
    modifier: Modifier = Modifier,
    strings: AppStrings,
    tab: HoroscopeTab,
    cost: Int,
    onUnlock: () -> Unit,
    canEarnMoonsWithRewardedAd: Boolean,
    onEarnMoonsWithRewardedAd: () -> Unit,
    onRewardedAdCtaShown: () -> Unit,
    isLoading: Boolean,
    canUnlock: Boolean,
    errorMessage: String?,
) {
    var rewardedCtaTracked by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(canEarnMoonsWithRewardedAd, isLoading) {
        val ctaVisible = canEarnMoonsWithRewardedAd && !isLoading
        when {
            ctaVisible && !rewardedCtaTracked -> {
                onRewardedAdCtaShown()
                rewardedCtaTracked = true
            }
            !ctaVisible -> rewardedCtaTracked = false
        }
    }
    val cta = when (tab) {
        HoroscopeTab.Daily -> strings.horoscope.unlockDayForMoonFormat
        HoroscopeTab.Weekly -> strings.horoscope.unlockWeekForMoonFormat
        HoroscopeTab.Monthly -> strings.horoscope.unlockMonthForMoonFormat
    }
    Card(modifier = modifier.fillMaxWidth(0.9f)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(strings.horoscope.unlockForMoonFormat.replaceFirst("%d", "$cost"), fontWeight = FontWeight.SemiBold)
            Text(strings.horoscope.unlockPeriodScopeHint, style = MaterialTheme.typography.bodySmall)
            errorMessage?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            BWitchPrimaryButton(onClick = onUnlock, enabled = !isLoading && canUnlock) {
                Text(cta.replaceFirst("%d", "$cost"))
            }
            if (canEarnMoonsWithRewardedAd) {
                BWitchSecondaryButton(onClick = onEarnMoonsWithRewardedAd, enabled = !isLoading) {
                    Text(strings.profile.moonPaywallWatchAdCta)
                }
            }
        }
    }
}

@Composable
private fun PeriodTabSelector(
    options: List<SelectorOption>,
    selectedId: String,
    onSelect: (String) -> Unit,
) {
    if (options.isEmpty()) return
    val selectedIndex = options.indexOfFirst { it.id == selectedId }.let { index ->
        if (index >= 0) index else 0
    }
    ScrollableTabRow(
        selectedTabIndex = selectedIndex,
        edgePadding = 0.dp,
    ) {
        options.forEach { option ->
            Tab(
                selected = option.id == selectedId,
                onClick = { onSelect(option.id) },
                modifier = Modifier.widthIn(min = 72.dp),
                text = { Text(option.label) },
            )
        }
    }
}

private data class SelectorOption(
    val id: String,
    val label: String,
)

private const val DEFAULT_MONTH_LANGUAGE_CODE = "es"

private fun String.toMonthNumberOrNull(): Int? =
    split("-").getOrNull(1)?.toIntOrNull()?.takeIf { it in 1..12 }

private fun monthNameFromKey(monthKey: String, languageCode: String): String {
    val month = monthKey.toMonthNumberOrNull() ?: return monthKey
    return when (languageCode) {
        "es" -> monthNameSpanish(month)
        "pt" -> monthNamePortuguese(month)
        "ru" -> monthNameRussian(month)
        "fr" -> monthNameFrench(month)
        "it" -> monthNameItalian(month)
        "de" -> monthNameGerman(month)
        else -> monthNameEnglish(month)
    }
}

private fun monthNameEnglish(month: Int): String = when (month) {
    1 -> "January"
    2 -> "February"
    3 -> "March"
    4 -> "April"
    5 -> "May"
    6 -> "June"
    7 -> "July"
    8 -> "August"
    9 -> "September"
    10 -> "October"
    11 -> "November"
    else -> "December"
}

private fun monthNameSpanish(month: Int): String = when (month) {
    1 -> "Enero"
    2 -> "Febrero"
    3 -> "Marzo"
    4 -> "Abril"
    5 -> "Mayo"
    6 -> "Junio"
    7 -> "Julio"
    8 -> "Agosto"
    9 -> "Septiembre"
    10 -> "Octubre"
    11 -> "Noviembre"
    else -> "Diciembre"
}

private fun monthNamePortuguese(month: Int): String = when (month) {
    1 -> "Janeiro"
    2 -> "Fevereiro"
    3 -> "Março"
    4 -> "Abril"
    5 -> "Maio"
    6 -> "Junho"
    7 -> "Julho"
    8 -> "Agosto"
    9 -> "Setembro"
    10 -> "Outubro"
    11 -> "Novembro"
    else -> "Dezembro"
}

private fun monthNameRussian(month: Int): String = when (month) {
    1 -> "Январь"
    2 -> "Февраль"
    3 -> "Март"
    4 -> "Апрель"
    5 -> "Май"
    6 -> "Июнь"
    7 -> "Июль"
    8 -> "Август"
    9 -> "Сентябрь"
    10 -> "Октябрь"
    11 -> "Ноябрь"
    else -> "Декабрь"
}

private fun monthNameFrench(month: Int): String = when (month) {
    1 -> "Janvier"
    2 -> "Février"
    3 -> "Mars"
    4 -> "Avril"
    5 -> "Mai"
    6 -> "Juin"
    7 -> "Juillet"
    8 -> "Août"
    9 -> "Septembre"
    10 -> "Octobre"
    11 -> "Novembre"
    else -> "Décembre"
}

private fun monthNameItalian(month: Int): String = when (month) {
    1 -> "Gennaio"
    2 -> "Febbraio"
    3 -> "Marzo"
    4 -> "Aprile"
    5 -> "Maggio"
    6 -> "Giugno"
    7 -> "Luglio"
    8 -> "Agosto"
    9 -> "Settembre"
    10 -> "Ottobre"
    11 -> "Novembre"
    else -> "Dicembre"
}

private fun monthNameGerman(month: Int): String = when (month) {
    1 -> "Januar"
    2 -> "Februar"
    3 -> "März"
    4 -> "April"
    5 -> "Mai"
    6 -> "Juni"
    7 -> "Juli"
    8 -> "August"
    9 -> "September"
    10 -> "Oktober"
    11 -> "November"
    else -> "Dezember"
}

private fun ZodiacSign.artResource(): DrawableResource = when (this) {
    ZodiacSign.aries -> Res.drawable.zodiac_aries_art
    ZodiacSign.taurus -> Res.drawable.zodiac_taurus_art
    ZodiacSign.gemini -> Res.drawable.zodiac_gemini_art
    ZodiacSign.cancer -> Res.drawable.zodiac_cancer_art
    ZodiacSign.leo -> Res.drawable.zodiac_leo_art
    ZodiacSign.virgo -> Res.drawable.zodiac_virgo_art
    ZodiacSign.libra -> Res.drawable.zodiac_libra_art
    ZodiacSign.scorpio -> Res.drawable.zodiac_scorpio_art
    ZodiacSign.sagittarius -> Res.drawable.zodiac_sagittarius_art
    ZodiacSign.capricorn -> Res.drawable.zodiac_capricorn_art
    ZodiacSign.aquarius -> Res.drawable.zodiac_aquarius_art
    ZodiacSign.pisces -> Res.drawable.zodiac_pisces_art
}

private fun ZodiacSign.localizedLabel(strings: AppStrings): String = when (this) {
    ZodiacSign.aries -> strings.zodiac.aries
    ZodiacSign.taurus -> strings.zodiac.taurus
    ZodiacSign.gemini -> strings.zodiac.gemini
    ZodiacSign.cancer -> strings.zodiac.cancer
    ZodiacSign.leo -> strings.zodiac.leo
    ZodiacSign.virgo -> strings.zodiac.virgo
    ZodiacSign.libra -> strings.zodiac.libra
    ZodiacSign.scorpio -> strings.zodiac.scorpio
    ZodiacSign.sagittarius -> strings.zodiac.sagittarius
    ZodiacSign.capricorn -> strings.zodiac.capricorn
    ZodiacSign.aquarius -> strings.zodiac.aquarius
    ZodiacSign.pisces -> strings.zodiac.pisces
}

private fun HoroscopeFeedbackMessage.toLocalizedMessage(strings: AppStrings): String = when (this) {
    HoroscopeFeedbackMessage.AlreadyUpdated -> strings.horoscope.alreadyUpdatedMessage
    HoroscopeFeedbackMessage.Updated -> strings.horoscope.updatedMessage
    HoroscopeFeedbackMessage.RefreshFailed -> strings.horoscope.refreshErrorMessage
    HoroscopeFeedbackMessage.ComingSoon -> strings.horoscope.comingSoon
    HoroscopeFeedbackMessage.UnlockFailed -> strings.horoscope.unlockError
    HoroscopeFeedbackMessage.UnlockInsufficientMoons -> strings.horoscope.unlockInsufficient
    HoroscopeFeedbackMessage.UnlockSuccess -> strings.horoscope.unlockSuccess
    HoroscopeFeedbackMessage.UnlockWeekFailed -> strings.horoscope.unlockError
    HoroscopeFeedbackMessage.UnlockMonthFailed -> strings.horoscope.unlockError
    HoroscopeFeedbackMessage.ContentInPreparation -> strings.horoscope.loading
}
