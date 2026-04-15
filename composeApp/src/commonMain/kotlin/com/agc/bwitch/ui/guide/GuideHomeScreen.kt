package com.agc.bwitch.ui.guide

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
import bwitch.composeapp.generated.resources.oracle_ornament
import bwitch.composeapp.generated.resources.pendulum_ornament
import bwitch.composeapp.generated.resources.tarot_ornament
import com.agc.bwitch.localization.appStrings
import com.agc.bwitch.presentation.navigation.Destination
import com.agc.bwitch.ui.common.designsystem.BWitchCard
import com.agc.bwitch.ui.common.designsystem.BWitchScreen
import com.agc.bwitch.ui.common.designsystem.BWitchSectionHeader
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

private val GuideCardHeight = 168.dp

@Composable
fun GuideHomeScreen(
    contentPadding: PaddingValues,
    onNavigate: (Destination) -> Unit,
) {
    val strings = appStrings.oracle

    BWitchScreen(contentPadding = contentPadding) {
        BWitchSectionHeader(
            title = "Explora tu intuición",
            subtitle = "Elige una práctica para lo que necesitas comprender hoy",
        )

        GuideOptionCard(
            title = "Tarot",
            subtitle = "Lecturas e interpretación simbólica",
            details = "Carta única · Tirada de 3",
            onClick = { onNavigate(Destination.TarotHome) },
            ornamentType = GuideCardOrnament.Tarot,
            modifier = Modifier.height(GuideCardHeight),
        )

        GuideOptionCard(
            title = strings.guideEntryTitle,
            subtitle = strings.guideEntrySubtitle,
            onClick = { onNavigate(Destination.Oracle) },
            ornamentType = GuideCardOrnament.Oracle,
            modifier = Modifier.height(GuideCardHeight),
        )

        GuideOptionCard(
            title = "El Péndulo",
            subtitle = "Una respuesta rápida para tu pregunta",
            details = "Sí · No · Tal vez · Aún no",
            onClick = { onNavigate(Destination.Pendulum) },
            ornamentType = GuideCardOrnament.Pendulum,
            modifier = Modifier.height(GuideCardHeight),
        )
    }
}

private enum class GuideCardOrnament {
    Tarot,
    Oracle,
    Pendulum,
}

private data class GuideCardOrnamentConfig(
    val resource: DrawableResource,
    val width: Dp,
    val offsetX: Dp,
    val alignment: Alignment,
    val alpha: Float,
)

private fun GuideCardOrnament.config(): GuideCardOrnamentConfig =
    when (this) {
        GuideCardOrnament.Tarot -> GuideCardOrnamentConfig(
            resource = Res.drawable.tarot_ornament,
            width = 230.dp,
            offsetX = 62.dp,
            alignment = Alignment.CenterEnd,
            alpha = 0.45f,
        )
        GuideCardOrnament.Oracle -> GuideCardOrnamentConfig(
            resource = Res.drawable.oracle_ornament,
            width = 200.dp,
            offsetX = 38.dp,
            alignment = Alignment.CenterEnd,
            alpha = 0.25f,
        )
        GuideCardOrnament.Pendulum -> GuideCardOrnamentConfig(
            resource = Res.drawable.pendulum_ornament,
            width = 260.dp,
            offsetX = 52.dp,
            alignment = Alignment.CenterEnd,
            alpha = 0.25f,
        )
    }

@Composable
private fun GuideOptionCard(
    title: String,
    subtitle: String,
    details: String? = null,
    onClick: () -> Unit,
    ornamentType: GuideCardOrnament? = null,
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
                if (!details.isNullOrBlank()) {
                    Text(
                        text = details,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
