package com.agc.bwitch.ui.rituals

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import bwitch.composeapp.generated.resources.Res
import bwitch.composeapp.generated.resources.horoscope_ornament
import bwitch.composeapp.generated.resources.oracle_ornament
import bwitch.composeapp.generated.resources.pendulum_ornament
import com.agc.bwitch.ui.common.designsystem.BWitchCard
import com.agc.bwitch.ui.common.designsystem.BWitchScreen
import com.agc.bwitch.ui.common.designsystem.BWitchSectionHeader
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

private val RitualsCardHeight = 168.dp

@Composable
fun RitualsPlaceholderScreen(
    contentPadding: PaddingValues,
    onOpenDailyRitual: () -> Unit,
    modifier: Modifier = Modifier,
) {
    BWitchScreen(
        contentPadding = contentPadding,
        modifier = modifier,
    ) {
        BWitchSectionHeader(
            title = "Rituales",
            subtitle = "Activa tu energía con pequeñas prácticas",
        )

        RitualsFeatureCard(
            title = "Ritual del día",
            subtitle = "Hoy: claridad interior",
            onClick = onOpenDailyRitual,
            ornamentType = RitualsCardOrnament.DailyRitual,
            modifier = Modifier.height(RitualsCardHeight),
        )

        RitualsFeatureCard(
            title = "Rituales",
            subtitle = "Prácticas para atraer lo que deseas",
            onClick = { },
            ornamentType = RitualsCardOrnament.Rituals,
            modifier = Modifier.height(RitualsCardHeight),
        )

        RitualsFeatureCard(
            title = "Hábitos",
            subtitle = "Pequeñas acciones que transforman tu energía",
            onClick = { },
            ornamentType = RitualsCardOrnament.Habits,
            modifier = Modifier.height(RitualsCardHeight),
        )
    }
}

private enum class RitualsCardOrnament {
    DailyRitual,
    Rituals,
    Habits,
}

private data class RitualsCardOrnamentConfig(
    val resource: DrawableResource,
    val width: Dp,
    val offsetX: Dp,
    val alignment: Alignment,
    val alpha: Float,
)

private fun RitualsCardOrnament.config(): RitualsCardOrnamentConfig =
    when (this) {
        RitualsCardOrnament.DailyRitual -> RitualsCardOrnamentConfig(
            // TODO: Replace with Res.drawable.ritual_daily_ornament when the raster asset is available.
            resource = Res.drawable.horoscope_ornament,
            width = 250.dp,
            offsetX = 74.dp,
            alignment = Alignment.CenterEnd,
            alpha = 0.22f,
        )
        RitualsCardOrnament.Rituals -> RitualsCardOrnamentConfig(
            // TODO: Replace with Res.drawable.rituals_ornament when the raster asset is available.
            resource = Res.drawable.oracle_ornament,
            width = 220.dp,
            offsetX = 56.dp,
            alignment = Alignment.CenterEnd,
            alpha = 0.24f,
        )
        RitualsCardOrnament.Habits -> RitualsCardOrnamentConfig(
            // TODO: Replace with Res.drawable.habits_ornament when the raster asset is available.
            resource = Res.drawable.pendulum_ornament,
            width = 246.dp,
            offsetX = 52.dp,
            alignment = Alignment.CenterEnd,
            alpha = 0.21f,
        )
    }

@Composable
private fun RitualsFeatureCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    ornamentType: RitualsCardOrnament? = null,
    modifier: Modifier = Modifier,
) {
    BWitchCard(
        modifier = modifier.fillMaxWidth(),
        onClick = onClick,
        contentPadding = PaddingValues(0.dp),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.CenterStart,
        ) {
            ornamentType?.config()?.let { ornament ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clipToBounds(),
                ) {
                    Image(
                        painter = painterResource(ornament.resource),
                        contentDescription = null,
                        modifier = Modifier
                            .align(ornament.alignment)
                            .requiredWidth(ornament.width)
                            .offset(x = ornament.offsetX),
                        contentScale = ContentScale.FillWidth,
                        alpha = ornament.alpha,
                    )
                }
            }

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
