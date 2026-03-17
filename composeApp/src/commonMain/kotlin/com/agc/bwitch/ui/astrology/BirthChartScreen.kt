package com.agc.bwitch.ui.astrology

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.agc.bwitch.presentation.astrology.birthchart.BirthChartViewModel
import com.agc.bwitch.ui.theme.BWitchThemeTokens
import org.koin.compose.koinInject

@Composable
fun BirthChartScreen(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    viewModel: BirthChartViewModel = koinInject()
) {
    val dimens = BWitchThemeTokens.dimens
    val extras = BWitchThemeTokens.extras
    val state by viewModel.uiState.collectAsState()

    val inputsEnabled = !state.isLoading && !state.isBusy

    Column(
        modifier = modifier
            .padding(contentPadding)
            .padding(dimens.spacingMd),
        verticalArrangement = Arrangement.spacedBy(dimens.spacingSm + dimens.spacingXs)
    ) {
        Text(
            "Introduce tus datos de nacimiento",
            style = MaterialTheme.typography.bodyMedium,
            color = extras.textSecondary
        )

        OutlinedTextField(
            value = state.dateText,
            onValueChange = viewModel::onDateChange,
            label = { Text("Fecha (YYYY-MM-DD)") },
            singleLine = true,
            enabled = inputsEnabled,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = state.timeText,
            onValueChange = viewModel::onTimeChange,
            label = { Text("Hora (HH:MM)") },
            singleLine = true,
            enabled = inputsEnabled,
            modifier = Modifier.fillMaxWidth()
        )

        OutlinedTextField(
            value = state.placeText,
            onValueChange = viewModel::onPlaceChange,
            label = { Text("Lugar (ciudad/país)") },
            singleLine = true,
            enabled = inputsEnabled,
            modifier = Modifier.fillMaxWidth()
        )

        Button(
            onClick = viewModel::refresh,
            enabled = !state.isLoading && !state.isBusy,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (state.isRefreshing) "Refrescando..." else "Refrescar")
        }

        Button(
            onClick = viewModel::onSave,
            enabled = !state.isLoading && !state.isBusy,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (state.isSaving) "Guardando..." else "Guardar")
        }

        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        state.savedMessage?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}
