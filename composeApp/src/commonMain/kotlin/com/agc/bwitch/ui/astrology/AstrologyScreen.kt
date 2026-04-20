package com.agc.bwitch.ui.astrology

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import bwitch.composeapp.generated.resources.Res
import bwitch.composeapp.generated.resources.essence_ornament
import bwitch.composeapp.generated.resources.horoscope_ornament
import bwitch.composeapp.generated.resources.synastry_ornament
import com.agc.bwitch.localization.appStrings
import com.agc.bwitch.presentation.navigation.Destination
import com.agc.bwitch.ui.common.designsystem.BWitchCard
import com.agc.bwitch.ui.common.designsystem.BWitchScreen
import com.agc.bwitch.ui.common.designsystem.BWitchSectionHeader
import org.jetbrains.compose.resources.painterResource

@Composable
fun AstrologyScreen(
    contentPadding: PaddingValues,
    onNavigate: (Destination) -> Unit
) {
    val strings = appStrings.astrologyHome

    BWitchScreen(
        contentPadding = contentPadding,
        modifier = Modifier.verticalScroll(rememberScrollState())
    ) {
        BWitchSectionHeader(
            title = strings.headerTitle,
            subtitle = strings.headerSubtitle,
            titleStyle = MaterialTheme.typography.headlineSmall,
            subtitleStyle = MaterialTheme.typography.bodyLarge,
        )

        AstrologyFeatureCard(
            title = strings.horoscopeCardTitle,
            subtitle = strings.horoscopeCardSubtitle,
            onClick = { onNavigate(Destination.HoroscopeDaily()) },
            modifier = Modifier.height(168.dp),
            ornamentType = AstrologyCardOrnament.Horoscope,
        )

        AstrologyFeatureCard(
            title = strings.birthEssenceCardTitle,
            subtitle = strings.birthEssenceCardSubtitle,
            onClick = { onNavigate(Destination.BirthChart) },
            modifier = Modifier.height(168.dp),
            ornamentType = AstrologyCardOrnament.Essence,
        )

        AstrologyFeatureCard(
            title = strings.synastryCardTitle,
            subtitle = strings.synastryCardSubtitle,
            onClick = { onNavigate(Destination.Synastry) },
            modifier = Modifier.height(168.dp),
            ornamentType = AstrologyCardOrnament.Synastry,
        )
    }
}

private enum class AstrologyCardOrnament {
    Horoscope,
    Essence,
    Synastry,
}

private data class AstrologyCardOrnamentConfig(
    val resource: org.jetbrains.compose.resources.DrawableResource,
    val width: androidx.compose.ui.unit.Dp,
    val offsetX: androidx.compose.ui.unit.Dp,
    val alignment: Alignment,
    val alpha: Float,
)

private fun AstrologyCardOrnament.config(): AstrologyCardOrnamentConfig =
    when (this) {
        AstrologyCardOrnament.Horoscope -> AstrologyCardOrnamentConfig(
            resource = Res.drawable.horoscope_ornament,
            width = 258.dp,
            offsetX = 72.dp,
            alignment = Alignment.CenterEnd,
            alpha = 0.24f,
        )
        AstrologyCardOrnament.Essence -> AstrologyCardOrnamentConfig(
            resource = Res.drawable.essence_ornament,
            width = 250.dp,
            offsetX = 78.dp,
            alignment = Alignment.CenterEnd,
            alpha = 0.24f,
        )
        AstrologyCardOrnament.Synastry -> AstrologyCardOrnamentConfig(
            resource = Res.drawable.synastry_ornament,
            width = 260.dp,
            offsetX = 80.dp,
            alignment = Alignment.CenterEnd,
            alpha = 0.25f,
        )
    }

@Composable
private fun AstrologyFeatureCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    ornamentType: AstrologyCardOrnament? = null,
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
            ornamentType?.config()?.let { ornament ->
                // Parámetros visuales de ornamento por card (ajustables sin afectar la altura de la card).

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
