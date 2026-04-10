package com.agc.bwitch.ui.astrology

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import com.agc.bwitch.domain.astrology.birthchart.BirthEssenceProfile
import com.agc.bwitch.domain.astrology.horoscope.ZodiacSign
import com.agc.bwitch.ui.common.toVisualResource
import com.agc.bwitch.ui.theme.BWitchThemeTokens
import org.jetbrains.compose.resources.painterResource

@Composable
fun BirthEssenceShareCard(
    essence: BirthEssenceProfile,
    modifier: Modifier = Modifier,
) {
    val dimens = BWitchThemeTokens.dimens
    val extras = BWitchThemeTokens.extras
    val archetype = essence.archetype
    val archetypeName = archetype?.displayName(essence.languageCode).orEmpty()
    val firstSentence = essence.firstSentence()

    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceVariant,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(dimens.spacingLg),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(dimens.spacingSm),
        ) {
            Text(
                text = archetypeName,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            archetype?.let {
                Image(
                    painter = painterResource(it.toVisualResource()),
                    contentDescription = archetypeName,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.6f),
                    contentScale = ContentScale.Fit,
                )
            }

            Text(
                text = "${essence.sunSign.toDisplayName()} · ${essence.moonSign.toDisplayName()} · ${essence.risingSign.toDisplayName()}",
                style = MaterialTheme.typography.bodySmall,
                color = extras.textSecondary,
            )

            Text(
                text = firstSentence,
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            Text(
                text = "BWitch ✨",
                style = MaterialTheme.typography.labelSmall,
                color = extras.textSecondary,
            )
        }
    }
}

private fun BirthEssenceProfile.firstSentence(): String {
    val hasMultipleSentences = interpretation.contains('.')
    val firstChunk = interpretation
        .split('.')
        .firstOrNull { it.isNotBlank() }
        ?.trim()
        .orEmpty()

    if (firstChunk.isEmpty()) {
        return interpretation.trim()
    }

    return if (hasMultipleSentences) "$firstChunk." else firstChunk
}

private fun ZodiacSign.toDisplayName(): String = when (this) {
    ZodiacSign.aries -> "Aries"
    ZodiacSign.taurus -> "Tauro"
    ZodiacSign.gemini -> "Géminis"
    ZodiacSign.cancer -> "Cáncer"
    ZodiacSign.leo -> "Leo"
    ZodiacSign.virgo -> "Virgo"
    ZodiacSign.libra -> "Libra"
    ZodiacSign.scorpio -> "Escorpio"
    ZodiacSign.sagittarius -> "Sagitario"
    ZodiacSign.capricorn -> "Capricornio"
    ZodiacSign.aquarius -> "Acuario"
    ZodiacSign.pisces -> "Piscis"
}
