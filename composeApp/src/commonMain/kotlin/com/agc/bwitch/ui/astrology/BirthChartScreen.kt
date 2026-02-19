package com.agc.bwitch.ui.astrology

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.PaddingValues
import com.agc.bwitch.presentation.astrology.birthchart.BirthChartViewModel
import org.koin.compose.koinInject

@Composable
fun BirthChartScreen(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    viewModel: BirthChartViewModel = koinInject()
) {
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .padding(contentPadding)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Introduce tus datos de nacimiento", style = MaterialTheme.typography.bodyMedium)

        OutlinedTextField(
            value = state.dateText,
            onValueChange = viewModel::onDateChange,
            label = { Text("Fecha (YYYY-MM-DD)") },
            singleLine = true,
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = state.timeText,
            onValueChange = viewModel::onTimeChange,
            label = { Text("Hora (HH:MM)") },
            singleLine = true,
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = state.placeText,
            onValueChange = viewModel::onPlaceChange,
            label = { Text("Lugar (ciudad/país)") },
            singleLine = true,
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = viewModel::onSave,
            enabled = !state.isLoading,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (state.isLoading) "Guardando..." else "Guardar")
        }

        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        state.savedMessage?.let { Text(it) }
    }
}
