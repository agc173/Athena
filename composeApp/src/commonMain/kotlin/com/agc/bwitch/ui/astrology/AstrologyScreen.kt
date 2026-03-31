package com.agc.bwitch.ui.astrology

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.matchParentSize
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
import bwitch.composeapp.generated.resources.synastry_ornament
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
            onClick = { onNavigate(Destination.HoroscopeDaily()) },
            modifier = Modifier.heightIn(min = 168.dp)
        )

        AstrologyFeatureCard(
            title = "Esencia natal",
            subtitle = "La huella de tu nacimiento",
            onClick = { onNavigate(Destination.BirthChart) }
        )

        AstrologyFeatureCard(
            title = "Sinastría",
            subtitle = "La energía entre dos",
            onClick = { onNavigate(Destination.Synastry) },
            showSynastryOrnament = true,
        )
    }
}

@Composable
private fun AstrologyFeatureCard(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    showSynastryOrnament: Boolean = false,
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
            if (showSynastryOrnament) {
                // Parámetros visuales del ornamento (ajustables para pruebas sin afectar la altura de la card).
                val synastryOrnamentWidth = 220.dp
                val synastryOrnamentOffsetX = 80.dp
                val synastryOrnamentAlignment = Alignment.CenterEnd
                val synastryOrnamentAlpha = 0.12f

                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .clipToBounds(),
                ) {
                    Image(
                        painter = painterResource(Res.drawable.synastry_ornament),
                        contentDescription = null,
                        modifier = Modifier
                            .align(synastryOrnamentAlignment)
                            .requiredWidth(synastryOrnamentWidth)
                            .offset(x = synastryOrnamentOffsetX),
                        contentScale = ContentScale.FillWidth,
                        alpha = synastryOrnamentAlpha,
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
