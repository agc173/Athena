package com.agc.bwitch.data.userprofile.dto

import com.agc.bwitch.domain.astrology.horoscope.ZodiacSign
import kotlinx.datetime.LocalDate
import kotlinx.serialization.Serializable

@Serializable
data class SaveUserProfileRequestDto(
    val displayName: String? = null,
    val photoUrl: String? = null,
    val email: String? = null,
    val username: String? = null,
    val birthDate: LocalDate? = null,
    val zodiacSign: ZodiacSign? = null,
    val birthEssenceSummary: String? = null,
    val updatedAtEpochMillis: Long,
)

@Serializable
data class SaveUserProfileResponseDto(
    val username: String? = null,
)
