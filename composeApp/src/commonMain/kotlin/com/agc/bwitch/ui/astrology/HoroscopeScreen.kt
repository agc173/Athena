package com.agc.bwitch.ui.astrology

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.font.FontWeight
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
                economyViewModel.requireLunas(
                    cost = state.futureDayCost,
                    source = "horoscope_daily_unlock",
                ) { viewModel.onUnlockSelectedDay() }
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

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ZodiacSign.values().forEach { sign ->
                val isProfile = state.highlightedSign == sign
                BWitchSecondaryButton(onClick = { onOpenSign(sign) }, modifier = Modifier.widthIn(min = 100.dp)) {
                    Text("${sign.symbol()} ${sign.localizedLabel(strings)}${if (isProfile) " · ${strings.horoscope.yourSignBadge}" else ""}")
                }
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
                    Text("${overlay.sign.symbol()} ${overlay.sign.localizedLabel(strings)} · ${overlay.dateIso}\n${strings.horoscope.unlockMessage}")
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
