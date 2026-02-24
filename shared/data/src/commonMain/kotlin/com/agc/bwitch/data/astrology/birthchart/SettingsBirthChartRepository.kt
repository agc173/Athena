package com.agc.bwitch.data.astrology.birthchart

import com.agc.bwitch.data.storage.SettingsFactory
import com.agc.bwitch.domain.astrology.birthchart.BirthChartRepository
import com.agc.bwitch.domain.astrology.birthchart.BirthData
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class SettingsBirthChartRepository(
    settingsFactory: SettingsFactory
) : BirthChartRepository {

    private val settings: Settings = settingsFactory.create("bwitch_birth_chart")
    private val json = Json { ignoreUnknownKeys = true }

    private val keyV2 = "birth_data_v2"
    private val keyV1 = "birth_data_v1"

    private val _birthData = MutableStateFlow<BirthData?>(readCachedOrNull())

    override fun observeBirthData() = _birthData.asStateFlow()

    private fun readCachedOrNull(): BirthData? =
        settings.getStringOrNull(keyV2)?.let { raw ->
            runCatching {
                val dto = json.decodeFromString(BirthDataDtoV2.serializer(), raw)
                BirthData(
                    date = LocalDate.parse(dto.date),
                    time = LocalTime.parse(dto.time),
                    placeName = dto.placeName,
                    lat = dto.lat,
                    lon = dto.lon
                )
            }.getOrNull()
        }


    override suspend fun getBirthData(): BirthData? {
        settings.getStringOrNull(keyV2)?.let { raw ->
            val parsed = runCatching {
                val dto = json.decodeFromString(BirthDataDtoV2.serializer(), raw)
                BirthData(
                    date = LocalDate.parse(dto.date),
                    time = LocalTime.parse(dto.time),
                    placeName = dto.placeName,
                    lat = dto.lat,
                    lon = dto.lon
                )
            }.getOrNull()

            _birthData.value = parsed
            return parsed
        }

        val rawV1 = settings.getStringOrNull(keyV1) ?: return null
        val migrated = runCatching {
            val dto = json.decodeFromString(BirthDataDtoV1.serializer(), rawV1)
            BirthData(
                date = LocalDate.parse(dto.date),
                time = LocalTime.parse(dto.time),
                placeName = dto.placeName,
                lat = dto.lat,
                lon = dto.lon
            )
        }.getOrNull()

        if (migrated != null) {
            saveBirthData(migrated) // también actualiza el flow
        } else {
            _birthData.value = null
        }

        return migrated
    }

    suspend fun clear() {
        settings.remove(keyV2)
        settings.remove(keyV1) // por si había datos legacy
        _birthData.value = null
    }

    internal suspend fun saveBirthDataWithUpdatedAt(
        data: BirthData,
        updatedAtEpochMillis: Long
    ) {
        val dto = BirthDataDtoV2(
            date = data.date.toString(),
            time = data.time.toString(),
            placeName = data.placeName,
            lat = data.lat,
            lon = data.lon,
            updatedAtEpochMillis = updatedAtEpochMillis
        )
        settings.putString(keyV2, json.encodeToString(BirthDataDtoV2.serializer(), dto))
        _birthData.value = data
    }


    override suspend fun saveBirthData(data: BirthData) {
        val now = Clock.System.now().toEpochMilliseconds()
        saveBirthDataWithUpdatedAt(data, now)
    }


    internal fun getLocalUpdatedAtEpochMillisOrNull(): Long? {
        val raw = settings.getStringOrNull(keyV2) ?: return null
        return runCatching { json.decodeFromString(BirthDataDtoV2.serializer(), raw).updatedAtEpochMillis }.getOrNull()
    }

    @Serializable
    private data class BirthDataDtoV1(
        val date: String,
        val time: String,
        val placeName: String,
        val lat: Double? = null,
        val lon: Double? = null
    )

    @Serializable
    private data class BirthDataDtoV2(
        val date: String,
        val time: String,
        val placeName: String,
        val lat: Double? = null,
        val lon: Double? = null,
        val updatedAtEpochMillis: Long
    )
}

