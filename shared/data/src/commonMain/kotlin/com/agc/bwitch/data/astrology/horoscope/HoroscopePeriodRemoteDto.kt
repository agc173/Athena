package com.agc.bwitch.data.astrology.horoscope

import com.agc.bwitch.domain.astrology.horoscope.MonthlyHoroscope
import com.agc.bwitch.domain.astrology.horoscope.WeeklyHoroscope
import com.agc.bwitch.domain.astrology.horoscope.ZodiacSign
import kotlinx.serialization.Serializable

@Serializable
internal data class HoroscopeWeeklyRemoteDto(
    val title: String = "",
    val overview: String = "",
    val loveAndRelationships: String = "",
    val workAndMoney: String = "",
    val spiritualEnergy: String = "",
    val weeklyAdvice: String = "",
    val mantra: String = "",
    val shareText: String? = null,
    val languageCode: String? = null,
    val updatedAtEpochMillis: Long = 0L,
) {
    fun toDomain(sign: ZodiacSign, weekKey: String, requestedLanguageCode: String): WeeklyHoroscope =
        WeeklyHoroscope(
            sign = sign,
            weekKey = weekKey,
            languageCode = languageCode?.trim()?.lowercase()?.ifBlank { null } ?: requestedLanguageCode.lowercase(),
            title = title,
            overview = overview,
            loveAndRelationships = loveAndRelationships,
            workAndMoney = workAndMoney,
            spiritualEnergy = spiritualEnergy,
            weeklyAdvice = weeklyAdvice,
            mantra = mantra,
            shareText = shareText,
            updatedAtEpochMillis = updatedAtEpochMillis,
        )
}

@Serializable
internal data class HoroscopeMonthlyRemoteDto(
    val title: String = "",
    val monthTheme: String = "",
    val loveAndRelationships: String = "",
    val workAndMoney: String = "",
    val personalGrowth: String = "",
    val ritualSuggestion: String = "",
    val mantra: String = "",
    val shareText: String? = null,
    val languageCode: String? = null,
    val updatedAtEpochMillis: Long = 0L,
) {
    fun toDomain(sign: ZodiacSign, monthKey: String, requestedLanguageCode: String): MonthlyHoroscope =
        MonthlyHoroscope(
            sign = sign,
            monthKey = monthKey,
            languageCode = languageCode?.trim()?.lowercase()?.ifBlank { null } ?: requestedLanguageCode.lowercase(),
            title = title,
            monthTheme = monthTheme,
            loveAndRelationships = loveAndRelationships,
            workAndMoney = workAndMoney,
            personalGrowth = personalGrowth,
            ritualSuggestion = ritualSuggestion,
            mantra = mantra,
            shareText = shareText,
            updatedAtEpochMillis = updatedAtEpochMillis,
        )
}
