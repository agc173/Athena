package com.agc.bwitch.ui.rituals

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import com.agc.bwitch.domain.rituals.RitualCatalogRepository
import com.agc.bwitch.domain.rituals.RitualCategory
import com.agc.bwitch.domain.rituals.RitualCategoryType
import com.agc.bwitch.localization.appStrings
import com.agc.bwitch.presentation.localization.AppLanguageViewModel
import com.agc.bwitch.ui.common.designsystem.BWitchCard
import com.agc.bwitch.ui.common.designsystem.BWitchScreen
import com.agc.bwitch.ui.common.designsystem.BWitchSectionHeader
import org.koin.compose.koinInject

@Composable
fun RitualsCategoryScreen(
    contentPadding: PaddingValues,
    onOpenCategory: (RitualCategoryType) -> Unit,
    modifier: Modifier = Modifier,
    repository: RitualCatalogRepository = koinInject(),
    appLanguageViewModel: AppLanguageViewModel = koinInject(),
) {
    val categories = remember(repository) { repository.getCategories() }
    val strings = appStrings.ritualCatalog
    val languageState by appLanguageViewModel.uiState.collectAsState()

    BWitchScreen(
        contentPadding = contentPadding,
        modifier = modifier,
    ) {
        BWitchSectionHeader(
            title = strings.categoriesHeaderTitle,
            subtitle = strings.categoriesHeaderSubtitle,
        )

        categories.forEach { category ->
            RitualCategoryCard(
                category = category.localized(languageState.currentLanguage),
                onClick = { onOpenCategory(category.type) },
            )
        }
    }
}

@Composable
private fun RitualCategoryCard(
    category: RitualCategory,
    onClick: () -> Unit,
) {
    BWitchCard(
        modifier = Modifier.fillMaxWidth(),
        onClick = onClick,
    ) {
        Text(
            text = category.title,
            style = MaterialTheme.typography.titleLarge,
        )
        Text(
            text = category.subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
