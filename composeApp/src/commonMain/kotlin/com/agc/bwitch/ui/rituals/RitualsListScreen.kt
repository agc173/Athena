package com.agc.bwitch.ui.rituals

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.agc.bwitch.domain.rituals.RitualCatalogRepository
import com.agc.bwitch.domain.rituals.RitualCategoryType
import com.agc.bwitch.domain.rituals.RitualListItem
import com.agc.bwitch.ui.common.designsystem.BWitchCard
import com.agc.bwitch.ui.common.designsystem.BWitchScreen
import com.agc.bwitch.ui.common.designsystem.BWitchSectionHeader
import org.koin.compose.koinInject

@Composable
fun RitualsListScreen(
    category: RitualCategoryType,
    contentPadding: PaddingValues,
    onOpenRitual: (String) -> Unit,
    modifier: Modifier = Modifier,
    repository: RitualCatalogRepository = koinInject(),
) {
    val rituals = remember(category, repository) { repository.getRitualsByCategory(category) }

    BWitchScreen(
        contentPadding = contentPadding,
        modifier = modifier,
    ) {
        BWitchSectionHeader(
            title = category.displayName(),
            subtitle = "Selecciona un ritual para ver su guía completa.",
        )

        rituals.forEach { ritual ->
            RitualListCard(
                ritual = ritual,
                onClick = { onOpenRitual(ritual.id) },
            )
        }
    }
}

@Composable
private fun RitualListCard(
    ritual: RitualListItem,
    onClick: () -> Unit,
) {
    BWitchCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Text(
            text = ritual.title,
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = ritual.subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = "Materiales: ${ritual.materialsHint}",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )
    }
}

private fun RitualCategoryType.displayName(): String =
    when (this) {
        RitualCategoryType.Love -> "Amor"
        RitualCategoryType.Prosperity -> "Prosperidad"
        RitualCategoryType.Protection -> "Protección"
        RitualCategoryType.Cleansing -> "Limpieza"
    }
