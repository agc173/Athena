package com.agc.bwitch.data.userprofile

import com.agc.bwitch.domain.astrology.horoscope.ZodiacSign
import com.agc.bwitch.domain.userprofile.UserProfile
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.runBlocking
import kotlinx.datetime.LocalDate

class SettingsUserProfileRepositoryTest {

    @Test
    fun getUserProfile_restoresDescriptionFromCache() = runBlocking {
        val settings = MapSettings()
        val repository = SettingsUserProfileRepository.fromSettings(settings)
        val profile = UserProfile(
            displayName = "Alfonso",
            photoUrl = "https://example.com/avatar.png",
            email = "alfonso@example.com",
            username = "alfonso",
            birthDate = LocalDate.parse("1995-08-14"),
            zodiacSign = ZodiacSign.leo,
            description = "Bio persisted across profile reloads",
            birthEssenceSummary = "Leo · Piscis · Libra",
        )

        repository.saveUserProfile(profile)

        val reloadedRepository = SettingsUserProfileRepository.fromSettings(settings)
        val reloaded = reloadedRepository.getUserProfile()

        assertEquals("Bio persisted across profile reloads", reloaded?.description)
    }
}
