package com.agc.bwitch.ui.astrology.birthplace

import bwitch.composeapp.generated.resources.Res
import com.agc.bwitch.domain.astrology.natal.BirthplacePreset
import org.jetbrains.compose.resources.ExperimentalResourceApi

object BirthplaceCatalogLoader {
    private const val BirthplacesResourcePath = "files/birthplaces.csv"

    @OptIn(ExperimentalResourceApi::class)
    suspend fun load(): List<BirthplacePreset> = runCatching {
        Res.readBytes(BirthplacesResourcePath).decodeToString()
    }.getOrNull()
        ?.let(BirthplaceCatalogCsvParser::parse)
        .orEmpty()
}

internal object BirthplaceCatalogCsvParser {
    private const val LegacyColumnCount = 9
    private const val SearchNamesColumnCount = 10
    private val LegacyHeader = listOf(
        "geonameId",
        "cityName",
        "countryName",
        "countryCode",
        "latitudeDegrees",
        "longitudeDegrees",
        "timezoneId",
        "population",
        "featureCode",
    )
    private val Header = LegacyHeader + "searchNames"
    private val NonAlphaNumericRegex = Regex("[^a-z0-9]+")

    fun parse(csv: String): List<BirthplacePreset> {
        if (csv.isBlank()) return emptyList()

        return csv.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .mapNotNull(::parseLine)
            .toList()
    }

    private fun parseLine(line: String): BirthplacePreset? {
        val columns = splitCsvLine(line) ?: return null
        if (columns == LegacyHeader || columns == Header) return null
        if (columns.size != LegacyColumnCount && columns.size != SearchNamesColumnCount) return null

        val geonameId = columns[0].trim()
        val cityName = columns[1].trim()
        val countryName = columns[2].trim()
        val countryCode = columns[3].trim().uppercase()
        val latitude = columns[4].trim().toDoubleOrNull()
        val longitude = columns[5].trim().toDoubleOrNull()
        val timezoneId = columns[6].trim()
        val searchNames = columns.getOrNull(9)
            ?.split("|")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            .orEmpty()

        if (
            geonameId.isBlank() ||
            cityName.isBlank() ||
            countryName.isBlank() ||
            countryCode.isBlank() ||
            latitude == null ||
            longitude == null ||
            timezoneId.isBlank()
        ) {
            return null
        }

        return BirthplacePreset(
            id = "${slug(cityName)}-${countryCode.lowercase()}-$geonameId",
            cityName = cityName,
            countryName = countryName,
            latitudeDegrees = latitude,
            longitudeDegrees = longitude,
            timezoneId = timezoneId,
            countryCode = countryCode,
            searchNames = searchNames,
        )
    }

    private fun slug(value: String): String = normalizeBirthplaceSearchText(value)
        .replace("&", " and ")
        .replace(NonAlphaNumericRegex, "-")
        .trim('-')
        .ifBlank { "city" }

    private fun splitCsvLine(line: String): List<String>? {
        val columns = mutableListOf<String>()
        val current = StringBuilder()
        var quoted = false
        var index = 0

        while (index < line.length) {
            val char = line[index]
            when {
                char == '"' && quoted && line.getOrNull(index + 1) == '"' -> {
                    current.append('"')
                    index++
                }
                char == '"' -> quoted = !quoted
                char == ',' && !quoted -> {
                    columns += current.toString()
                    current.clear()
                }
                else -> current.append(char)
            }
            index++
        }

        if (quoted) return null
        columns += current.toString()
        return columns
    }
}
