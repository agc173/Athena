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
                    color = primary.copy(alpha = 0.28f),
                    radius = size.minDimension * 0.56f,
                    center = Offset(size.width * 1.02f, size.height * 0.2f)
                )
                drawCircle(
                    color = primary.copy(alpha = 0.52f),
                    radius = size.minDimension * 0.22f,
                    center = Offset(size.width * 0.83f, size.height * 0.38f)
                )
                drawCircle(
                    color = primary.copy(alpha = 0.5f),
                    radius = size.minDimension * 0.24f,
                    center = Offset(size.width * 0.85f, size.height * 0.68f)
                )
                drawCircle(
                    color = surface,
                    radius = size.minDimension * 0.2f,
                    center = Offset(size.width * 0.94f, size.height * 0.66f)
                )
                drawCircle(
                    color = primary.copy(alpha = 0.58f),
                    radius = size.minDimension * 0.028f,
                    center = Offset(size.width * 0.7f, size.height * 0.56f)
                )
                drawCircle(
                    color = primary.copy(alpha = 0.52f),
                    radius = size.minDimension * 0.023f,
                    center = Offset(size.width * 0.77f, size.height * 0.42f)
                )
                drawCircle(
                    color = primary.copy(alpha = 0.48f),
                    radius = size.minDimension * 0.02f,
                    center = Offset(size.width * 0.86f, size.height * 0.52f)
                )
            }

            AstrologyCardOrnament.BirthEssence -> {
                val haloBrush = Brush.radialGradient(
                    colors = listOf(
                        primary.copy(alpha = 0.55f),
                        primary.copy(alpha = 0.28f),
                        Color.Transparent,
                    ),
                    center = Offset(size.width * 0.8f, size.height * 0.5f),
                    radius = size.minDimension * 0.78f,
                )
                drawCircle(
                    brush = haloBrush,
                    radius = size.minDimension * 0.78f,
                    center = Offset(size.width * 0.8f, size.height * 0.5f)
                )
                drawCircle(
                    color = primary.copy(alpha = 0.48f),
                    radius = size.minDimension * 0.25f,
                    center = Offset(size.width * 0.8f, size.height * 0.5f)
                )
                drawCircle(
                    color = primary.copy(alpha = 0.66f),
                    radius = size.minDimension * 0.13f,
                    center = Offset(size.width * 0.8f, size.height * 0.5f)
                )

                val spiral = Path().apply {
                    moveTo(size.width * 0.54f, size.height * 0.94f)
                    cubicTo(
                        size.width * 0.98f,
                        size.height * 0.88f,
                        size.width * 0.96f,
                        size.height * 0.14f,
                        size.width * 0.75f,
                        size.height * 0.26f,
                    )
                    cubicTo(
                        size.width * 0.64f,
                        size.height * 0.36f,
                        size.width * 0.72f,
                        size.height * 0.61f,
                        size.width * 0.86f,
                        size.height * 0.54f,
                    )
                }
                drawPath(
                    path = spiral,
                    color = primary.copy(alpha = 0.34f),
                    style = Stroke(width = size.minDimension * 0.05f, cap = StrokeCap.Round)
                )
            }

            AstrologyCardOrnament.Synastry -> {
                drawCircle(
                    color = primary.copy(alpha = 0.44f),
                    radius = size.minDimension * 0.31f,
                    center = Offset(size.width * 0.72f, size.height * 0.54f)
                )
                drawCircle(
                    color = primary.copy(alpha = 0.44f),
                    radius = size.minDimension * 0.31f,
                    center = Offset(size.width * 0.9f, size.height * 0.54f)
                )
                drawCircle(
                    color = primary.copy(alpha = 0.62f),
                    radius = size.minDimension * 0.16f,
                    center = Offset(size.width * 0.81f, size.height * 0.54f)
                )

                val bridge = Path().apply {
                    moveTo(size.width * 0.66f, size.height * 0.8f)
                    cubicTo(
                        size.width * 0.74f,
                        size.height * 0.9f,
                        size.width * 0.88f,
                        size.height * 0.25f,
                        size.width * 0.96f,
                        size.height * 0.34f,
                    )
                    lineTo(size.width * 0.95f, size.height * 0.44f)
                    cubicTo(
                        size.width * 0.89f,
                        size.height * 0.37f,
                        size.width * 0.77f,
                        size.height * 0.8f,
                        size.width * 0.69f,
                        size.height * 0.73f,
                    )
                    close()
                }
                drawPath(
                    path = bridge,
                    color = primary.copy(alpha = 0.36f)
                )
            }
        }
    }
}
