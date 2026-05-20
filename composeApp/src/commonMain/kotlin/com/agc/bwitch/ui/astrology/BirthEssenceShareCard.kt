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
import com.agc.bwitch.localization.ZodiacStrings
import com.agc.bwitch.localization.appStrings
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
    val interpretationPreview = essence.interpretationPreview()
    val zodiacStrings = appStrings.zodiac

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
                text = "${essence.sunSign.toDisplayName(zodiacStrings)} · ${essence.moonSign.toDisplayName(zodiacStrings)} · ${essence.risingSign.toDisplayName(zodiacStrings)}",
                style = MaterialTheme.typography.bodySmall,
                color = extras.textSecondary,
            )

            Text(
                text = interpretationPreview,
                style = MaterialTheme.typography.bodyMedium,
                fontStyle = FontStyle.Italic,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun BirthEssenceProfile.interpretationPreview(maxChars: Int = 280): String {
    val normalized = interpretation.trim().replace("\n", " ")
    if (normalized.isEmpty()) return normalized

    val sentenceRegex = Regex("[^.!?]+[.!?]")
    val sentences = sentenceRegex
        .findAll(normalized)
        .map { it.value.trim() }
        .filter { it.isNotEmpty() }
        .take(2)
        .toList()

    if (sentences.isNotEmpty()) {
        return sentences.joinToString(separator = " ")
    }

    if (normalized.length <= maxChars) return normalized

    return normalized
        .take(maxChars)
        .substringBeforeLast(" ", normalized.take(maxChars))
        .trimEnd() + "…"
}

private fun ZodiacSign.toDisplayName(strings: ZodiacStrings): String = when (this) {
    ZodiacSign.aries -> strings.aries
    ZodiacSign.taurus -> strings.taurus
    ZodiacSign.gemini -> strings.gemini
    ZodiacSign.cancer -> strings.cancer
    ZodiacSign.leo -> strings.leo
    ZodiacSign.virgo -> strings.virgo
    ZodiacSign.libra -> strings.libra
    ZodiacSign.scorpio -> strings.scorpio
    ZodiacSign.sagittarius -> strings.sagittarius
    ZodiacSign.capricorn -> strings.capricorn
    ZodiacSign.aquarius -> strings.aquarius
    ZodiacSign.pisces -> strings.pisces
}
