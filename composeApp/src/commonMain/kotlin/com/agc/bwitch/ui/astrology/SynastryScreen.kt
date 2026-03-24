package com.agc.bwitch.ui.astrology

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.agc.bwitch.domain.astrology.horoscope.ZodiacSign
import com.agc.bwitch.domain.astrology.synastry.SynastryDailyAxisState
import com.agc.bwitch.domain.astrology.synastry.SynastryDimension
import com.agc.bwitch.domain.astrology.synastry.SynastryEnergyAxis
import com.agc.bwitch.domain.astrology.synastry.SynastryReading
import com.agc.bwitch.domain.astrology.synastry.toFiveStarRating
import com.agc.bwitch.domain.astrology.synastry.SynastryReadingDepth
import com.agc.bwitch.domain.astrology.synastry.SynastrySignal
import com.agc.bwitch.presentation.astrology.synastry.SynastryPersonForm
import com.agc.bwitch.presentation.astrology.synastry.SynastryViewModel
import com.agc.bwitch.ui.common.designsystem.BWitchCard
import com.agc.bwitch.ui.common.designsystem.BWitchPrimaryButton
import com.agc.bwitch.ui.theme.BWitchThemeTokens
import org.koin.compose.koinInject

@Composable
fun SynastryScreen(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    viewModel: SynastryViewModel = koinInject(),
) {
    val state by viewModel.uiState.collectAsState()
    val spacing = BWitchThemeTokens.dimens

    Column(
        modifier = modifier
            .padding(contentPadding)
            .padding(spacing.spacingMd)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(spacing.spacingSm + spacing.spacingXs)
    ) {
        Text(
            text = "Sinastría",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
        )

        Text(
            text = "Explora la dinámica entre dos signos y descubre qué áreas del vínculo se activan con más fuerza.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "La lectura combina afinidad simbólica entre signos con un matiz energético diario del momento.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        PersonFormCard(
            title = "Carta A",
            form = state.personA,
            onSunChange = viewModel::onPersonASunSignChange,
            onMoonChange = viewModel::onPersonAMoonSignChange,
            onRisingChange = viewModel::onPersonARisingSignChange,
        )

        PersonFormCard(
            title = "Carta B",
            form = state.personB,
            onSunChange = viewModel::onPersonBSunSignChange,
            onMoonChange = viewModel::onPersonBMoonSignChange,
            onRisingChange = viewModel::onPersonBRisingSignChange,
        )

        BWitchPrimaryButton(
            onClick = viewModel::generate,
            enabled = state.canGenerate,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(if (state.isGenerating) "Calculando..." else "Calcular lectura")
        }

        state.error?.let { error ->
            Text(
                text = error,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        state.reading?.let { reading ->
            SynastryResultCard(reading = reading)
        }
    }
}

@Composable
private fun PersonFormCard(
    title: String,
    form: SynastryPersonForm,
    onSunChange: (ZodiacSign?) -> Unit,
    onMoonChange: (ZodiacSign?) -> Unit,
    onRisingChange: (ZodiacSign?) -> Unit,
) {
    BWitchCard {
        Text(text = title, style = MaterialTheme.typography.titleMedium)

        SignDropdown(
            label = "Signo solar (obligatorio)",
            selected = form.sunSign,
            allowEmpty = false,
            onSelect = onSunChange,
        )

        SignDropdown(
            label = "Luna (opcional)",
            selected = form.moonSign,
            allowEmpty = true,
            onSelect = onMoonChange,
        )

        SignDropdown(
            label = "Ascendente (opcional)",
            selected = form.risingSign,
            allowEmpty = true,
            onSelect = onRisingChange,
        )
    }
}

@Composable
private fun SynastryResultCard(reading: SynastryReading) {
    val structured = reading.structured

    BWitchCard {
        Text(text = "Resultado", style = MaterialTheme.typography.titleMedium)
        Text(
            text = structured.depthInfo.depth.toUiDepthLabel(),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        Text(text = "Métricas del vínculo", style = MaterialTheme.typography.titleSmall)
        metricOrder.forEach { dimension ->
            val score = structured.scores[dimension] ?: return@forEach
            MetricStarsRow(dimension = dimension, stars = score.toFiveStarRating())
        }

        Text(text = "Fortalezas", style = MaterialTheme.typography.titleSmall)
        if (structured.strengths.isEmpty()) {
            Text("• No se detectaron fortalezas dominantes en esta lectura.")
        } else {
            structured.strengths.forEach { Text("• ${it.toUiLabel()}") }
        }

        Text(text = "Tensiones", style = MaterialTheme.typography.titleSmall)
        if (structured.tensions.isEmpty()) {
            Text("• No se detectaron tensiones principales.")
        } else {
            structured.tensions.forEach { Text("• ${it.toUiLabel()}") }
        }

        Text(text = "Guía", style = MaterialTheme.typography.titleSmall)
        if (structured.guidance.isEmpty()) {
            Text("• Mantengan una comunicación presente y honesta.")
        } else {
            structured.guidance.forEach { Text("• ${it.toUiLabel()}") }
        }

        Text(text = "Narrativa", style = MaterialTheme.typography.titleSmall)
        Text(
            text = reading.narrative,
            style = MaterialTheme.typography.bodyMedium,
        )

        reading.dailyOverlay?.let { daily ->
            Text(text = "Clima del vínculo hoy", style = MaterialTheme.typography.titleSmall)
            Text(
                text = "• Energía: ${daily.dailyEnergyLabel}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "• Dimensión destacada: ${daily.highlightedDimension.toUiLabel()}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = "• Dimensión sensible: ${daily.sensitiveDimension.toUiLabel()}",
                style = MaterialTheme.typography.bodyMedium,
            )
            daily.axes.forEach { axis ->
                Text(
                    text = "• ${axis.toUiLabel()}",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            Text(
                text = "• Consejo diario: ${daily.dailyGuidance}",
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = daily.dailyNarrativeFragment,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}



@Composable
private fun MetricStarsRow(dimension: SynastryDimension, stars: Double) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = dimension.toUiLabel(), style = MaterialTheme.typography.bodyMedium)
        StarRating(stars = stars)
    }
}

@Composable
private fun StarRating(stars: Double) {
    val full = stars.toInt()
    val hasHalf = stars - full >= 0.5
    val empty = 5 - full - if (hasHalf) 1 else 0

    Row(modifier = Modifier.width(BWitchThemeTokens.dimens.spacingMd * 10)) {
        repeat(full) { Text("★") }
        if (hasHalf) Text("⯨")
        repeat(empty) { Text("☆") }
    }
}

private val metricOrder = listOf(
    SynastryDimension.ATTRACTION,
    SynastryDimension.EMOTIONAL,
    SynastryDimension.COMMUNICATION,
    SynastryDimension.GROWTH,
)

@Composable
private fun SignDropdown(
    label: String,
    selected: ZodiacSign?,
    allowEmpty: Boolean,
    onSelect: (ZodiacSign?) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(BWitchThemeTokens.dimens.spacingXs)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text(
                text = selected?.toDisplayName() ?: "Seleccionar",
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.onSurface,
            )
        }

        if (expanded) {
            SignPickerDialog(
                title = label,
                selected = selected,
                allowEmpty = allowEmpty,
                onSelect = {
                    onSelect(it)
                    expanded = false
                },
                onDismiss = { expanded = false },
            )
        }
    }
}


@Composable
private fun SignPickerDialog(
    title: String,
    selected: ZodiacSign?,
    allowEmpty: Boolean,
    onSelect: (ZodiacSign?) -> Unit,
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
                    .padding(spacing.spacingMd)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(spacing.spacingXs)
            ) {
                Text(text = title, style = MaterialTheme.typography.titleMedium)

                if (allowEmpty) {
                    OutlinedButton(
                        onClick = { onSelect(null) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(text = if (selected == null) "No especificar ✓" else "No especificar")
                    }
                }

                ZodiacSign.entries.forEach { sign ->
                    OutlinedButton(
                        onClick = { onSelect(sign) },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text(
                            text = if (sign == selected) "${sign.toDisplayName()} ✓" else sign.toDisplayName()
                        )
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

private fun SynastryReadingDepth.toUiDepthLabel(): String = when (this) {
    SynastryReadingDepth.BASIC -> "Lectura esencial"
    SynastryReadingDepth.PARTIAL -> "Lectura ampliada"
    SynastryReadingDepth.COMPLETE -> "Lectura completa"
}

private fun SynastryDimension.toUiLabel(): String = when (this) {
    SynastryDimension.ATTRACTION -> "Atracción"
    SynastryDimension.EMOTIONAL -> "Sintonía emocional"
    SynastryDimension.COMMUNICATION -> "Comunicación"
    SynastryDimension.GROWTH -> "Potencial de crecimiento"
}

private fun SynastryDailyAxisState.toUiLabel(): String = when (axis) {
    SynastryEnergyAxis.HARMONY_INTENSITY -> if (value >= 0) "Armonía ←→ Intensidad: más Intensidad" else "Armonía ←→ Intensidad: más Armonía"
    SynastryEnergyAxis.STABILITY_TRANSFORMATION -> if (value >= 0) "Estabilidad ←→ Transformación: más Transformación" else "Estabilidad ←→ Transformación: más Estabilidad"
    SynastryEnergyAxis.CALM_MOVEMENT -> if (value >= 0) "Calma ←→ Movimiento: más Movimiento" else "Calma ←→ Movimiento: más Calma"
}

private fun SynastrySignal.toUiLabel(): String = when (this) {
    SynastrySignal.STRONG_EMOTIONAL_RESONANCE -> "Resonancia emocional fuerte"
    SynastrySignal.DIFFERENT_EMOTIONAL_RHYTHMS -> "Ritmos emocionales diferentes"
    SynastrySignal.NATURAL_SPARK -> "Chispa natural"
    SynastrySignal.COMMUNICATION_FLOW -> "Comunicación fluida"
    SynastrySignal.STABILITY_POTENTIAL -> "Potencial de estabilidad"
    SynastrySignal.GROWTH_THROUGH_DIFFERENCE -> "Crecimiento a través de la diferencia"
    SynastrySignal.HIGH_INTENSITY -> "Intensidad alta"
    SynastrySignal.NEED_FOR_PATIENCE -> "Necesidad de paciencia"
    SynastrySignal.GROUNDING_BOND -> "Vínculo de anclaje"
    SynastrySignal.MENTAL_STIMULATION -> "Estimulación mental"
    SynastrySignal.CREATE_SHARED_RHYTHM -> "Crear ritmo compartido"
    SynastrySignal.USE_DIFFERENCE_AS_GROWTH -> "Usar la diferencia para crecer"
    SynastrySignal.PROTECT_THE_SOFTNESS -> "Proteger la parte sensible"
    SynastrySignal.SLOW_DOWN_REACTIVITY -> "Bajar la reactividad"
}
