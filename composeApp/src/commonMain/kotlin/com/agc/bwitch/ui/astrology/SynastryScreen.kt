package com.agc.bwitch.ui.astrology

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.text.font.FontWeight
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
            personA = state.personA,
            personB = state.personB,
            onPersonASunChange = viewModel::onPersonASunSignChange,
            onPersonAMoonChange = viewModel::onPersonAMoonSignChange,
            onPersonARisingChange = viewModel::onPersonARisingSignChange,
            onPersonBSunChange = viewModel::onPersonBSunSignChange,
            onPersonBMoonChange = viewModel::onPersonBMoonSignChange,
            onPersonBRisingChange = viewModel::onPersonBRisingSignChange,
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
    personA: SynastryPersonForm,
    personB: SynastryPersonForm,
    onPersonASunChange: (ZodiacSign?) -> Unit,
    onPersonAMoonChange: (ZodiacSign?) -> Unit,
    onPersonARisingChange: (ZodiacSign?) -> Unit,
    onPersonBSunChange: (ZodiacSign?) -> Unit,
    onPersonBMoonChange: (ZodiacSign?) -> Unit,
    onPersonBRisingChange: (ZodiacSign?) -> Unit,
) {
    BWitchCard {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Carta A",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                modifier = Modifier.weight(1f),
            )
            Box(
                modifier = Modifier
                    .size(30.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                    .border(
                        width = 1.dp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                        shape = CircleShape,
                    ),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "↔",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = "Carta B",
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                textAlign = TextAlign.End,
                modifier = Modifier.weight(1f),
            )
        }

        ComparativeSignRow(
            label = "☀ Signo solar",
            aSelected = personA.sunSign,
            bSelected = personB.sunSign,
            allowEmpty = false,
            onSelectA = onPersonASunChange,
            onSelectB = onPersonBSunChange,
        )

        ComparativeSignRow(
            label = "🌙 Luna",
            aSelected = personA.moonSign,
            bSelected = personB.moonSign,
            allowEmpty = true,
            onSelectA = onPersonAMoonChange,
            onSelectB = onPersonBMoonChange,
        )

        ComparativeSignRow(
            label = "⤴ Ascendente",
            aSelected = personA.risingSign,
            bSelected = personB.risingSign,
            allowEmpty = true,
            onSelectA = onPersonARisingChange,
            onSelectB = onPersonBRisingChange,
        )
    }
}

@Composable
private fun ComparativeSignRow(
    label: String,
    aSelected: ZodiacSign?,
    bSelected: ZodiacSign?,
    allowEmpty: Boolean,
    onSelectA: (ZodiacSign?) -> Unit,
    onSelectB: (ZodiacSign?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SignDropdown(
                label = "Carta A",
                selected = aSelected,
                allowEmpty = allowEmpty,
                onSelect = onSelectA,
                showLabel = false,
                modifier = Modifier.weight(1f),
            )
            SignDropdown(
                label = "Carta B",
                selected = bSelected,
                allowEmpty = allowEmpty,
                onSelect = onSelectB,
                showLabel = false,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Composable
private fun SynastryResultCard(reading: SynastryReading) {
    val structured = reading.structured

    BWitchCard {
        Text(
            text = "Resultado",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )
        Text(
            text = structured.depthInfo.depth.toUiDepthLabel(),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center,
        )

        SectionSeparator()
        ResultBlockTitle("MÉTRICAS DEL VÍNCULO")
        metricOrder.forEach { dimension ->
            val score = structured.scores[dimension] ?: return@forEach
            MetricStarsRow(dimension = dimension, stars = score.toFiveStarRating())
        }

        reading.dailyOverlay?.let { daily ->
            SectionSeparator()
            ResultBlockTitle("ENERGÍA DEL DÍA")
            daily.axes.forEach { axis ->
                DailyEnergyAxisRow(axis)
            }
        }

        SectionSeparator()
        ResultBlockTitle("FORTALEZA")
        Text(
            text = reading.primaryStrengthCopy(),
            style = MaterialTheme.typography.bodyLarge,
        )

        ResultBlockTitle("TENSIÓN")
        Text(
            text = reading.primaryTensionCopy(),
            style = MaterialTheme.typography.bodyLarge,
        )

        ResultBlockTitle("GUÍA")
        Text(
            text = reading.primaryGuidanceCopy(),
            style = MaterialTheme.typography.bodyLarge,
        )

        reading.dailyOverlay?.let { daily ->
            ResultBlockTitle("CONSEJO DEL DÍA")
            Text(
                text = daily.dailyGuidance,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SectionSeparator()
        ResultBlockTitle("NARRATIVA")
        Text(
            text = reading.narrative,
            style = MaterialTheme.typography.bodyLarge,
        )
    }
}

@Composable
private fun MetricStarsRow(dimension: SynastryDimension, stars: Double) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = dimension.toUiLabel(), style = MaterialTheme.typography.bodyMedium)
        StarRating(stars = stars)
    }
}

@Composable
private fun StarRating(stars: Double) {
    val normalized = stars.coerceIn(0.0, 5.0)

    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        repeat(5) { index ->
            val fillFraction = (normalized - index).coerceIn(0.0, 1.0).toFloat()
            StarCell(fillFraction = fillFraction)
        }
    }
}

@Composable
private fun StarCell(fillFraction: Float) {
    val inactiveColor = MaterialTheme.colorScheme.outlineVariant
    val activeColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = Modifier
            .size(16.dp)
            .drawWithContent {
                val starPath = buildStarPath(size.width, size.height)
                drawPath(path = starPath, color = inactiveColor, style = Fill)
                if (fillFraction > 0f) {
                    clipRect(left = 0f, top = 0f, right = size.width * fillFraction, bottom = size.height) {
                        drawPath(path = starPath, color = activeColor, style = Fill)
                    }
                }
            }
    )
}

@Composable
private fun DailyEnergyAxisRow(axis: SynastryDailyAxisState) {
    val position = ((axis.value + 100f) / 200f).coerceIn(0f, 1f)
    val markerSize = 20.dp
    var showAxisInfo by remember(axis.axis) { mutableStateOf(false) }

    val moonColor = MaterialTheme.colorScheme.tertiary
    val moonCutoutColor = MaterialTheme.colorScheme.surface
    val markerBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
    val axisLineColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.35f)
    val axisLeftColor = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.65f)
    val axisCenterColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.95f)
    val axisRightColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 4.dp),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    text = axis.leftLabel(),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text(
                    text = "• •",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.outline,
                )
                AxisInfoButton(onClick = { showAxisInfo = true })
                Text(
                    text = "• •",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.outline,
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 4.dp),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Text(
                    text = axis.rightLabel(),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.End,
                )
            }
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(30.dp),
        ) {
            val travelRange = (maxWidth - markerSize).coerceAtLeast(0.dp)
            val markerOffset = travelRange * position

            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .height(12.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                axisLeftColor,
                                axisCenterColor,
                                axisRightColor,
                            )
                        )
                    ),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.Center)
                    .width(2.dp)
                    .height(16.dp)
                    .background(axisLineColor),
            )
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = markerOffset)
                    .size(markerSize)
                    .clip(CircleShape)
                    .background(moonCutoutColor)
                    .border(1.5.dp, markerBorderColor, CircleShape),
            )
            Canvas(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .offset(x = markerOffset + 2.dp, y = 2.dp)
                    .size(markerSize - 4.dp),
            ) {
                drawCircle(color = moonColor)
                drawCircle(
                    color = moonCutoutColor,
                    radius = size.minDimension * 0.42f,
                    center = Offset(size.width * 0.64f, size.height * 0.5f),
                )
            }
        }

        if (showAxisInfo) {
            AxisInfoDialog(axis = axis.axis, onDismiss = { showAxisInfo = false })
        }
    }
}

@Composable
private fun AxisInfoButton(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "i",
            style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

@Composable
private fun SectionSeparator() {
    Text(
        text = "✦  ✦  ✦",
        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
        color = MaterialTheme.colorScheme.outline,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun ResultBlockTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp),
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun AxisInfoDialog(
    axis: SynastryEnergyAxis,
    onDismiss: () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .clip(RoundedCornerShape(20.dp)),
            tonalElevation = 6.dp,
            shape = RoundedCornerShape(20.dp),
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Text(
                    text = axis.axisTitle(),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = axis.axisInfoDescription(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text("Cerrar")
                }
            }
        }
    }
}

private fun buildStarPath(width: Float, height: Float): Path {
    val center = Offset(width / 2f, height / 2f)
    val outerRadius = minOf(width, height) / 2f
    val innerRadius = outerRadius * 0.5f
    val path = Path()

    repeat(10) { index ->
        val isOuter = index % 2 == 0
        val radius = if (isOuter) outerRadius.toDouble() else innerRadius.toDouble()
        val angle = (-90.0 + index * 36.0) * (kotlin.math.PI / 180.0)
        val x = center.x + (kotlin.math.cos(angle) * radius).toFloat()
        val y = center.y + (kotlin.math.sin(angle) * radius).toFloat()
        if (index == 0) {
            path.moveTo(x, y)
        } else {
            path.lineTo(x, y)
        }
    }
    path.close()
    return path
}

private fun SynastryEnergyAxis.axisTitle(): String = when (this) {
    SynastryEnergyAxis.HARMONY_INTENSITY -> "Armonía ↔ Intensidad"
    SynastryEnergyAxis.STABILITY_TRANSFORMATION -> "Estabilidad ↔ Transformación"
    SynastryEnergyAxis.CALM_MOVEMENT -> "Calma ↔ Movimiento"
}

private fun SynastryEnergyAxis.axisInfoDescription(): String = when (this) {
    SynastryEnergyAxis.HARMONY_INTENSITY -> "Mide si hoy el vínculo fluye desde equilibrio y facilidad, o desde una chispa emocional más intensa."
    SynastryEnergyAxis.STABILITY_TRANSFORMATION -> "Indica si la energía del día favorece sostener lo construido o abrir cambios y redefinir acuerdos."
    SynastryEnergyAxis.CALM_MOVEMENT -> "Refleja si conviene priorizar pausa y contención, o activar más movimiento, iniciativa y acción compartida."
}

private fun synastryReadingPrimaryCopy(
    signals: List<SynastrySignal>,
    axis: SynastryDailyAxisState?,
    seedBase: Int,
    fallback: String,
): String {
    val primarySignal = signals.firstOrNull() ?: return fallback
    val variants = primarySignal.copyVariants(axis)
    val index = ((seedBase * 31) + primarySignal.ordinal + (axis?.value ?: 0)).absolutePositive() % variants.size
    return variants[index]
}

private fun SynastrySignal.copyVariants(axis: SynastryDailyAxisState?): List<String> = when (this) {
    SynastrySignal.STRONG_EMOTIONAL_RESONANCE -> listOf(
        "Hay una sintonía emocional que facilita comprenderse sin demasiadas explicaciones.",
        "La conexión sensible aparece como un punto fuerte: captan rápido lo que el otro siente.",
        "La base emocional se percibe cercana y eso ayuda a sostener momentos exigentes.",
    )

    SynastrySignal.DIFFERENT_EMOTIONAL_RHYTHMS -> listOf(
        "Los tiempos emocionales no siempre coinciden, y eso puede generar cruces si no se nombran.",
        "La tensión principal está en cómo cada persona procesa lo que siente y cuándo lo expresa.",
        "Se nota diferencia de ritmo emocional: conviene validar tiempos distintos sin forzar respuestas.",
    )

    SynastrySignal.NATURAL_SPARK -> listOf(
        "La atracción aparece de forma natural y mantiene vivo el interés entre ambos.",
        "Hay química espontánea, con capacidad de renovar el vínculo cuando se cuida el ritmo.",
        "La chispa está presente y aporta impulso para acercarse con más facilidad.",
    )

    SynastrySignal.COMMUNICATION_FLOW -> listOf(
        "El diálogo tiende a fluir y eso vuelve más simples los ajustes cotidianos.",
        "Existe una base de comunicación clara que ayuda a ordenar decisiones en conjunto.",
        "Hablarse suele resultar natural, lo que fortalece la coordinación entre ambos.",
    )

    SynastrySignal.STABILITY_POTENTIAL -> listOf(
        "Se percibe potencial para construir una base estable sin perder cercanía.",
        "Hay condiciones para sostener acuerdos en el tiempo con constancia y cuidado.",
        "El vínculo muestra capacidad de anclaje y continuidad cuando priorizan lo esencial.",
    )

    SynastrySignal.GROWTH_THROUGH_DIFFERENCE -> listOf(
        "Las diferencias pueden convertirse en evolución si se usan como aprendizaje mutuo.",
        "Existe potencial de crecimiento compartido cuando no intentan pensar igual en todo.",
        "El vínculo se fortalece al integrar perspectivas distintas en lugar de competir por ellas.",
    )

    SynastrySignal.HIGH_INTENSITY -> listOf(
        "La intensidad es alta y puede desbordar si no acompañan con pausas conscientes.",
        "La carga emocional del vínculo sube rápido; conviene evitar decisiones impulsivas.",
        "Hay mucha chispa, pero también riesgo de reactividad si falta regulación conjunta.",
    )

    SynastrySignal.NEED_FOR_PATIENCE -> listOf(
        "La comunicación pide más paciencia para evitar interpretaciones rápidas.",
        "El principal reto está en sostener conversaciones sin apurar conclusiones.",
        "Conviene bajar el apuro al dialogar para que los matices no se pierdan.",
    )

    SynastrySignal.GROUNDING_BOND -> listOf(
        "Predomina una energía de anclaje que ayuda a bajar ruido y enfocarse en lo importante.",
        "El vínculo ofrece una cualidad de calma práctica que ordena el día a día.",
        "Hay una base aterrizada que favorece decisiones más serenas y consistentes.",
    )

    SynastrySignal.MENTAL_STIMULATION -> listOf(
        "Hay estímulo mental mutuo y eso mantiene viva la curiosidad entre ambos.",
        "La conexión intelectual suma dinamismo y abre conversaciones nutritivas.",
        "Se desafían de forma constructiva, lo que renueva ideas y perspectivas.",
    )

    SynastrySignal.CREATE_SHARED_RHYTHM -> listOf(
        "La clave es crear un ritmo compartido para no entrar en sincronías forzadas.",
        "Conviene acordar tiempos comunes para que la relación gane estabilidad diaria.",
        "Un pulso común, aunque sea simple, puede reducir fricción y mejorar coordinación.",
    )

    SynastrySignal.USE_DIFFERENCE_AS_GROWTH -> listOf(
        "La guía principal es usar la diferencia como recurso de crecimiento.",
        "Lo más útil hoy es convertir los contrastes en acuerdos más inteligentes.",
        "Tomar lo distinto como aprendizaje puede elevar el tono general del vínculo.",
    )

    SynastrySignal.PROTECT_THE_SOFTNESS -> listOf(
        "Conviene cuidar la parte sensible del vínculo antes de discutir formas.",
        "La guía es proteger la sensibilidad mutua y priorizar tono sobre velocidad.",
        "Sostener un clima seguro ayuda a que lo importante pueda decirse mejor.",
    )

    SynastrySignal.SLOW_DOWN_REACTIVITY -> listOf(
        if ((axis?.value ?: 0) >= 0) {
            "La guía es bajar un punto la aceleración para responder con más conciencia."
        } else {
            "Conviene pausar antes de reaccionar para que el diálogo no se tense de más."
        },
        "Tomarse un segundo antes de responder puede cambiar por completo el resultado.",
        "Reducir reactividad mejora el clima y evita escalar roces innecesarios.",
    )
}

private fun Int.absolutePositive(): Int = kotlin.math.abs(this)

private fun SynastryReading.primaryStrengthCopy(): String = synastryReadingPrimaryCopy(
    signals = structured.strengths,
    axis = dailyOverlay?.axes?.maxByOrNull { kotlin.math.abs(it.value) },
    seedBase = structured.overallScore.value + personA.sunSign.ordinal + personB.sunSign.ordinal,
    fallback = "No se detectó una fortaleza dominante en esta lectura.",
)

private fun SynastryReading.primaryTensionCopy(): String = synastryReadingPrimaryCopy(
    signals = structured.tensions,
    axis = dailyOverlay?.axes?.maxByOrNull { kotlin.math.abs(it.value) },
    seedBase = structured.overallScore.value + (personA.sunSign.ordinal * 7) + personB.sunSign.ordinal,
    fallback = "No se detectó una tensión dominante en esta lectura.",
)

private fun SynastryReading.primaryGuidanceCopy(): String = synastryReadingPrimaryCopy(
    signals = structured.guidance,
    axis = dailyOverlay?.axes?.firstOrNull(),
    seedBase = structured.overallScore.value + (personA.sunSign.ordinal * 17) + personB.sunSign.ordinal,
    fallback = "Mantengan una comunicación presente y honesta.",
)

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
    showLabel: Boolean = true,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(BWitchThemeTokens.dimens.spacingXs)
    ) {
        if (showLabel) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

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
