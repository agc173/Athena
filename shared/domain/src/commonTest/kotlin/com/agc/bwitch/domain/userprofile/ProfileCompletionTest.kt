package com.agc.bwitch.domain.userprofile

import com.agc.bwitch.domain.astrology.horoscope.ZodiacSign
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ProfileCompletionTest {
    @Test
    fun `profile with username and birth date is complete without avatar`() {
        val profile = UserProfile(
            displayName = "Luna",
            photoUrl = null,
            email = "luna@example.com",
            username = "luna_user",
            birthDate = LocalDate.parse("1995-10-12"),
            zodiacSign = ZodiacSign.libra,
        )

        assertTrue(profile.hasMinimumProfileCompleted())
    }

    @Test
    fun `profile missing zodiac sign is incomplete`() {
        val profile = UserProfile(
            displayName = null,
            photoUrl = null,
            email = null,
            username = "luna_user",
            birthDate = LocalDate.parse("1995-10-12"),
            zodiacSign = null,
        )
        assertFalse(profile.hasMinimumProfileCompleted())
    }
}
