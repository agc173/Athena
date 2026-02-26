package com.agc.bwitch.ui.astrology

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.agc.bwitch.domain.astrology.horoscope.ZodiacSign
import com.agc.bwitch.presentation.astrology.horoscope.HoroscopeUiState
import com.agc.bwitch.presentation.astrology.horoscope.HoroscopeViewModel
import org.koin.compose.koinInject

@Composable
fun HoroscopeScreen(
    contentPadding: PaddingValues,
    preselectedSign: ZodiacSign? = null,
    modifier: Modifier = Modifier,
    viewModel: HoroscopeViewModel = koinInject()
) {
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
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        HoroscopeScreenContent(
            modifier = Modifier.padding(contentPadding).padding(innerPadding),
            state = state,
            onSelectSign = viewModel::onSelectSign,
            onRefresh = viewModel::onRefresh
        )
    }
}


@Composable
private fun HoroscopeScreenContent(
    modifier: Modifier = Modifier,
    state: HoroscopeUiState,
    onSelectSign: (ZodiacSign) -> Unit,
    onRefresh: () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        SignSelector(
            selected = state.selectedSign,
            onSelected = onSelectSign
        )

        Button(
            onClick = onRefresh,
            enabled = !state.isRefreshing
        ) {
            Text(
                when {
                    state.isRefreshing -> "Actualizando..."
                    state.isLoading -> "Cargando..."
                    else -> "Actualizar"
                }
            )
        }


        state.horoscope?.let { h ->
            Card {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Fecha: ${h.dateIso}", style = MaterialTheme.typography.labelLarge)
                    Text(h.text, style = MaterialTheme.typography.bodyLarge)
                    Text("Mood: ${h.mood}")
                    Text("Número de la suerte: ${h.luckyNumber}")
                    Text("Color de la suerte: ${h.luckyColor}")
                }
            }
        }
    }
}

@Composable
private fun SignSelector(
    selected: ZodiacSign,
    onSelected: (ZodiacSign) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val options = remember { ZodiacSign.values().toList() }

    Box {
        OutlinedButton(onClick = { expanded = true }) {
            Text(selected.name.lowercase().replaceFirstChar { it.uppercase() })
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            options.forEach { sign ->
                DropdownMenuItem(
                    text = { Text(sign.name.lowercase().replaceFirstChar { it.uppercase() }) },
                    onClick = {
                        expanded = false
                        onSelected(sign)
                    }
                )
            }
        }
    }
}
