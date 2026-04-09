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
import bwitch.composeapp.generated.resources.Res
import bwitch.composeapp.generated.resources.app_language_section_subtitle
import bwitch.composeapp.generated.resources.app_language_section_title
import bwitch.composeapp.generated.resources.app_language_selected_prefix
import com.agc.bwitch.domain.localization.AppLanguage
import org.jetbrains.compose.resources.stringResource

/**
 * Selector mínimo reutilizable para onboarding y ajustes.
 *
 * Nota: en próximas iteraciones se migrarán más textos a recursos Compose
 * (`composeResources/values[-xx]/strings.xml`) de forma pantalla-a-pantalla.
 */
@Composable
fun LanguageSelectorSection(
    currentLanguage: AppLanguage,
    supportedLanguages: List<AppLanguage>,
    onLanguageSelected: (AppLanguage) -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
    titleText: String = stringResource(Res.string.app_language_section_title),
    subtitleText: String = stringResource(Res.string.app_language_section_subtitle),
    selectedPrefixText: String = stringResource(Res.string.app_language_selected_prefix),
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Text(
            text = titleText,
            style = MaterialTheme.typography.titleMedium,
        )
        Text(
            text = subtitleText,
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
                val prefix = if (selected) selectedPrefixText else ""
                Text(text = prefix + language.nativeLabel)
            }
        }
    }
}
