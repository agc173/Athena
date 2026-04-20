package com.agc.bwitch.data.astrology.birthchart

import com.agc.bwitch.data.storage.SettingsFactory
import com.agc.bwitch.domain.astrology.birthchart.BirthChartRepository
import com.agc.bwitch.domain.astrology.birthchart.BirthEssenceDraft
import com.agc.bwitch.domain.astrology.birthchart.BirthEssenceInput
import com.agc.bwitch.domain.astrology.birthchart.BirthEssenceArchetype
import com.agc.bwitch.domain.astrology.birthchart.BirthEssenceProfile
import com.agc.bwitch.domain.astrology.birthchart.BirthEssenceReading
import com.agc.bwitch.domain.astrology.horoscope.ZodiacSign
import com.agc.bwitch.domain.shared.ApiError
import com.agc.bwitch.domain.shared.ApiResult
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class SettingsBirthChartRepository(
    settingsFactory: SettingsFactory
) : BirthChartRepository {

    private val settings: Settings = settingsFactory.create("bwitch_birth_essence")
    private val json = Json { ignoreUnknownKeys = true }

    private val keyV3 = "birth_essence_v1"
    private val legacyKeys = listOf("birth_data_v2", "birth_data_v1")

    private val _birthEssence = MutableStateFlow<BirthEssenceProfile?>(readCachedOrNull())

    override fun observeBirthEssence() = _birthEssence.asStateFlow()

    private fun readCachedOrNull(): BirthEssenceProfile? {
        val raw = settings.getStringOrNull(keyV3) ?: return null
        return runCatching {
            val dto = json.decodeFromString(BirthEssenceLocalDto.serializer(), raw)
            dto.toDomain()
        }.getOrNull()
    }

    override suspend fun getBirthEssence(): BirthEssenceProfile? {
        val parsed = readCachedOrNull()
        _birthEssence.value = parsed
        return parsed
    }

    suspend fun clear() {
        settings.remove(keyV3)
        legacyKeys.forEach(settings::remove)
        _birthEssence.value = null
    }

    internal suspend fun saveBirthEssenceWithTimestamps(
        draft: BirthEssenceDraft,
        savedAtEpochMillis: Long,
        updatedAtEpochMillis: Long,
    ) {
        val dto = BirthEssenceLocalDto(
            sunSign = draft.sunSign.name,
            moonSign = draft.moonSign.name,
            risingSign = draft.risingSign.name,
            archetype = draft.archetype?.name,
            interpretation = draft.interpretation,
            languageCode = draft.languageCode,
            savedAtEpochMillis = savedAtEpochMillis,
            updatedAtEpochMillis = updatedAtEpochMillis,
        )
        settings.putString(keyV3, json.encodeToString(BirthEssenceLocalDto.serializer(), dto))
        _birthEssence.value = dto.toDomain()
    }

    override suspend fun saveBirthEssence(draft: BirthEssenceDraft) {
        val now = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
        saveBirthEssenceWithTimestamps(
            draft = draft,
            savedAtEpochMillis = now,
            updatedAtEpochMillis = now,
        )
    }

    internal fun getLocalUpdatedAtEpochMillisOrNull(): Long? {
        val raw = settings.getStringOrNull(keyV3) ?: return null
        return runCatching {
            json.decodeFromString(BirthEssenceLocalDto.serializer(), raw).updatedAtEpochMillis
        }.getOrNull()
    }

    override suspend fun generateBirthEssence(input: BirthEssenceInput): ApiResult<BirthEssenceReading> {
        return ApiResult.Err(ApiError.Internal("Generation is unavailable in local-only repository"))
    }

    @Serializable
    private data class BirthEssenceLocalDto(
        val sunSign: String,
        val moonSign: String,
        val risingSign: String,
        val archetype: String? = null,
        val interpretation: String,
        val languageCode: String? = null,
        val savedAtEpochMillis: Long,
        val updatedAtEpochMillis: Long,
    ) {
        fun toDomain(): BirthEssenceProfile {
            val sun = ZodiacSign.valueOf(sunSign.lowercase())
            val moon = ZodiacSign.valueOf(moonSign.lowercase())
            val rising = ZodiacSign.valueOf(risingSign.lowercase())
            return BirthEssenceProfile(
                sunSign = sun,
                moonSign = moon,
                risingSign = rising,
                interpretation = interpretation,
                languageCode = normalizeLanguageCode(languageCode) ?: "es",
                archetype = BirthEssenceArchetype.fromRawOrNull(archetype),
                savedAtEpochMillis = savedAtEpochMillis,
                updatedAtEpochMillis = updatedAtEpochMillis,
            )
        }
    }
}

private fun normalizeLanguageCode(raw: String?): String? =
    raw
        ?.trim()
        ?.lowercase()
        ?.substringBefore('-')
        ?.substringBefore('_')
        ?.ifBlank { null }
