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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
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
import com.agc.bwitch.presentation.astrology.horoscope.HoroscopeDayItemUi
import com.agc.bwitch.presentation.astrology.horoscope.HoroscopeFeedbackMessage
import com.agc.bwitch.presentation.astrology.horoscope.HoroscopeTab
import com.agc.bwitch.presentation.astrology.horoscope.HoroscopeUiState
import com.agc.bwitch.presentation.astrology.horoscope.HoroscopeViewModel
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
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(preselectedSign) { preselectedSign?.let(viewModel::onSelectSign) }
    LaunchedEffect(state.infoMessage) {
        state.infoMessage?.let {
            snackbarHostState.showSnackbar(it.toLocalizedMessage(strings))
            viewModel.onInfoShown()
        }
    }
    LaunchedEffect(state.errorMessage) {
        val message = state.errorMessage ?: return@LaunchedEffect
        if (state.overlay != null) return@LaunchedEffect
        snackbarHostState.showSnackbar(message.toLocalizedMessage(strings))
        viewModel.onErrorShown()
    }

    Scaffold(
        modifier = modifier,
        snackbarHost = { SnackbarHost(snackbarHostState) { Snackbar(it) } },
    ) { innerPadding ->
        HoroscopeScreenContent(
            modifier = Modifier.padding(contentPadding).padding(innerPadding),
            state = state,
            strings = strings,
            onSelectTab = viewModel::onSelectTab,
            onSelectDate = viewModel::onSelectDate,
            onOpenSign = viewModel::onOpenSign,
            onRefresh = viewModel::onRefresh,
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
    onOpenSign: (ZodiacSign) -> Unit,
    onRefresh: () -> Unit,
    onUnlock: () -> Unit,
    onCloseOverlay: () -> Unit,
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        val tabs = HoroscopeTab.values()
        TabRow(selectedTabIndex = tabs.indexOf(state.selectedTab).coerceAtLeast(0)) {
            tabs.forEach { tab ->
                val isDaily = tab == HoroscopeTab.Daily
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
                    enabled = isDaily,
                )
            }
        }

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(state.days, key = { it.dateIso }) { day ->
                DayChip(day = day, strings = strings, onClick = { onSelectDate(day.dateIso) })
            }
        }

        BWitchSecondaryButton(onClick = onRefresh, enabled = !state.isRefreshing) {
            Text(if (state.isRefreshing) strings.horoscope.refreshLoading else strings.horoscope.refreshCta)
        }

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            maxItemsInEachRow = 3,
        ) {
            ZodiacSign.values().forEach { sign ->
                ZodiacSignCard(
                    sign = sign,
                    isProfileSign = state.highlightedSign == sign,
                    strings = strings,
                    onClick = { onOpenSign(sign) },
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
private fun DayChip(day: HoroscopeDayItemUi, strings: AppStrings, onClick: () -> Unit) {
    BWitchSecondaryButton(onClick = onClick) {
        Text(
            "${if (day.isToday) strings.horoscope.todayLabel else day.shortLabel}${if (day.isLocked) " 🔒" else ""}",
            fontWeight = if (day.isSelected) FontWeight.Bold else FontWeight.Normal,
        )
    }
}

@Composable
private fun ZodiacSignCard(
    sign: ZodiacSign,
    isProfileSign: Boolean,
    strings: AppStrings,
    onClick: () -> Unit,
) {
    val colors = MaterialTheme.colorScheme
    Card(
        onClick = onClick,
        modifier = Modifier
            .weight(1f)
            .widthIn(min = 96.dp)
            .aspectRatio(1f)
            .semantics {
                role = Role.Button
                contentDescription = sign.localizedLabel(strings)
            },
        colors = CardDefaults.cardColors(containerColor = colors.surfaceVariant.copy(alpha = 0.55f)),
        border = BorderStroke(1.dp, if (isProfileSign) colors.primary else colors.outlineVariant),
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(10.dp)) {
            if (isProfileSign) {
                AssistChip(
                    onClick = onClick,
                    label = { Text(strings.horoscope.yourSignBadge) },
                    modifier = Modifier.align(Alignment.TopEnd),
                    colors = AssistChipDefaults.assistChipColors(containerColor = colors.primaryContainer),
                )
            }
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
}
