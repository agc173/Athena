package com.agc.bwitch.ui.portal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agc.bwitch.presentation.navigation.Destination
import com.agc.bwitch.ui.common.AppScaffold
import com.agc.bwitch.ui.common.designsystem.BWitchCard
import com.agc.bwitch.ui.common.designsystem.BWitchScreen
import com.agc.bwitch.ui.theme.BWitchThemeTokens

@Composable
fun PortalScreen(
    contentPadding: PaddingValues,
    onNavigate: (Destination) -> Unit,
    modifier: Modifier = Modifier
) {
    val activeModules = listOf(
        PortalItemConfig(
            title = "Perfil",
            subtitle = "Tu energía y ajustes",
            symbol = "◐",
            destination = Destination.UserProfile,
            enabled = true
        ),
        PortalItemConfig(
            title = "Astrología",
            subtitle = "Carta, signos y compatibilidad",
            symbol = "✶",
            destination = Destination.Astrology,
            enabled = true
        ),
        PortalItemConfig(
            title = "Guía",
            subtitle = "Tarot, oráculo y péndulo",
            symbol = "◈",
            destination = Destination.Guide,
            enabled = true
        ),
    )

    val upcomingModules = listOf(
        PortalItemConfig(
            title = "Comunidad",
            subtitle = "Encuentros y conversaciones",
            symbol = "◍",
            destination = null,
            enabled = false
        ),
        PortalItemConfig(
            title = "Tienda",
            subtitle = "Créditos, suscripciones y mazos",
            symbol = "◔",
            destination = null,
            enabled = false
        ),
        PortalItemConfig(
            title = "Rituales",
            subtitle = "Prácticas y momentos especiales",
            symbol = "✦",
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
        BWitchScreen(
            contentPadding = scaffoldPadding,
            modifier = Modifier.padding(contentPadding),
            verticalArrangement = Arrangement.spacedBy(BWitchThemeTokens.dimens.spacingLg),
        ) {
            PortalHeroHeader()

            Column(verticalArrangement = Arrangement.spacedBy(BWitchThemeTokens.dimens.spacingMd)) {
                activeModules.forEach { module ->
                    PortalModuleCard(
                        module = module,
                        onClick = {
                            val destination = module.destination ?: return@PortalModuleCard
                            onNavigate(destination)
                        },
                    )
                }
            }

            Divider(modifier = Modifier.alpha(0.25f))
            PortalSectionLabel(label = "Próximamente")

            Column(verticalArrangement = Arrangement.spacedBy(BWitchThemeTokens.dimens.spacingMd)) {
                upcomingModules.forEach { module ->
                    PortalModuleCard(
                        module = module,
                        onClick = null,
                    )
                }
            }
        }
    }
}

@Composable
private fun PortalHeroHeader(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(BWitchThemeTokens.dimens.spacingXs),
    ) {
        Text(
            text = "PORTAL",
            style = MaterialTheme.typography.displaySmall.copy(
                letterSpacing = 6.sp,
                fontWeight = FontWeight.SemiBold,
            ),
            textAlign = TextAlign.Center,
        )
        Text(
            text = "Elige un camino",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PortalSectionLabel(label: String, modifier: Modifier = Modifier) {
    Text(
        text = label.uppercase(),
        modifier = modifier,
        style = MaterialTheme.typography.labelLarge.copy(letterSpacing = 2.sp),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}

@Composable
private fun PortalModuleCard(
    module: PortalItemConfig,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val isEnabled = module.enabled && module.destination != null && onClick != null

    BWitchCard(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 156.dp),
        onClick = if (isEnabled) onClick else null,
        enabled = isEnabled,
        contentPadding = PaddingValues(
            horizontal = BWitchThemeTokens.dimens.spacingLg,
            vertical = BWitchThemeTokens.dimens.spacingXl,
        ),
        contentVerticalArrangement = Arrangement.Center,
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Column(
                verticalArrangement = Arrangement.spacedBy(BWitchThemeTokens.dimens.spacingXs),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(end = BWitchThemeTokens.dimens.spacingXl),
            ) {
                Text(
                    text = module.title,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = module.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            Row(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(top = BWitchThemeTokens.dimens.spacingLg),
                verticalAlignment = Alignment.Bottom,
            ) {
                Text(
                    text = module.symbol,
                    style = MaterialTheme.typography.displayMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .alpha(if (isEnabled) 0.14f else 0.08f)
                        .size(56.dp),
                )
            }
        }
    }
}
