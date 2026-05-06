package com.agc.bwitch.ui.localization

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
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

    var expanded by remember { mutableStateOf(false) }

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

        Column(modifier = Modifier.fillMaxWidth()) {
            OutlinedButton(
                onClick = { expanded = true },
                enabled = enabled && supportedLanguages.isNotEmpty(),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = currentLanguage.nativeLabel,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(text = "⌄")
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.widthIn(min = 240.dp),
            ) {
                supportedLanguages.forEach { language ->
                    val selected = language == currentLanguage
                    DropdownMenuItem(
                        text = {
                            val prefix = if (selected) resolvedSelectedPrefixText else ""
                            Text(text = prefix + language.nativeLabel)
                        },
                        onClick = {
                            expanded = false
                            onLanguageSelected(language)
                        },
                        enabled = enabled,
                    )
                }
            }
        }
    }
}
