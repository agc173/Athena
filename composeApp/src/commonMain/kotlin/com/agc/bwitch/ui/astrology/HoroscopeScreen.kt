package com.agc.bwitch.ui.astrology

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.agc.bwitch.domain.astrology.horoscope.ZodiacSign
import com.agc.bwitch.localization.AppStrings
import com.agc.bwitch.localization.appStrings
import com.agc.bwitch.presentation.astrology.horoscope.HoroscopeUiState
import com.agc.bwitch.presentation.astrology.horoscope.HoroscopeViewModel
import com.agc.bwitch.ui.common.designsystem.BWitchPrimaryButton
import com.agc.bwitch.ui.common.designsystem.BWitchSecondaryButton
import com.agc.bwitch.ui.theme.BWitchThemeTokens
import org.koin.compose.koinInject

@Composable
fun HoroscopeScreen(
    contentPadding: PaddingValues,
    preselectedSign: ZodiacSign? = null,
    modifier: Modifier = Modifier,
    viewModel: HoroscopeViewModel = koinInject()
) {
    val colors = MaterialTheme.colorScheme
    val strings = appStrings
    val state by viewModel.uiState.collectAsState()

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(preselectedSign) {
        preselectedSign?.let { sign ->
            viewModel.onSelectSign(sign)
        }
    }

    // Info snackbar
    LaunchedEffect(state.infoMessage) {
        val msg = state.infoMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message = msg)
        viewModel.onInfoShown()
    }

    // Error snackbar
    LaunchedEffect(state.errorMessage) {
        val msg = state.errorMessage ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message = msg)
        viewModel.onErrorShown()
    }

    Scaffold(
        modifier = modifier,
        containerColor = colors.background,
        snackbarHost = {
            SnackbarHost(
                snackbarHostState,
                snackbar = { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = colors.surfaceVariant,
                        contentColor = colors.onSurface
                    )
                }
            )
        }
    ) { innerPadding ->
        HoroscopeScreenContent(
            modifier = Modifier.padding(contentPadding).padding(innerPadding),
            state = state,
            strings = strings,
            onSelectSign = viewModel::onSelectSign,
            onRefresh = viewModel::onRefresh
        )
    }
}


@Composable
private fun HoroscopeScreenContent(
    modifier: Modifier = Modifier,
    state: HoroscopeUiState,
    strings: AppStrings,
    onSelectSign: (ZodiacSign) -> Unit,
    onRefresh: () -> Unit
) {
    val dimens = BWitchThemeTokens.dimens
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(dimens.spacingMd),
        verticalArrangement = Arrangement.spacedBy(dimens.spacingSm + dimens.spacingXs)
    ) {
        SignSelector(
            selected = state.selectedSign,
            strings = strings,
            onSelected = onSelectSign
        )

        BWitchPrimaryButton(
            onClick = onRefresh,
            enabled = !state.isRefreshing,
        ) {
            Text(
                when {
                    state.isRefreshing -> strings.horoscope.refreshLoading
                    state.isLoading -> strings.horoscope.loading
                    else -> strings.horoscope.refreshCta
                }
            )
        }


        state.horoscope?.let { h ->
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = colors.surfaceVariant,
                    contentColor = colors.onSurface
                )
            ) {
                Column(
                    modifier = Modifier.padding(dimens.spacingMd),
                    verticalArrangement = Arrangement.spacedBy(dimens.spacingSm)
                ) {
                    Text("${strings.horoscope.dateLabel}: ${h.dateIso}", style = MaterialTheme.typography.labelLarge)
                    Text(h.text, style = MaterialTheme.typography.bodyLarge)
                    Text("${strings.horoscope.moodLabel}: ${h.mood}", color = colors.onSurfaceVariant)
                    Text("${strings.horoscope.luckyNumberLabel}: ${h.luckyNumber}", color = colors.onSurfaceVariant)
                    Text("${strings.horoscope.luckyColorLabel}: ${h.luckyColor}", color = colors.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun SignSelector(
    selected: ZodiacSign,
    strings: AppStrings,
    onSelected: (ZodiacSign) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val options = remember { ZodiacSign.values().toList() }

    Box {
        BWitchSecondaryButton(onClick = { expanded = true }) {
            Text(selected.localizedLabel(strings))
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { sign ->
                DropdownMenuItem(
                    text = { Text(sign.localizedLabel(strings)) },
                    onClick = {
                        expanded = false
                        onSelected(sign)
                    }
                )
            }
        }
    }
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
