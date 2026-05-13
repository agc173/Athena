package com.agc.bwitch.ui.rituals

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.agc.bwitch.domain.rituals.RitualCatalogRepository
import com.agc.bwitch.localization.appStrings
import com.agc.bwitch.presentation.localization.AppLanguageViewModel
import com.agc.bwitch.ui.common.designsystem.BWitchCard
import com.agc.bwitch.ui.common.designsystem.BWitchScreen
import com.agc.bwitch.ui.common.designsystem.BWitchSectionHeader
import org.koin.compose.koinInject

@Composable
fun RitualDetailScreen(
    ritualId: String,
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    repository: RitualCatalogRepository = koinInject(),
    appLanguageViewModel: AppLanguageViewModel = koinInject(),
) {
    val ritual = remember(ritualId, repository) { repository.getRitualById(ritualId) }
    val strings = appStrings.ritualCatalog
    val languageState by appLanguageViewModel.uiState.collectAsState()
    val localizedRitual = ritual?.localized(languageState.currentLanguage)

    BWitchScreen(
        contentPadding = contentPadding,
        modifier = modifier.verticalScroll(rememberScrollState()),
    ) {
        if (localizedRitual == null) {
            BWitchSectionHeader(
                title = appStrings.navigation.ritual,
                subtitle = strings.detailNotFound,
            )
            return@BWitchScreen
        }

        BWitchSectionHeader(
            title = localizedRitual.title,
            subtitle = localizedRitual.subtitle,
        )

        BWitchCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = strings.detailIntentionTitle,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = localizedRitual.intention,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = strings.detailMaterialsTitle,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = localizedRitual.materials.joinToString(separator = "\n") { item ->
                    strings.detailMaterialBulletFormat.withText(item)
                },
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        BWitchCard(modifier = Modifier.fillMaxWidth()) {
            localizedRitual.preparation?.let { preparation ->
                Text(text = strings.detailPreparationTitle, style = MaterialTheme.typography.titleMedium)
                Text(text = preparation, style = MaterialTheme.typography.bodyMedium)
            }
            Text(text = strings.detailActionTitle, style = MaterialTheme.typography.titleMedium)
            Text(text = localizedRitual.action, style = MaterialTheme.typography.bodyMedium)
            Text(text = strings.detailClosingTitle, style = MaterialTheme.typography.titleMedium)
            Text(text = localizedRitual.closing, style = MaterialTheme.typography.bodyMedium)
            localizedRitual.optionalNote?.let { note ->
                Text(text = strings.detailOptionalNoteTitle, style = MaterialTheme.typography.titleMedium)
                Text(text = note, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}

private fun String.withText(value: String): String = replaceFirst("%s", value)
