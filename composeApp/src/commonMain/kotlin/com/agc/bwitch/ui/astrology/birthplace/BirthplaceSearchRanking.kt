package com.agc.bwitch.ui.astrology.birthplace

import com.agc.bwitch.domain.astrology.natal.BirthplacePreset

private const val MaxBirthplaceSearchResults = 20
private const val DefaultCountryPriority = Int.MAX_VALUE

private val BirthplaceSearchAliases = mapOf(
    "roma" to listOf("rome"),
    "moscu" to listOf("moscow"),
    "pekin" to listOf("beijing"),
    "londres" to listOf("london"),
    "nueva york" to listOf("new york", "new york city"),
    "new york" to listOf("new york city"),
)

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
    "beijing" to listOf("CN"),
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

    val equivalentQueries = equivalentBirthplaceQueries(normalizedQuery)

    return presets
        .asSequence()
        .mapIndexedNotNull { index, preset ->
            val score = equivalentQueries
                .mapNotNull { equivalentQuery ->
                    preset.birthplaceSearchScore(
                        normalizedQuery = equivalentQuery.query,
                        aliasPenalty = equivalentQuery.aliasPenalty,
                    )
                }
                .minWithOrNull(BirthplaceSearchScoreComparator)
                ?: return@mapIndexedNotNull null
            RankedBirthplace(preset = preset, score = score, originalIndex = index)
        }
        .sortedWith(
            compareBy<RankedBirthplace> { it.score.effectiveMatchTier }
                .thenBy { it.score.matchTier }
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
    return equivalentBirthplaceQueries(normalizedQuery).any { equivalentQuery ->
        birthplaceSearchScore(
            normalizedQuery = equivalentQuery.query,
            aliasPenalty = equivalentQuery.aliasPenalty,
        ) != null
    }
}

private fun equivalentBirthplaceQueries(normalizedQuery: String): List<EquivalentBirthplaceQuery> {
    val queries = mutableListOf(EquivalentBirthplaceQuery(query = normalizedQuery, aliasPenalty = 0))
    BirthplaceSearchAliases[normalizedQuery].orEmpty().forEach { alias ->
        val normalizedAlias = normalizeBirthplaceSearchText(alias)
        if (normalizedAlias.isNotBlank() && queries.none { it.query == normalizedAlias }) {
            queries += EquivalentBirthplaceQuery(query = normalizedAlias, aliasPenalty = 1)
        }
    }
    return queries
}

private fun BirthplacePreset.birthplaceSearchScore(
    normalizedQuery: String,
    aliasPenalty: Int,
): BirthplaceSearchScore? {
    val normalizedCity = normalizeBirthplaceSearchText(cityName)
    val normalizedCountry = normalizeBirthplaceSearchText(countryName)
    val normalizedDisplayName = normalizeBirthplaceSearchText("$cityName $countryName")
    val matchTier = when {
        normalizedCity == normalizedQuery -> 0
        normalizedDisplayName == normalizedQuery -> 0
        normalizedCity.startsWith(normalizedQuery) -> 1
        normalizedDisplayName.startsWith(normalizedQuery) -> 1
        normalizedCity.contains(normalizedQuery) -> 2
        normalizedDisplayName.contains(normalizedQuery) -> 2
        matchesSearchName(normalizedQuery) -> 3
        normalizedCountry.contains(normalizedQuery) -> 4
        else -> return null
    }

    return BirthplaceSearchScore(
        effectiveMatchTier = matchTier + aliasPenalty,
        matchTier = matchTier,
        cityNameLength = normalizedCity.length,
        countryPriority = countryPriorityFor(normalizedCity, countryCode),
        countryNameLength = normalizedCountry.length,
    )
}

private fun BirthplacePreset.matchesSearchName(normalizedQuery: String): Boolean = searchNames.any { searchName ->
    val normalizedSearchName = normalizeBirthplaceSearchText(searchName)
    normalizedSearchName == normalizedQuery ||
        normalizedSearchName.startsWith(normalizedQuery) ||
        normalizedSearchName.contains(normalizedQuery)
}

private data class EquivalentBirthplaceQuery(
    val query: String,
    val aliasPenalty: Int,
)

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

private val BirthplaceSearchScoreComparator = compareBy<BirthplaceSearchScore> { it.effectiveMatchTier }
    .thenBy { it.matchTier }
    .thenBy { it.countryPriority }
    .thenBy { it.cityNameLength }
    .thenBy { it.countryNameLength }

private data class BirthplaceSearchScore(
    val effectiveMatchTier: Int,
    val matchTier: Int,
    val countryPriority: Int,
    val cityNameLength: Int,
    val countryNameLength: Int,
)
