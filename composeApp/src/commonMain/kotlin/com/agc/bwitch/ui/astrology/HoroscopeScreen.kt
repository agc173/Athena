package com.agc.bwitch.ui.astrology

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeContentPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.agc.bwitch.domain.astrology.horoscope.ZodiacSign
import com.agc.bwitch.presentation.astrology.horoscope.HoroscopeViewModel
import org.koin.compose.getKoin

@Composable
fun HoroscopeScreen(
    viewModel: HoroscopeViewModel = remember { getKoin().get<HoroscopeViewModel>() },
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .safeContentPadding()
            .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = "Horóscopo diario",
            style = MaterialTheme.typography.headlineSmall,
        )

        ZodiacSign.entries.forEach { sign ->
            SignOption(
                sign = sign,
                selected = state.selectedSign == sign,
                onSelect = { viewModel.onSelectSign(sign) },
            )
        }

        Button(
            onClick = viewModel::onRefresh,
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Actualizar")
        }

        state.errorMessage?.let { message ->
            Text(
                text = message,
                color = MaterialTheme.colorScheme.error,
            )
        }

        state.horoscope?.let { horoscope ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("Fecha: ${horoscope.dateIso}")
                    Text(horoscope.text)
                    Text("Mood: ${horoscope.mood}")
                    Text("Número de la suerte: ${horoscope.luckyNumber}")
                    Text("Color de la suerte: ${horoscope.luckyColor}")
                }
            }
        }
    }
}

@Composable
private fun SignOption(
    sign: ZodiacSign,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    androidx.compose.foundation.layout.Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        RadioButton(
            selected = selected,
            onClick = onSelect,
        )
        Text(sign.label)
    }
}
