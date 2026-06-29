package com.agc.bwitch.ui.astrology.birthplace

import com.agc.bwitch.domain.astrology.natal.BirthplacePreset

private const val MaxBirthplaceSearchResults = 20
private const val DefaultCountryPriority = Int.MAX_VALUE

private val BirthplaceCountryPriorities = mapOf(
    "madrid" to listOf("ES"),
    "tokyo" to listOf("JP"),
    "moscow" to listOf("RU"),
    "buenos aires" to listOf("AR"),
    "bogota" to listOf("CO"),
    "ciudad de mexico" to listOf("MX"),
    "mexico city" to listOf("MX"),
    "new york" to listOf("US"),
    "los angeles" to listOf("US"),
    "chicago" to listOf("US"),
    "london" to listOf("GB"),
    "paris" to listOf("FR"),
    "berlin" to listOf("DE"),
    "rome" to listOf("IT"),
    "sao paulo" to listOf("BR"),
    "rio de janeiro" to listOf("BR"),
)

internal fun rankBirthplaceMatches(
    query: String,
    presets: List<BirthplacePreset>,
    limit: Int = MaxBirthplaceSearchResults,
): List<BirthplacePreset> {
    val normalizedQuery = normalizeBirthplaceSearchText(query)
    if (normalizedQuery.isBlank()) return presets.take(limit)

    return presets
        .asSequence()
        .mapIndexedNotNull { index, preset ->
            val score = preset.birthplaceSearchScore(normalizedQuery) ?: return@mapIndexedNotNull null
            RankedBirthplace(preset = preset, score = score, originalIndex = index)
        }
        .sortedWith(
            compareBy<RankedBirthplace> { it.score.matchTier }
                .thenBy { it.score.countryPriority }
                .thenBy { it.score.cityNameLength }
                .thenBy { it.score.countryNameLength }
                .thenBy { it.originalIndex }
        )
        .take(limit)
        .map { it.preset }
        .toList()
}

internal fun BirthplacePreset.matchesBirthplaceQuery(query: String): Boolean {
    val normalizedQuery = normalizeBirthplaceSearchText(query)
    if (normalizedQuery.isBlank()) return true
    return birthplaceSearchScore(normalizedQuery) != null
}

private fun BirthplacePreset.birthplaceSearchScore(normalizedQuery: String): BirthplaceSearchScore? {
    val normalizedCity = normalizeBirthplaceSearchText(cityName)
    val normalizedCountry = normalizeBirthplaceSearchText(countryName)
    val matchTier = when {
        normalizedCity == normalizedQuery -> 0
        normalizedCity.startsWith(normalizedQuery) -> 1
        normalizedCity.contains(normalizedQuery) -> 2
        normalizedCountry.contains(normalizedQuery) -> 3
        else -> return null
    }

    return BirthplaceSearchScore(
        matchTier = matchTier,
        cityNameLength = normalizedCity.length,
        countryPriority = countryPriorityFor(normalizedCity, countryCode),
        countryNameLength = normalizedCountry.length,
    )
}

private data class RankedBirthplace(
    val preset: BirthplacePreset,
    val score: BirthplaceSearchScore,
    val originalIndex: Int,
)

private fun countryPriorityFor(normalizedCity: String, countryCode: String?): Int {
    val prioritizedCountryCodes = BirthplaceCountryPriorities[normalizedCity] ?: return DefaultCountryPriority
    val normalizedCountryCode = countryCode?.uppercase() ?: return DefaultCountryPriority
    val priority = prioritizedCountryCodes.indexOf(normalizedCountryCode)
    return if (priority >= 0) priority else DefaultCountryPriority
}

private data class BirthplaceSearchScore(
    val matchTier: Int,
    val countryPriority: Int,
    val cityNameLength: Int,
    val countryNameLength: Int,
)
