package com.agc.bwitch.ui.localization

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.agc.bwitch.domain.localization.AppLanguage
import com.agc.bwitch.localization.appStrings

@Composable
fun LanguageSelectorSection(
    currentLanguage: AppLanguage,
    supportedLanguages: List<AppLanguage>,
    onLanguageSelected: (AppLanguage) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    titleText: String? = null,
    subtitleText: String? = null,
    selectedPrefixText: String? = null,
) {
    val strings = appStrings.common
    val resolvedTitleText = titleText ?: strings.languageSectionTitle
    val resolvedSubtitleText = subtitleText ?: strings.languageSectionSubtitle
    val resolvedSelectedPrefixText = selectedPrefixText ?: strings.languageSelectedPrefix

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = resolvedTitleText,
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = resolvedSubtitleText,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )

        supportedLanguages.forEach { language ->
            val selected = language == currentLanguage
            OutlinedButton(
                onClick = { onLanguageSelected(language) },
                enabled = enabled,
                modifier = Modifier.fillMaxWidth(),
            ) {
                val prefix = if (selected) resolvedSelectedPrefixText else ""
                Text(text = prefix + language.nativeLabel)
            }
        }
    }
}
