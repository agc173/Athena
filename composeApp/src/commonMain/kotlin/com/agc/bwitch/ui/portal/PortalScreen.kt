package com.agc.bwitch.ui.portal

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agc.bwitch.presentation.navigation.Destination
import com.agc.bwitch.ui.common.AppScaffold
import com.agc.bwitch.ui.common.designsystem.BWitchCard
import com.agc.bwitch.ui.theme.BWitchThemeTokens
import com.agc.bwitch.ui.theme.bwitchDisplayFontFamily

@Composable
fun PortalScreen(
    contentPadding: PaddingValues,
    onNavigate: (Destination) -> Unit,
    modifier: Modifier = Modifier
) {
    val portalModules = listOf(
        PortalItemConfig(
            title = "Perfil",
            subtitle = "Tu energía y ajustes",
            ornament = PortalOrnament.PROFILE,
            destination = Destination.UserProfile,
            enabled = true
        ),
        PortalItemConfig(
            title = "Astrología",
            subtitle = "Carta, signos y compatibilidad",
            ornament = PortalOrnament.ASTROLOGY,
            destination = Destination.Astrology,
            enabled = true
        ),
        PortalItemConfig(
            title = "Guía",
            subtitle = "Tarot, oráculo y péndulo",
            ornament = PortalOrnament.GUIDE,
            destination = Destination.Guide,
            enabled = true
        ),
        PortalItemConfig(
            title = "Comunidad",
            subtitle = "Encuentros y conversaciones",
            ornament = PortalOrnament.COMMUNITY,
            destination = null,
            enabled = false
        ),
        PortalItemConfig(
            title = "Tienda",
            subtitle = "Créditos, suscripciones y mazos",
            ornament = PortalOrnament.STORE,
            destination = null,
            enabled = false
        ),
        PortalItemConfig(
            title = "Rituales",
            subtitle = "Prácticas y momentos especiales",
            ornament = PortalOrnament.RITUALS,
            destination = null,
            enabled = false
        ),
    )

    AppScaffold(
        title = "BWitch",
        canGoBack = false,
        onBack = {},
        modifier = modifier
    ) { scaffoldPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(scaffoldPadding)
                    .padding(contentPadding)
                    .padding(horizontal = BWitchThemeTokens.dimens.spacingMd)
                    .padding(top = BWitchThemeTokens.dimens.spacingXs, bottom = BWitchThemeTokens.dimens.spacingSm),
                verticalArrangement = Arrangement.spacedBy(BWitchThemeTokens.dimens.spacingSm),
            ) {
                PortalHeader()
                Spacer(modifier = Modifier.height(2.dp))

                Column(verticalArrangement = Arrangement.spacedBy(BWitchThemeTokens.dimens.spacingSm)) {
                    portalModules.forEach { module ->
                        PortalModuleCard(
                            module = module,
                            onClick = {
                                val destination = module.destination ?: return@PortalModuleCard
                                onNavigate(destination)
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PortalHeader(modifier: Modifier = Modifier) {
    val colorScheme = MaterialTheme.colorScheme

    Column(
        modifier = modifier
            .fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            HeaderOrnament(color = colorScheme.primary.copy(alpha = 0.62f))
            Text(
                text = "PORTAL",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontFamily = bwitchDisplayFontFamily(),
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 34.sp,
                    letterSpacing = 3.8.sp,
                    lineHeight = 40.sp,
                ),
                color = colorScheme.onSurface.copy(alpha = 0.96f),
                textAlign = TextAlign.Center,
            )
            HeaderOrnament(color = colorScheme.primary.copy(alpha = 0.62f))
        }

        Box(
            modifier = Modifier
                .width(92.dp)
                .heightIn(min = 1.dp)
                .background(colorScheme.outlineVariant.copy(alpha = 0.7f)),
        )
    }
}

@Composable
private fun HeaderOrnament(
    color: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(
        modifier = modifier
            .size(width = 34.dp, height = 12.dp),
    ) {
        val centerY = size.height / 2f
        drawLine(
            color = color.copy(alpha = 0.42f),
            start = Offset(0f, centerY),
            end = Offset(size.width, centerY),
            strokeWidth = 1.6f,
            cap = StrokeCap.Round,
        )
        drawCircle(
            color = color,
            radius = 2.4f,
            center = Offset(size.width * 0.5f, centerY),
        )
    }
}

@Composable
private fun PortalModuleCard(
    module: PortalItemConfig,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val isEnabled = module.enabled && module.destination != null && onClick != null
    val colorScheme = MaterialTheme.colorScheme
    val cardShape = RoundedCornerShape(16.dp)
    val enabledContainer = lerp(colorScheme.surface, colorScheme.surfaceVariant, 0.36f)
    val disabledContainer = lerp(colorScheme.surface, colorScheme.surfaceVariant, 0.5f).copy(alpha = 0.86f)

    BWitchCard(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 90.dp)
            .border(
                width = 1.dp,
                color = if (isEnabled) {
                    colorScheme.outlineVariant.copy(alpha = 0.46f)
                } else {
                    colorScheme.outlineVariant.copy(alpha = 0.24f)
                },
                shape = cardShape,
            ),
        onClick = if (isEnabled) onClick else null,
        enabled = isEnabled,
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled) {
                enabledContainer
            } else {
                disabledContainer
            },
            contentColor = colorScheme.onSurface,
            disabledContainerColor = disabledContainer,
            disabledContentColor = colorScheme.onSurfaceVariant.copy(alpha = 0.84f),
        ),
        contentPadding = PaddingValues(
            horizontal = 20.dp,
            vertical = 12.dp,
        ),
        contentVerticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            CardOrnament(
                ornament = module.ornament,
                isEnabled = isEnabled,
                modifier = Modifier
                    .matchParentSize()
                    .clip(cardShape),
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 62.dp)
                    .padding(end = 88.dp),
                verticalArrangement = Arrangement.Center,
            ) {
                Text(
                    text = module.title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = if (isEnabled) FontWeight.SemiBold else FontWeight.Medium,
                        lineHeight = 29.sp,
                        letterSpacing = 0.15.sp,
                    ),
                    color = if (isEnabled) {
                        colorScheme.onSurface
                    } else {
                        colorScheme.onSurface.copy(alpha = 0.82f)
                    },
                )
                Text(
                    text = module.subtitle,
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Normal,
                        letterSpacing = 0.18.sp,
                    ),
                    color = colorScheme.onSurfaceVariant.copy(
                        alpha = if (isEnabled) 0.88f else 0.72f,
                    ),
                )
            }
        }
    }
}

@Composable
private fun CardOrnament(
    ornament: PortalOrnament,
    isEnabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val strokeColor = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isEnabled) 0.12f else 0.08f)
    val softColor = MaterialTheme.colorScheme.primary.copy(alpha = if (isEnabled) 0.09f else 0.06f)

    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val centerX = w * 0.8f
        val centerY = h * 0.53f

        when (ornament) {
            PortalOrnament.PROFILE -> {
                drawCircle(
                    color = softColor,
                    radius = h * 0.38f,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = 3f),
                )
                drawCircle(
                    color = strokeColor,
                    radius = h * 0.19f,
                    center = Offset(centerX, centerY - h * 0.08f),
                    style = Stroke(width = 2.2f),
                )
                drawArc(
                    color = strokeColor,
                    startAngle = 200f,
                    sweepAngle = 140f,
                    useCenter = false,
                    topLeft = Offset(centerX - h * 0.22f, centerY - h * 0.03f),
                    size = Size(h * 0.44f, h * 0.34f),
                    style = Stroke(width = 3f, cap = StrokeCap.Round),
                )
            }

            PortalOrnament.ASTROLOGY -> {
                drawCircle(
                    color = strokeColor,
                    radius = h * 0.36f,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = 2.4f),
                )
                drawCircle(
                    color = strokeColor,
                    radius = h * 0.26f,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = 1.8f),
                )
                drawCircle(color = softColor, radius = 5f, center = Offset(centerX - h * 0.2f, centerY - h * 0.14f))
                drawCircle(color = strokeColor, radius = 4f, center = Offset(centerX + h * 0.13f, centerY + h * 0.06f))
                drawLine(
                    color = strokeColor,
                    start = Offset(centerX - h * 0.2f, centerY - h * 0.14f),
                    end = Offset(centerX + h * 0.13f, centerY + h * 0.06f),
                    strokeWidth = 1.6f,
                )
            }

            PortalOrnament.GUIDE -> {
                drawRoundRect(
                    color = strokeColor,
                    topLeft = Offset(centerX - h * 0.28f, centerY - h * 0.24f),
                    size = Size(h * 0.46f, h * 0.42f),
                    style = Stroke(width = 2.2f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(24f, 24f),
                )
                drawLine(
                    color = strokeColor,
                    start = Offset(centerX - h * 0.05f, centerY - h * 0.26f),
                    end = Offset(centerX + h * 0.18f, centerY + h * 0.18f),
                    strokeWidth = 2f,
                )
                drawLine(
                    color = strokeColor,
                    start = Offset(centerX + h * 0.18f, centerY - h * 0.24f),
                    end = Offset(centerX - h * 0.05f, centerY + h * 0.2f),
                    strokeWidth = 2f,
                )
                drawCircle(
                    color = softColor,
                    radius = 6f,
                    center = Offset(centerX + h * 0.26f, centerY - h * 0.18f),
                )
            }

            PortalOrnament.COMMUNITY -> {
                drawCircle(
                    color = strokeColor,
                    radius = h * 0.16f,
                    center = Offset(centerX - h * 0.14f, centerY),
                    style = Stroke(width = 2.6f),
                )
                drawCircle(
                    color = strokeColor,
                    radius = h * 0.16f,
                    center = Offset(centerX + h * 0.02f, centerY),
                    style = Stroke(width = 2.6f),
                )
                drawCircle(
                    color = softColor,
                    radius = h * 0.16f,
                    center = Offset(centerX + h * 0.18f, centerY),
                    style = Stroke(width = 2.6f),
                )
            }

            PortalOrnament.STORE -> {
                drawArc(
                    color = strokeColor,
                    startAngle = 186f,
                    sweepAngle = 168f,
                    useCenter = false,
                    topLeft = Offset(centerX - h * 0.3f, centerY - h * 0.26f),
                    size = Size(h * 0.58f, h * 0.58f),
                    style = Stroke(width = 2.6f, cap = StrokeCap.Round),
                )
                drawCircle(
                    color = softColor,
                    radius = h * 0.14f,
                    center = Offset(centerX, centerY + h * 0.02f),
                    style = Stroke(width = 2.2f),
                )
                drawCircle(
                    color = strokeColor,
                    radius = 4.5f,
                    center = Offset(centerX, centerY + h * 0.02f),
                )
            }

            PortalOrnament.RITUALS -> {
                drawCircle(
                    color = softColor,
                    radius = h * 0.34f,
                    center = Offset(centerX, centerY),
                    style = Stroke(width = 2.2f),
                )
                drawRoundRect(
                    color = strokeColor,
                    topLeft = Offset(centerX - h * 0.06f, centerY - h * 0.15f),
                    size = Size(h * 0.12f, h * 0.34f),
                    style = Stroke(width = 2f),
                    cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f),
                )
                drawArc(
                    color = strokeColor,
                    startAngle = 210f,
                    sweepAngle = 120f,
                    useCenter = true,
                    topLeft = Offset(centerX - h * 0.08f, centerY - h * 0.28f),
                    size = Size(h * 0.16f, h * 0.18f),
                )
            }
        }
    }
}
