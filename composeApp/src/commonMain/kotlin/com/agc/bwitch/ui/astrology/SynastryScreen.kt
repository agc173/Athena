package com.agc.bwitch.ui.astrology

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.agc.bwitch.domain.astrology.horoscope.ZodiacSign
import com.agc.bwitch.domain.astrology.synastry.SynastryDailyAxisState
import com.agc.bwitch.domain.astrology.synastry.SynastryDimension
import com.agc.bwitch.domain.astrology.synastry.SynastryEnergyAxis
import com.agc.bwitch.domain.astrology.synastry.SynastryReading
import com.agc.bwitch.domain.astrology.synastry.SynastryReadingDepth
import com.agc.bwitch.domain.astrology.synastry.SynastrySignal
import com.agc.bwitch.domain.astrology.synastry.toFiveStarRating
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

        reading.dailyOverlay?.let { daily ->
            Text(text = "Energía del día", style = MaterialTheme.typography.titleSmall)
            Text(
                text = daily.dailyEnergyLabel,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            daily.axes.forEach { axis ->
                DailyEnergyAxisRow(axis)
            }
            Text(
                text = "Guía de hoy: ${daily.dailyGuidance}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Text(text = "Fortaleza principal", style = MaterialTheme.typography.titleSmall)
        Text(
            text = structured.strengths.firstOrNull()?.toUiLabel()
                ?: "No se detectó una fortaleza dominante en esta lectura.",
            style = MaterialTheme.typography.bodyMedium,
        )

        Text(text = "Tensión principal", style = MaterialTheme.typography.titleSmall)
        Text(
            text = structured.tensions.firstOrNull()?.toUiLabel()
                ?: "No se detectó una tensión dominante en esta lectura.",
            style = MaterialTheme.typography.bodyMedium,
        )

        Text(text = "Guía principal", style = MaterialTheme.typography.titleSmall)
        Text(
            text = structured.guidance.firstOrNull()?.toUiLabel()
                ?: "Mantengan una comunicación presente y honesta.",
            style = MaterialTheme.typography.bodyMedium,
        )

        Text(text = "Narrativa", style = MaterialTheme.typography.titleSmall)
        Text(
            text = reading.narrative,
            style = MaterialTheme.typography.bodyMedium,
        )
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
    val normalized = stars.coerceIn(0.0, 5.0)

    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(5) { index ->
            val fillFraction = (normalized - index).coerceIn(0.0, 1.0).toFloat()
            StarCell(fillFraction = fillFraction)
        }
    }
}

@Composable
private fun StarCell(fillFraction: Float) {
    Box {
        Text(
            text = "☆",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.outline,
        )
        if (fillFraction > 0f) {
            Box(modifier = Modifier.fillMaxWidth(fillFraction).clipToBounds()) {
                Text(
                    text = "★",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun DailyEnergyAxisRow(axis: SynastryDailyAxisState) {
    val position = ((axis.value + 100f) / 200f).coerceIn(0f, 1f)
    val markerSize = 16.dp

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(
                text = axis.leftLabel(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = axis.rightLabel(),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp),
        ) {
            val travelRange = (maxWidth - markerSize).coerceAtLeast(0.dp)
            val markerOffset = travelRange * position

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(2.dp)
                    .height(14.dp)
                    .background(MaterialTheme.colorScheme.outline),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = markerOffset)
                    .size(markerSize)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary),
            )
        }

        Text(
            text = axis.positionLabel(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
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

private fun SynastryDailyAxisState.leftLabel(): String = when (axis) {
    SynastryEnergyAxis.HARMONY_INTENSITY -> "Armonía"
    SynastryEnergyAxis.STABILITY_TRANSFORMATION -> "Estabilidad"
    SynastryEnergyAxis.CALM_MOVEMENT -> "Calma"
}

private fun SynastryDailyAxisState.rightLabel(): String = when (axis) {
    SynastryEnergyAxis.HARMONY_INTENSITY -> "Intensidad"
    SynastryEnergyAxis.STABILITY_TRANSFORMATION -> "Transformación"
    SynastryEnergyAxis.CALM_MOVEMENT -> "Movimiento"
}

private fun SynastryDailyAxisState.positionLabel(): String = when {
    value in -12..12 -> "Eje equilibrado"
    value > 0 -> "Más cerca de ${rightLabel()}"
    else -> "Más cerca de ${leftLabel()}"
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
