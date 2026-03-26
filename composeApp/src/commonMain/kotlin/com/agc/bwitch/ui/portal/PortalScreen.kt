package com.agc.bwitch.ui.portal

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
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
                    .padding(horizontal = BWitchThemeTokens.dimens.spacingMd)
                    .padding(top = 4.dp, bottom = BWitchThemeTokens.dimens.spacingSm),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                PortalHeader()

                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
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
    val headerAccent = Color(0xFF8B6D8F)
    val dividerColor = Color(0xFFBDAFB7)
    val titleColor = Color(0xFF2F2830)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 2.dp, bottom = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = "✦",
                style = MaterialTheme.typography.labelMedium.copy(
                    letterSpacing = 0.6.sp,
                ),
                color = headerAccent.copy(alpha = 0.72f),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "PORTAL",
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontFamily = bwitchDisplayFontFamily(),
                    fontWeight = FontWeight.SemiBold,
                    letterSpacing = 5.sp,
                    fontSize = 30.sp,
                    lineHeight = 36.sp,
                ),
                textAlign = TextAlign.Center,
                color = titleColor.copy(alpha = 0.98f),
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "✦",
                style = MaterialTheme.typography.labelMedium.copy(
                    letterSpacing = 0.6.sp,
                ),
                color = headerAccent.copy(alpha = 0.72f),
            )
        }
        HorizontalDivider(
            modifier = Modifier
                .fillMaxWidth(0.48f)
                .padding(top = 2.dp),
            thickness = 1.dp,
            color = dividerColor.copy(alpha = 0.56f),
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
    val cardShape = RoundedCornerShape(18.dp)
    val enabledContainer = Color(0xFFF6F0F2)
    val disabledContainer = Color(0xFFEDE5E8)
    val enabledTitleColor = Color(0xFF2F2830)
    val disabledTitleColor = Color(0xFF5E525C)
    val enabledSubtitleColor = Color(0xFF5A4B57)
    val disabledSubtitleColor = Color(0xFF7A6E77)

    BWitchCard(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 88.dp),
        onClick = if (isEnabled) onClick else null,
        enabled = isEnabled,
        shape = cardShape,
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled) {
                enabledContainer
            } else {
                disabledContainer
            },
            contentColor = enabledTitleColor,
            disabledContainerColor = disabledContainer,
            disabledContentColor = disabledTitleColor,
        ),
        contentPadding = PaddingValues(
            horizontal = BWitchThemeTokens.dimens.spacingMd + 2.dp,
            vertical = 10.dp,
        ),
        contentVerticalArrangement = Arrangement.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 68.dp),
        ) {
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
                    .padding(end = 86.dp)
                    .align(Alignment.CenterStart),
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Text(
                    text = module.title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 21.sp,
                        lineHeight = 27.sp,
                        fontWeight = if (isEnabled) FontWeight.SemiBold else FontWeight.Medium,
                        letterSpacing = 0.12.sp,
                    ),
                    color = if (isEnabled) {
                        enabledTitleColor
                    } else {
                        disabledTitleColor
                    },
                )
                Text(
                    text = module.subtitle,
                    style = MaterialTheme.typography.bodySmall.copy(
                        lineHeight = 17.sp,
                        letterSpacing = 0.16.sp,
                    ),
                    color = if (isEnabled) enabledSubtitleColor else disabledSubtitleColor,
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
    val strokeColor = if (isEnabled) {
        Color(0xFF7E6978).copy(alpha = 0.28f)
    } else {
        Color(0xFF8F7E89).copy(alpha = 0.2f)
    }
    val softColor = if (isEnabled) {
        Color(0xFFB79AAA).copy(alpha = 0.2f)
    } else {
        Color(0xFFC4B1BC).copy(alpha = 0.14f)
    }

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
