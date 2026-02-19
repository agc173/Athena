package com.agc.bwitch.data.astrology.birthchart

import com.agc.bwitch.data.storage.SettingsFactory
import com.agc.bwitch.domain.astrology.birthchart.BirthChartRepository
import com.agc.bwitch.domain.astrology.birthchart.BirthData
import com.russhwolf.settings.Settings
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class SettingsBirthChartRepository(
    settingsFactory: SettingsFactory
) : BirthChartRepository {

    private val settings: Settings = settingsFactory.create("bwitch_user_profile")
    private val json = Json { ignoreUnknownKeys = true }

    private val key = "birth_data_v1"

    override suspend fun getBirthData(): BirthData? {
        val raw = settings.getStringOrNull(key) ?: return null
        return runCatching {
            val dto = json.decodeFromString(BirthDataDto.serializer(), raw)
            BirthData(
                date = LocalDate.parse(dto.date),
                time = LocalTime.parse(dto.time),
                placeName = dto.placeName,
                lat = dto.lat,
                lon = dto.lon
            )
        }.getOrNull()
    }

    override suspend fun saveBirthData(data: BirthData) {
        val dto = BirthDataDto(
            date = data.date.toString(),
            time = data.time.toString(),
            placeName = data.placeName,
            lat = data.lat,
            lon = data.lon
        )
        val raw = json.encodeToString(BirthDataDto.serializer(), dto)
        settings.putString(key, raw)
    }

    @Serializable
    private data class BirthDataDto(
        val date: String,
        val time: String,
        val placeName: String,
        val lat: Double? = null,
        val lon: Double? = null
    )
}
