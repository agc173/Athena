package com.agc.bwitch.ui.rituals

import com.agc.bwitch.domain.localization.AppLanguage
import com.agc.bwitch.domain.rituals.RitualCategory
import com.agc.bwitch.domain.rituals.RitualDetail
import com.agc.bwitch.domain.rituals.RitualListItem
import com.agc.bwitch.domain.rituals.i18n.RitualCatalogContentRepository

internal fun RitualCategory.localized(language: AppLanguage): RitualCategory =
    copy(
        title = RitualCatalogContentRepository.resolveCompat(language, title),
        subtitle = RitualCatalogContentRepository.resolveCompat(language, subtitle),
    )

internal fun RitualListItem.localized(language: AppLanguage): RitualListItem =
    copy(
        title = RitualCatalogContentRepository.resolveCompat(language, title),
        subtitle = RitualCatalogContentRepository.resolveCompat(language, subtitle),
        materialsHint = RitualCatalogContentRepository.resolveCompat(language, materialsHint),
    )

internal fun RitualDetail.localized(language: AppLanguage): RitualDetail =
    copy(
        title = RitualCatalogContentRepository.resolveCompat(language, title),
        subtitle = RitualCatalogContentRepository.resolveCompat(language, subtitle),
        intention = RitualCatalogContentRepository.resolveCompat(language, intention),
        materials = materials.map { material -> RitualCatalogContentRepository.resolveCompat(language, material) },
        preparation = preparation?.let { value -> RitualCatalogContentRepository.resolveCompat(language, value) },
        action = RitualCatalogContentRepository.resolveCompat(language, action),
        closing = RitualCatalogContentRepository.resolveCompat(language, closing),
        optionalNote = optionalNote?.let { value -> RitualCatalogContentRepository.resolveCompat(language, value) },
    )
