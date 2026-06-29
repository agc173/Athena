package com.agc.bwitch.ui.astrology.birthplace

import com.agc.bwitch.domain.astrology.natal.BirthplacePreset

internal class BirthplaceCatalogRepository(
    private val loadCatalog: suspend () -> List<BirthplacePreset>,
    private val generatedPresets: List<BirthplacePreset>,
    private val handWrittenPresets: List<BirthplacePreset>,
) {
    private var cachedBirthplaces: List<BirthplacePreset>? = null

    suspend fun getBirthplaces(): List<BirthplacePreset> {
        cachedBirthplaces?.let { return it }

        val csvBirthplaces = runCatching { loadCatalog() }
            .getOrNull()
            .orEmpty()
        val resolvedBirthplaces = csvBirthplaces
            .takeIf { it.size >= MinimumRuntimeCsvBirthplaceCount }
            .orEmpty()
            .ifEmpty { generatedPresets }
            .ifEmpty { handWrittenPresets }

        cachedBirthplaces = resolvedBirthplaces
        return resolvedBirthplaces
    }

    private companion object {
        // Temporary guardrail until birthplaces.csv is regenerated from GeoNames with a real catalog.
        const val MinimumRuntimeCsvBirthplaceCount = 100
    }
}

internal val DefaultBirthplaceCatalogRepository = BirthplaceCatalogRepository(
    loadCatalog = BirthplaceCatalogLoader::load,
    generatedPresets = GeneratedBirthplacePresets,
    handWrittenPresets = HandWrittenBirthplacePresets,
)
