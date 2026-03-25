package com.agc.bwitch.ui.portal

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.matchParentSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agc.bwitch.presentation.navigation.Destination
import com.agc.bwitch.ui.common.AppScaffold
import com.agc.bwitch.ui.common.designsystem.BWitchCard
import com.agc.bwitch.ui.theme.BWitchThemeTokens

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
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(scaffoldPadding)
                .padding(contentPadding)
                .padding(horizontal = BWitchThemeTokens.dimens.spacingMd)
                .padding(top = BWitchThemeTokens.dimens.spacingXs, bottom = BWitchThemeTokens.dimens.spacingSm),
            verticalArrangement = Arrangement.spacedBy(BWitchThemeTokens.dimens.spacingSm),
        ) {
            PortalHeader()

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

@Composable
private fun PortalHeader(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 2.dp, bottom = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Text(
            text = "PORTAL",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 8.sp,
            ),
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Elige tu camino",
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.4.sp),
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.72f),
            textAlign = TextAlign.Center,
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
    val cardShape = RoundedCornerShape(22.dp)

    BWitchCard(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 104.dp),
        onClick = if (isEnabled) onClick else null,
        enabled = isEnabled,
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.96f)
            } else {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f)
            },
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f),
            disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.92f),
        ),
        contentPadding = PaddingValues(
            horizontal = BWitchThemeTokens.dimens.spacingLg,
            vertical = BWitchThemeTokens.dimens.spacingMd,
        ),
        contentVerticalArrangement = Arrangement.spacedBy(6.dp),
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
                    .padding(end = 88.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = module.title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = if (isEnabled) FontWeight.SemiBold else FontWeight.Medium,
                        letterSpacing = 0.2.sp,
                    ),
                    color = if (isEnabled) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.82f)
                    },
                )
                Text(
                    text = module.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(
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
    val strokeColor = MaterialTheme.colorScheme.onSurface.copy(alpha = if (isEnabled) 0.16f else 0.1f)
    val softColor = MaterialTheme.colorScheme.primary.copy(alpha = if (isEnabled) 0.12f else 0.08f)

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
