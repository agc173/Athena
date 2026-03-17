package com.agc.bwitch.domain.userprofile

import com.agc.bwitch.domain.astrology.horoscope.ZodiacSign
import kotlinx.datetime.LocalDate

data class UserProfile(
    val displayName: String?,
    val photoUrl: String?,
    val email: String?,
    val username: String?,
    val birthDate: LocalDate?,
    val zodiacSign: ZodiacSign?,
)
