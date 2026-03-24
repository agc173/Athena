package com.agc.bwitch.ui.astrology

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.agc.bwitch.domain.astrology.birthchart.BirthEssenceArchetype
import com.agc.bwitch.domain.astrology.birthchart.BirthEssenceProfile
import com.agc.bwitch.domain.astrology.horoscope.ZodiacSign
import com.agc.bwitch.presentation.astrology.birthchart.BirthChartUiState
import com.agc.bwitch.presentation.astrology.birthchart.BirthChartViewModel
import com.agc.bwitch.ui.common.toVisualResource
import com.agc.bwitch.ui.common.designsystem.BWitchCard
import com.agc.bwitch.ui.common.designsystem.BWitchPrimaryButton
import com.agc.bwitch.ui.theme.BWitchThemeTokens
import org.jetbrains.compose.resources.painterResource
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
    val shareLauncher = rememberBirthEssenceShareLauncher()
    var shareError by remember { mutableStateOf<String?>(null) }

    Column(
        modifier = modifier
            .padding(contentPadding)
            .padding(dimens.spacingMd)
            .verticalScroll(rememberScrollState()),
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
                    Text("Arquetipo", style = MaterialTheme.typography.labelLarge, color = extras.textSecondary)
                    Text(it.displayNameEs, style = MaterialTheme.typography.titleLarge)
                    ArchetypeVisual(
                        archetype = it,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.6f)
                    )
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
                BWitchPrimaryButton(
                    onClick = {
                        shareError = null
                        val shareProfile = state.toShareProfileOrNull()
                        if (shareProfile == null) {
                            shareError = "Primero genera una esencia válida"
                            return@BWitchPrimaryButton
                        }

                        shareLauncher
                            .share(shareProfile)
                            .onFailure {
                                shareError = it.message ?: "No se pudo compartir la esencia"
                            }
                    },
                    enabled = !state.isBusy && state.hasGeneratedResult,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Compartir esencia")
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
        shareError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        state.savedSummary?.let { Text(it, color = MaterialTheme.colorScheme.onSurfaceVariant) }
    }
}

private fun BirthChartUiState.toShareProfileOrNull(): BirthEssenceProfile? {
    val interpretation = generatedInterpretation?.trim().orEmpty()
    if (interpretation.isBlank()) return null

    return BirthEssenceProfile(
        sunSign = selectedSunSign,
        moonSign = selectedMoonSign,
        risingSign = selectedRisingSign,
        interpretation = interpretation,
        archetype = generatedArchetype,
        savedAtEpochMillis = 0L,
        updatedAtEpochMillis = 0L,
    )
}

@Composable
private fun SignDropdown(
    label: String,
    selected: ZodiacSign,
    enabled: Boolean,
    onSelect: (ZodiacSign) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        verticalArrangement = Arrangement.spacedBy(BWitchThemeTokens.dimens.spacingXs)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedButton(
            onClick = { expanded = true },
            enabled = enabled,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = selected.toDisplayName(),
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        if (expanded) {
            SignPickerDialog(
                title = label,
                selected = selected,
                onSelect = {
                    onSelect(it)
                    expanded = false
                },
                onDismiss = { expanded = false }
            )
        }
    }
}

@Composable
private fun SignPickerDialog(
    title: String,
    selected: ZodiacSign,
    onSelect: (ZodiacSign) -> Unit,
    onDismiss: () -> Unit,
) {
    val spacing = BWitchThemeTokens.dimens

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = true)
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = spacing.spacingXs,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(spacing.spacingMd),
                verticalArrangement = Arrangement.spacedBy(spacing.spacingXs)
            ) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(spacing.spacingXs)
                ) {
                    ZodiacSign.entries.forEach { sign ->
                        OutlinedButton(
                            onClick = { onSelect(sign) },
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            val text = if (sign == selected) "${sign.toDisplayName()} ✓" else sign.toDisplayName()
                            Text(text)
                        }
                    }
                }
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

@Composable
private fun ArchetypeVisual(
    archetype: BirthEssenceArchetype,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Image(
            painter = painterResource(archetype.toVisualResource()),
            contentDescription = "Visual ${archetype.displayNameEs}",
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1.6f),
            contentScale = ContentScale.Fit,
        )
    }
}
