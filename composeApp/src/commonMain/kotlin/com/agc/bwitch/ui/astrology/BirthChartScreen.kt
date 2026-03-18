package com.agc.bwitch.ui.astrology

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenu
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.agc.bwitch.domain.astrology.horoscope.ZodiacSign
import com.agc.bwitch.presentation.astrology.birthchart.BirthChartViewModel
import com.agc.bwitch.ui.common.designsystem.BWitchCard
import com.agc.bwitch.ui.common.designsystem.BWitchPrimaryButton
import com.agc.bwitch.ui.theme.BWitchThemeTokens
import org.koin.compose.koinInject

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BirthChartScreen(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    viewModel: BirthChartViewModel = koinInject()
) {
    val dimens = BWitchThemeTokens.dimens
    val extras = BWitchThemeTokens.extras
    val state by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .padding(contentPadding)
            .padding(dimens.spacingMd),
        verticalArrangement = Arrangement.spacedBy(dimens.spacingSm + dimens.spacingXs)
    ) {
        Text(
            "Esencia natal",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Text(
            "Selecciona Sol, Luna y Ascendente para recibir una lectura breve estilo BWitch.",
            style = MaterialTheme.typography.bodyMedium,
            color = extras.textSecondary,
        )

        SignDropdown(
            label = "Signo solar",
            selected = state.selectedSunSign,
            enabled = !state.isBusy,
            onSelect = viewModel::onSunSignChange,
        )

        SignDropdown(
            label = "Signo lunar",
            selected = state.selectedMoonSign,
            enabled = !state.isBusy,
            onSelect = viewModel::onMoonSignChange,
        )

        SignDropdown(
            label = "Ascendente",
            selected = state.selectedRisingSign,
            enabled = !state.isBusy,
            onSelect = viewModel::onRisingSignChange,
        )

        BWitchPrimaryButton(
            onClick = viewModel::discoverEssence,
            enabled = !state.isBusy,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (state.isGenerating) "Descubriendo..." else "Descubrir mi esencia")
        }

        if (state.hasGeneratedResult) {
            BWitchCard {
                Text("Tu triada", style = MaterialTheme.typography.titleMedium)
                Text(
                    "Sol: ${state.selectedSunSign.toDisplayName()} · Luna: ${state.selectedMoonSign.toDisplayName()} · Asc: ${state.selectedRisingSign.toDisplayName()}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = extras.textSecondary,
                )
                state.generatedArchetype?.let {
                    Text("Arquetipo: $it", style = MaterialTheme.typography.titleSmall)
                }
                Text(
                    text = state.generatedInterpretation.orEmpty(),
                    style = MaterialTheme.typography.bodyMedium,
                )
                BWitchPrimaryButton(
                    onClick = viewModel::saveActiveEssence,
                    enabled = !state.isBusy,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(if (state.isSaving) "Guardando..." else "Guardar en mi perfil")
                }
            }
        }

        BWitchPrimaryButton(
            onClick = viewModel::refresh,
            enabled = !state.isBusy,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(if (state.isRefreshing) "Sincronizando..." else "Sincronizar")
        }

        state.error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        state.savedSummary?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SignDropdown(
    label: String,
    selected: ZodiacSign,
    enabled: Boolean,
    onSelect: (ZodiacSign) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) expanded = !expanded },
    ) {
        OutlinedTextField(
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            value = selected.toDisplayName(),
            onValueChange = {},
            label = { Text(label) },
            readOnly = true,
            enabled = enabled,
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            ZodiacSign.entries.forEach { sign ->
                DropdownMenuItem(
                    text = { Text(sign.toDisplayName()) },
                    onClick = {
                        onSelect(sign)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun ZodiacSign.toDisplayName(): String = when (this) {
    ZodiacSign.aries -> "Aries"
    ZodiacSign.taurus -> "Tauro"
    ZodiacSign.gemini -> "Géminis"
    ZodiacSign.cancer -> "Cáncer"
    ZodiacSign.leo -> "Leo"
    ZodiacSign.virgo -> "Virgo"
    ZodiacSign.libra -> "Libra"
    ZodiacSign.scorpio -> "Escorpio"
    ZodiacSign.sagittarius -> "Sagitario"
    ZodiacSign.capricorn -> "Capricornio"
    ZodiacSign.aquarius -> "Acuario"
    ZodiacSign.pisces -> "Piscis"
}
