package com.agc.bwitch.ui.astrology

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.agc.bwitch.domain.astrology.horoscope.ZodiacSign
import com.agc.bwitch.localization.AppStrings
import com.agc.bwitch.localization.appStrings
import com.agc.bwitch.presentation.astrology.horoscope.HoroscopeFeedbackMessage
import com.agc.bwitch.presentation.astrology.horoscope.HoroscopeMonthPeriod
import com.agc.bwitch.presentation.astrology.horoscope.HoroscopeTab
import com.agc.bwitch.presentation.astrology.horoscope.HoroscopeUiState
import com.agc.bwitch.presentation.astrology.horoscope.HoroscopeViewModel
import com.agc.bwitch.presentation.astrology.horoscope.HoroscopeWeekPeriod
import com.agc.bwitch.presentation.economy.EconomyViewModel
import com.agc.bwitch.ui.common.designsystem.BWitchPrimaryButton
import com.agc.bwitch.ui.common.designsystem.BWitchSecondaryButton
import org.koin.compose.koinInject

@Composable
fun HoroscopeScreen(
    contentPadding: PaddingValues,
    preselectedSign: ZodiacSign? = null,
    modifier: Modifier = Modifier,
    viewModel: HoroscopeViewModel = koinInject(),
    economyViewModel: EconomyViewModel = koinInject(),
) {
    val strings = appStrings
    val state by viewModel.uiState.collectAsState()
    val economyState by economyViewModel.uiState.collectAsState()

    LaunchedEffect(preselectedSign) { preselectedSign?.let(viewModel::onSelectSign) }
    LaunchedEffect(economyState.isPremium) { viewModel.onPremiumAccessChanged(economyState.isPremium) }
    Scaffold(modifier = modifier) { innerPadding ->
        HoroscopeScreenContent(
            modifier = Modifier.padding(innerPadding).padding(contentPadding),
            state = state,
            strings = strings,
            onSelectTab = viewModel::onSelectTab,
            onSelectDate = viewModel::onSelectDate,
            onSelectWeek = viewModel::onSelectWeek,
            onSelectMonth = viewModel::onSelectMonth,
            onOpenSign = viewModel::onOpenSign,
            canEarnMoonsWithRewardedAd = economyState.rewardedAdsRemaining > 0,
            onEarnMoonsWithRewardedAd = { economyViewModel.claimRewardedAd("horoscope_period_lock_overlay") },
            onUnlock = {
                val hasEnoughBalance = economyState.hasUsableSnapshot && economyState.balance >= state.futureDayCost
                if (hasEnoughBalance) {
                    viewModel.onUnlockSelectedDay()
                } else {
                    viewModel.onUnlockDeferredToPaywall()
                    economyViewModel.requireLunas(
                        cost = state.futureDayCost,
                        source = "horoscope_daily_unlock",
                    ) { viewModel.onUnlockSelectedDay() }
                }
            },
            onUnlockWeek = {
                val hasEnoughBalance = economyState.hasUsableSnapshot && economyState.balance >= state.weeklyCost
                if (hasEnoughBalance) {
                    viewModel.onUnlockSelectedWeek()
                } else {
                    viewModel.onUnlockWeekDeferredToPaywall()
                    economyViewModel.requireLunas(
                        cost = state.weeklyCost,
                        source = "horoscope_weekly_unlock",
                    ) { viewModel.onUnlockSelectedWeek() }
                }
            },
            onUnlockMonth = {
                val hasEnoughBalance = economyState.hasUsableSnapshot && economyState.balance >= state.monthlyCost
                if (hasEnoughBalance) {
                    viewModel.onUnlockSelectedMonth()
                } else {
                    viewModel.onUnlockMonthDeferredToPaywall()
                    economyViewModel.requireLunas(
                        cost = state.monthlyCost,
                        source = "horoscope_monthly_unlock",
                    ) { viewModel.onUnlockSelectedMonth() }
                }
            },
            onCloseOverlay = viewModel::onCloseOverlay,
        )
    }
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
    onUnlock: () -> Unit,
    onUnlockWeek: () -> Unit,
    onUnlockMonth: () -> Unit,
    onCloseOverlay: () -> Unit,
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

        Box(
            modifier = Modifier.fillMaxWidth(),
        ) {
            FlowRow(
                modifier = Modifier.alpha(if (periodLocked) 0.35f else 1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                maxItemsInEachRow = 3,
            ) {
                ZodiacSign.values().forEach { sign ->
                    ZodiacSignCard(
                        sign = sign,
                        strings = strings,
                        enabled = !periodLocked,
                        onClick = { if (!periodLocked) onOpenSign(sign) },
                    )
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
                    isLoading = state.isUnlocking,
                    errorMessage = state.lockCardMessage?.toLocalizedMessage(strings),
                )
            }
        }

    }

    val overlay = state.overlay
    if (overlay != null) {
        if (overlay.isLocked) {
            AlertDialog(
                onDismissRequest = onCloseOverlay,
                title = { Text(strings.horoscope.lockedTitle) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text("${overlay.sign.symbol()} ${overlay.sign.localizedLabel(strings)} · ${overlay.dateIso}\n${strings.horoscope.unlockMessage}")
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
                    BWitchPrimaryButton(onClick = onUnlock, enabled = !state.isUnlocking) {
                        Text(strings.horoscope.unlockForMoonFormat.replaceFirst("%d", "${state.futureDayCost}"))
                    }
                },
                dismissButton = { BWitchSecondaryButton(onClick = onCloseOverlay) { Text(strings.horoscope.closeCta) } },
            )
        } else {
            AlertDialog(
                onDismissRequest = onCloseOverlay,
                title = { Text("${overlay.sign.symbol()} ${overlay.sign.localizedLabel(strings)} · ${overlay.dateIso}") },
                text = {
                    val horoscope = overlay.horoscope
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (overlay.isLoading) {
                            Text(strings.horoscope.loading)
                        } else {
                            Text("${strings.horoscope.moodLabel}: ${horoscope?.mood ?: "-"}", style = MaterialTheme.typography.titleSmall)
                            Text("${strings.horoscope.luckyNumberLabel}: ${horoscope?.luckyNumber ?: "-"}", style = MaterialTheme.typography.titleSmall)
                            Text("${strings.horoscope.luckyColorLabel}: ${horoscope?.luckyColor ?: "-"}", style = MaterialTheme.typography.titleSmall)
                            Text(horoscope?.text ?: strings.horoscope.noContentYet, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                },
                confirmButton = { BWitchSecondaryButton(onClick = {}, enabled = false) { Text(strings.horoscope.shareCta) } },
                dismissButton = { BWitchPrimaryButton(onClick = onCloseOverlay) { Text(strings.horoscope.closeCta) } },
            )
        }
    }
}

@Composable
private fun ZodiacSignCard(
    sign: ZodiacSign,
    strings: AppStrings,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Card(
        onClick = onClick,
        modifier = Modifier
            .width(108.dp)
            .aspectRatio(1f)
            .semantics {
                role = Role.Button
                contentDescription = sign.localizedLabel(strings)
            },
        colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant.copy(alpha = if (enabled) 0.55f else 0.22f)),
        border = BorderStroke(1.dp, colors.outlineVariant),
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(10.dp)) {
            Column(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = sign.symbol(),
                    style = MaterialTheme.typography.headlineLarge,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = sign.localizedLabel(strings),
                    style = MaterialTheme.typography.titleSmall,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Medium,
                )
            }
        }
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
    isLoading: Boolean,
    errorMessage: String?,
) {
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
            BWitchPrimaryButton(onClick = onUnlock, enabled = !isLoading) {
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

private fun ZodiacSign.symbol(): String = when (this) {
    ZodiacSign.aries -> "♈"
    ZodiacSign.taurus -> "♉"
    ZodiacSign.gemini -> "♊"
    ZodiacSign.cancer -> "♋"
    ZodiacSign.leo -> "♌"
    ZodiacSign.virgo -> "♍"
    ZodiacSign.libra -> "♎"
    ZodiacSign.scorpio -> "♏"
    ZodiacSign.sagittarius -> "♐"
    ZodiacSign.capricorn -> "♑"
    ZodiacSign.aquarius -> "♒"
    ZodiacSign.pisces -> "♓"
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
}
