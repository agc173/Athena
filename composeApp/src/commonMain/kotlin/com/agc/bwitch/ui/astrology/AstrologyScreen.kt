package com.agc.bwitch.ui.astrology

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.drawWithCache
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
    val primary = MaterialTheme.colorScheme.primary
    val surface = MaterialTheme.colorScheme.surface
    val ornamentModifier = if (ornament == AstrologyCardOrnament.Synastry) {
        Modifier
    } else {
        Modifier.drawWithCache {
            onDrawBehind {
                drawAstrologyOrnament(
                    ornament = ornament,
                    primary = primary,
                    surface = surface,
                )
            }
        }
    }

    BWitchCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        contentPadding = PaddingValues(0.dp),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 148.dp)
                .then(ornamentModifier),
            contentAlignment = Alignment.CenterStart,
        ) {
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

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawAstrologyOrnament(
    ornament: AstrologyCardOrnament,
    primary: Color,
    surface: Color,
) {
    val strokeColor = primary.copy(alpha = 0.32f)
    val accentColor = primary.copy(alpha = 0.44f)
    val strokeWidth = size.minDimension * 0.022f
    val thinStrokeWidth = strokeWidth * 0.7f
    val tinyStrokeWidth = strokeWidth * 0.5f

    when (ornament) {
        AstrologyCardOrnament.Horoscope -> {
            val moonCenter = Offset(size.width * 0.88f, size.height * 0.36f)
            val moonRadius = size.minDimension * 0.23f
            drawCrescent(
                center = moonCenter,
                radius = moonRadius,
                color = strokeColor,
                width = strokeWidth,
                cutoutColor = surface,
            )

            val sunArcSize = size.minDimension * 0.62f
            drawArc(
                color = accentColor,
                startAngle = 204f,
                sweepAngle = 138f,
                useCenter = false,
                topLeft = Offset(size.width * 0.67f, size.height * 0.34f),
                size = androidx.compose.ui.geometry.Size(sunArcSize, sunArcSize),
                style = Stroke(width = thinStrokeWidth, cap = StrokeCap.Round),
            )

            val rayCenter = Offset(size.width * 0.98f, size.height * 0.65f)
            val rayLength = size.minDimension * 0.07f
            for (angle in listOf(218f, 248f, 278f, 308f)) {
                val radians = Math.toRadians(angle.toDouble())
                val start = Offset(
                    x = rayCenter.x + (kotlin.math.cos(radians) * rayLength * 0.45f).toFloat(),
                    y = rayCenter.y + (kotlin.math.sin(radians) * rayLength * 0.45f).toFloat(),
                )
                val end = Offset(
                    x = rayCenter.x + (kotlin.math.cos(radians) * rayLength).toFloat(),
                    y = rayCenter.y + (kotlin.math.sin(radians) * rayLength).toFloat(),
                )
                drawLine(
                    color = accentColor,
                    start = start,
                    end = end,
                    strokeWidth = tinyStrokeWidth,
                    cap = StrokeCap.Round,
                )
            }

            drawLinearStar(
                center = Offset(size.width * 0.74f, size.height * 0.42f),
                radius = size.minDimension * 0.034f,
                color = accentColor,
                width = tinyStrokeWidth,
            )
            drawLinearStar(
                center = Offset(size.width * 0.82f, size.height * 0.67f),
                radius = size.minDimension * 0.026f,
                color = strokeColor,
                width = tinyStrokeWidth,
            )
        }

        AstrologyCardOrnament.BirthEssence -> {
            val center = Offset(size.width * 0.82f, size.height * 0.52f)
            drawCircle(
                color = strokeColor,
                radius = size.minDimension * 0.3f,
                center = center,
                style = Stroke(width = thinStrokeWidth),
            )
            drawCircle(
                color = accentColor,
                radius = size.minDimension * 0.17f,
                center = center,
                style = Stroke(width = tinyStrokeWidth),
            )
            drawLinearStar(
                center = center,
                radius = size.minDimension * 0.038f,
                color = accentColor,
                width = tinyStrokeWidth,
            )

            val spiral = Path().apply {
                moveTo(size.width * 0.6f, size.height * 0.95f)
                cubicTo(
                    size.width * 0.98f,
                    size.height * 0.92f,
                    size.width * 0.99f,
                    size.height * 0.23f,
                    size.width * 0.79f,
                    size.height * 0.22f,
                )
                cubicTo(
                    size.width * 0.67f,
                    size.height * 0.34f,
                    size.width * 0.73f,
                    size.height * 0.67f,
                    size.width * 0.86f,
                    size.height * 0.56f,
                )
            }
            drawPath(
                path = spiral,
                color = strokeColor,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
            )

            val rootCurve = Path().apply {
                moveTo(size.width * 0.69f, size.height * 0.9f)
                cubicTo(
                    size.width * 0.74f,
                    size.height * 0.79f,
                    size.width * 0.82f,
                    size.height * 0.74f,
                    size.width * 0.89f,
                    size.height * 0.82f,
                )
            }
            drawPath(
                path = rootCurve,
                color = accentColor,
                style = Stroke(width = tinyStrokeWidth, cap = StrokeCap.Round)
            )
        }

        AstrologyCardOrnament.Synastry -> Unit
    }
}

private fun DrawScope.drawCrescent(
    center: Offset,
    radius: Float,
    color: Color,
    width: Float,
    cutoutColor: Color,
) {
    drawCircle(
        color = color,
        radius = radius,
        center = center,
        style = Stroke(width = width),
    )
    drawArc(
        color = cutoutColor,
        startAngle = 90f,
        sweepAngle = 180f,
        useCenter = false,
        topLeft = Offset(center.x - radius * 0.95f, center.y - radius),
        size = androidx.compose.ui.geometry.Size(width = radius * 1.9f, height = radius * 2f),
        style = Stroke(width = width * 1.45f, cap = StrokeCap.Round),
    )
}

private fun DrawScope.drawLinearStar(
    center: Offset,
    radius: Float,
    color: Color,
    width: Float,
) {
    drawLine(
        color = color,
        start = Offset(center.x - radius, center.y),
        end = Offset(center.x + radius, center.y),
        strokeWidth = width,
        cap = StrokeCap.Round,
    )
    drawLine(
        color = color,
        start = Offset(center.x, center.y - radius),
        end = Offset(center.x, center.y + radius),
        strokeWidth = width,
        cap = StrokeCap.Round,
    )
}
