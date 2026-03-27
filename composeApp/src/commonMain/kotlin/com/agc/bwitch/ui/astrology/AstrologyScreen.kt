package com.agc.bwitch.ui.astrology

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.agc.bwitch.presentation.navigation.Destination
import com.agc.bwitch.ui.common.designsystem.BWitchCard
import com.agc.bwitch.ui.common.designsystem.BWitchScreen
import com.agc.bwitch.ui.common.designsystem.BWitchSectionHeader

@Composable
fun AstrologyScreen(
    contentPadding: PaddingValues,
    onNavigate: (Destination) -> Unit
) {
    BWitchScreen(
        contentPadding = contentPadding,
        modifier = Modifier.verticalScroll(rememberScrollState())
    ) {
        BWitchSectionHeader(
            title = "Tu cielo interior",
            subtitle = "Explora tu esencia y los ritmos que te acompañan",
            titleStyle = MaterialTheme.typography.headlineSmall,
            subtitleStyle = MaterialTheme.typography.bodyLarge,
        )

        AstrologyFeatureCard(
            title = "Horóscopo",
            subtitle = "Tu energía de hoy… y lo que viene",
            ornament = AstrologyCardOrnament.Horoscope,
            onClick = { onNavigate(Destination.HoroscopeDaily()) },
            modifier = Modifier.heightIn(min = 168.dp)
        )

        AstrologyFeatureCard(
            title = "Esencia natal",
            subtitle = "La huella de tu nacimiento",
            ornament = AstrologyCardOrnament.BirthEssence,
            onClick = { onNavigate(Destination.BirthChart) }
        )

        AstrologyFeatureCard(
            title = "Sinastría",
            subtitle = "La energía entre dos",
            ornament = AstrologyCardOrnament.Synastry,
            onClick = { onNavigate(Destination.Synastry) }
        )
    }
}

private enum class AstrologyCardOrnament {
    Horoscope,
    BirthEssence,
    Synastry,
}

@Composable
private fun AstrologyFeatureCard(
    title: String,
    subtitle: String,
    ornament: AstrologyCardOrnament,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BWitchCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Box(modifier = Modifier.fillMaxWidth().heightIn(min = 132.dp)) {
            CardOrnament(
                ornament = ornament,
                modifier = Modifier.fillMaxSize(),
            )

            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun CardOrnament(
    ornament: AstrologyCardOrnament,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surface

    Canvas(modifier = modifier) {
        when (ornament) {
            AstrologyCardOrnament.Horoscope -> {
                drawCircle(
                    color = primary.copy(alpha = 0.11f),
                    radius = size.minDimension * 0.4f,
                    center = Offset(size.width * 0.98f, size.height * 0.12f)
                )
                drawArc(
                    color = primary.copy(alpha = 0.15f),
                    startAngle = 18f,
                    sweepAngle = 300f,
                    useCenter = false,
                    topLeft = Offset(size.width * 0.68f, size.height * -0.24f),
                    size = Size(size.width * 0.52f, size.width * 0.52f),
                    style = Stroke(width = size.minDimension * 0.045f, cap = StrokeCap.Round)
                )
                drawCircle(
                    color = primary.copy(alpha = 0.14f),
                    radius = size.minDimension * 0.2f,
                    center = Offset(size.width * 0.86f, size.height * 0.68f)
                )
                drawCircle(
                    color = surface,
                    radius = size.minDimension * 0.2f,
                    center = Offset(size.width * 0.93f, size.height * 0.66f)
                )
                drawCircle(
                    color = primary.copy(alpha = 0.17f),
                    radius = size.minDimension * 0.018f,
                    center = Offset(size.width * 0.71f, size.height * 0.56f)
                )
                drawCircle(
                    color = primary.copy(alpha = 0.13f),
                    radius = size.minDimension * 0.014f,
                    center = Offset(size.width * 0.78f, size.height * 0.48f)
                )
                drawCircle(
                    color = primary.copy(alpha = 0.11f),
                    radius = size.minDimension * 0.012f,
                    center = Offset(size.width * 0.84f, size.height * 0.57f)
                )
            }

            AstrologyCardOrnament.BirthEssence -> {
                val haloBrush = Brush.radialGradient(
                    colors = listOf(primary.copy(alpha = 0.16f), Color.Transparent),
                    center = Offset(size.width * 0.82f, size.height * 0.35f),
                    radius = size.minDimension * 0.55f,
                )
                drawCircle(
                    brush = haloBrush,
                    radius = size.minDimension * 0.55f,
                    center = Offset(size.width * 0.82f, size.height * 0.35f)
                )
                drawCircle(
                    color = primary.copy(alpha = 0.12f),
                    radius = size.minDimension * 0.12f,
                    center = Offset(size.width * 0.82f, size.height * 0.35f)
                )

                val spiral = Path().apply {
                    moveTo(size.width * 0.6f, size.height * 1.05f)
                    cubicTo(
                        size.width * 1.05f,
                        size.height * 0.95f,
                        size.width * 0.95f,
                        size.height * 0.1f,
                        size.width * 0.74f,
                        size.height * 0.26f,
                    )
                    cubicTo(
                        size.width * 0.62f,
                        size.height * 0.35f,
                        size.width * 0.69f,
                        size.height * 0.57f,
                        size.width * 0.81f,
                        size.height * 0.52f,
                    )
                }
                drawPath(
                    path = spiral,
                    color = primary.copy(alpha = 0.14f),
                    style = Stroke(width = size.minDimension * 0.04f, cap = StrokeCap.Round)
                )
            }

            AstrologyCardOrnament.Synastry -> {
                drawCircle(
                    color = primary.copy(alpha = 0.12f),
                    radius = size.minDimension * 0.36f,
                    center = Offset(size.width * 0.73f, size.height * 0.52f),
                    style = Stroke(width = size.minDimension * 0.05f)
                )
                drawCircle(
                    color = primary.copy(alpha = 0.12f),
                    radius = size.minDimension * 0.3f,
                    center = Offset(size.width * 0.9f, size.height * 0.52f),
                    style = Stroke(width = size.minDimension * 0.05f)
                )

                val bridge = Path().apply {
                    moveTo(size.width * 0.66f, size.height * 0.8f)
                    cubicTo(
                        size.width * 0.78f,
                        size.height * 0.93f,
                        size.width * 0.86f,
                        size.height * 0.18f,
                        size.width * 0.98f,
                        size.height * 0.29f,
                    )
                }
                drawPath(
                    path = bridge,
                    color = primary.copy(alpha = 0.14f),
                    style = Stroke(width = size.minDimension * 0.04f, cap = StrokeCap.Round)
                )
            }
        }
    }
}
