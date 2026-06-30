package com.agc.bwitch

import com.agc.bwitch.domain.astrology.natal.BirthplacePreset
import com.agc.bwitch.ui.astrology.birthplace.BirthplaceCatalogRepository
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class BirthplaceCatalogRepositoryTest {
    private val csvPreset = BirthplacePreset(
        id = "madrid-es-3117735",
        cityName = "Madrid",
        countryName = "Spain",
        latitudeDegrees = 40.4165,
        longitudeDegrees = -3.7026,
        timezoneId = "Europe/Madrid",
        countryCode = "ES",
    )
    private val generatedPreset = BirthplacePreset(
        id = "generated-fallback",
        cityName = "Generated",
        countryName = "Fallback",
        latitudeDegrees = 1.0,
        longitudeDegrees = 2.0,
        timezoneId = "UTC",
    )
    private val handWrittenPreset = BirthplacePreset(
        id = "handwritten-fallback",
        cityName = "Handwritten",
        countryName = "Fallback",
        latitudeDegrees = 3.0,
        longitudeDegrees = 4.0,
        timezoneId = "UTC",
    )

    @Test
    fun csvCatalogWithOneEntryFallsBackToGeneratedPresets() = runTest {
        val repository = BirthplaceCatalogRepository(
            loadCatalog = { listOf(csvPreset) },
            generatedPresets = listOf(generatedPreset),
            handWrittenPresets = listOf(handWrittenPreset),
        )

        assertEquals(listOf(generatedPreset), repository.getBirthplaces())
    }

    @Test
    fun csvCatalogWithAtLeastOneHundredEntriesIsPrimarySource() = runTest {
        val csvCatalog = csvCatalog(size = 100)
        val repository = BirthplaceCatalogRepository(
            loadCatalog = { csvCatalog },
            generatedPresets = listOf(generatedPreset),
            handWrittenPresets = listOf(handWrittenPreset),
        )

        assertEquals(csvCatalog, repository.getBirthplaces())
    }

    @Test
    fun emptyCsvCatalogFallsBackToGeneratedPresets() = runTest {
        val repository = BirthplaceCatalogRepository(
            loadCatalog = { emptyList() },
            generatedPresets = listOf(generatedPreset),
            handWrittenPresets = listOf(handWrittenPreset),
        )

        assertEquals(listOf(generatedPreset), repository.getBirthplaces())
    }

    @Test
    fun cachesResultAndDoesNotReloadCatalog() = runTest {
        var loadCount = 0
        val csvCatalog = csvCatalog(size = 100)
        val repository = BirthplaceCatalogRepository(
            loadCatalog = {
                loadCount++
                csvCatalog
            },
            generatedPresets = listOf(generatedPreset),
            handWrittenPresets = listOf(handWrittenPreset),
        )

        assertEquals(csvCatalog, repository.getBirthplaces())
        assertEquals(csvCatalog, repository.getBirthplaces())
        assertEquals(1, loadCount)
    }

    @Test
    fun loaderFailureFallsBackToGeneratedPresets() = runTest {
        val repository = BirthplaceCatalogRepository(
            loadCatalog = { error("CSV unavailable") },
            generatedPresets = listOf(generatedPreset),
            handWrittenPresets = listOf(handWrittenPreset),
        )

        assertEquals(listOf(generatedPreset), repository.getBirthplaces())
    }

    @Test
    fun emptyCsvAndGeneratedPresetsFallBackToHandWrittenPresets() = runTest {
        val repository = BirthplaceCatalogRepository(
            loadCatalog = { emptyList() },
            generatedPresets = emptyList(),
            handWrittenPresets = listOf(handWrittenPreset),
        )

        assertEquals(listOf(handWrittenPreset), repository.getBirthplaces())
    }

    private fun csvCatalog(size: Int): List<BirthplacePreset> = List(size) { index ->
        csvPreset.copy(
            id = "csv-$index",
            cityName = "Csv City $index",
        )
    }
}
