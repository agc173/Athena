package com.agc.bwitch.ui.astrology

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
        contentPadding = PaddingValues(0.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 148.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            CardOrnament(
                ornament = ornament,
                modifier = Modifier.fillMaxSize(),
            )

            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 18.dp),
            ) {
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
                    color = primary.copy(alpha = 0.22f),
                    radius = size.minDimension * 0.46f,
                    center = Offset(size.width * 0.89f, size.height * 0.18f)
                )
                drawArc(
                    color = primary.copy(alpha = 0.32f),
                    startAngle = 18f,
                    sweepAngle = 300f,
                    useCenter = false,
                    topLeft = Offset(size.width * 0.56f, size.height * -0.06f),
                    size = Size(size.width * 0.56f, size.width * 0.56f),
                    style = Stroke(width = size.minDimension * 0.07f, cap = StrokeCap.Round)
                )
                drawCircle(
                    color = primary.copy(alpha = 0.3f),
                    radius = size.minDimension * 0.26f,
                    center = Offset(size.width * 0.82f, size.height * 0.64f)
                )
                drawCircle(
                    color = surface,
                    radius = size.minDimension * 0.22f,
                    center = Offset(size.width * 0.9f, size.height * 0.64f)
                )
                drawCircle(
                    color = primary.copy(alpha = 0.5f),
                    radius = size.minDimension * 0.03f,
                    center = Offset(size.width * 0.65f, size.height * 0.58f)
                )
                drawCircle(
                    color = primary.copy(alpha = 0.45f),
                    radius = size.minDimension * 0.025f,
                    center = Offset(size.width * 0.75f, size.height * 0.44f)
                )
                drawCircle(
                    color = primary.copy(alpha = 0.4f),
                    radius = size.minDimension * 0.022f,
                    center = Offset(size.width * 0.83f, size.height * 0.56f)
                )
            }

            AstrologyCardOrnament.BirthEssence -> {
                val haloBrush = Brush.radialGradient(
                    colors = listOf(primary.copy(alpha = 0.38f), Color.Transparent),
                    center = Offset(size.width * 0.8f, size.height * 0.4f),
                    radius = size.minDimension * 0.7f,
                )
                drawCircle(
                    brush = haloBrush,
                    radius = size.minDimension * 0.7f,
                    center = Offset(size.width * 0.8f, size.height * 0.4f)
                )
                drawCircle(
                    color = primary.copy(alpha = 0.48f),
                    radius = size.minDimension * 0.17f,
                    center = Offset(size.width * 0.8f, size.height * 0.4f)
                )

                val spiral = Path().apply {
                    moveTo(size.width * 0.56f, size.height * 1.02f)
                    cubicTo(
                        size.width * 1.03f,
                        size.height * 0.96f,
                        size.width * 0.95f,
                        size.height * 0.12f,
                        size.width * 0.73f,
                        size.height * 0.25f,
                    )
                    cubicTo(
                        size.width * 0.61f,
                        size.height * 0.35f,
                        size.width * 0.7f,
                        size.height * 0.59f,
                        size.width * 0.83f,
                        size.height * 0.51f,
                    )
                }
                drawPath(
                    path = spiral,
                    color = primary.copy(alpha = 0.44f),
                    style = Stroke(width = size.minDimension * 0.055f, cap = StrokeCap.Round)
                )
            }

            AstrologyCardOrnament.Synastry -> {
                drawCircle(
                    color = primary.copy(alpha = 0.42f),
                    radius = size.minDimension * 0.35f,
                    center = Offset(size.width * 0.72f, size.height * 0.52f),
                    style = Stroke(width = size.minDimension * 0.068f)
                )
                drawCircle(
                    color = primary.copy(alpha = 0.42f),
                    radius = size.minDimension * 0.3f,
                    center = Offset(size.width * 0.88f, size.height * 0.52f),
                    style = Stroke(width = size.minDimension * 0.068f)
                )

                val bridge = Path().apply {
                    moveTo(size.width * 0.68f, size.height * 0.8f)
                    cubicTo(
                        size.width * 0.78f,
                        size.height * 0.94f,
                        size.width * 0.9f,
                        size.height * 0.15f,
                        size.width * 1.0f,
                        size.height * 0.28f,
                    )
                }
                drawPath(
                    path = bridge,
                    color = primary.copy(alpha = 0.46f),
                    style = Stroke(width = size.minDimension * 0.055f, cap = StrokeCap.Round)
                )
            }
        }
    }
}
