package com.agc.bwitch.ui.rituals

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.agc.bwitch.domain.rituals.RitualCatalogRepository
import com.agc.bwitch.localization.appStrings
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
) {
    val ritual = remember(ritualId, repository) { repository.getRitualById(ritualId) }
    val strings = appStrings.ritualCatalog

    BWitchScreen(
        contentPadding = contentPadding,
        modifier = modifier,
    ) {
        if (ritual == null) {
            BWitchSectionHeader(
                title = appStrings.navigation.ritual,
                subtitle = strings.detailNotFound,
            )
            return@BWitchScreen
        }

        BWitchSectionHeader(
            title = ritual.title,
            subtitle = ritual.subtitle,
        )

        BWitchCard(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = strings.detailIntentionTitle,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = ritual.intention,
                style = MaterialTheme.typography.bodyMedium,
            )
            Text(
                text = strings.detailMaterialsTitle,
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = ritual.materials.joinToString(separator = "\n") { item -> "• $item" },
                style = MaterialTheme.typography.bodyMedium,
            )
        }

        BWitchCard(modifier = Modifier.fillMaxWidth()) {
            ritual.preparation?.let { preparation ->
                Text(text = strings.detailPreparationTitle, style = MaterialTheme.typography.titleMedium)
                Text(text = preparation, style = MaterialTheme.typography.bodyMedium)
            }
            Text(text = strings.detailActionTitle, style = MaterialTheme.typography.titleMedium)
            Text(text = ritual.action, style = MaterialTheme.typography.bodyMedium)
            Text(text = strings.detailClosingTitle, style = MaterialTheme.typography.titleMedium)
            Text(text = ritual.closing, style = MaterialTheme.typography.bodyMedium)
            ritual.optionalNote?.let { note ->
                Text(text = strings.detailOptionalNoteTitle, style = MaterialTheme.typography.titleMedium)
                Text(text = note, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
