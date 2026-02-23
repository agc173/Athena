package com.agc.bwitch.data.userprofile

import com.agc.bwitch.data.storage.SettingsFactory
import com.agc.bwitch.domain.userprofile.UserProfile
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class SettingsUserProfileRepository(
    settingsFactory: SettingsFactory
) {

    private val settings: Settings =
        settingsFactory.create("bwitch_user_profile")

    private val json = Json { ignoreUnknownKeys = true }

    private val keyV1 = "user_profile_v1"

    private val _profile =
        MutableStateFlow<UserProfile?>(readCachedOrNull())

    fun observeUserProfile() = _profile.asStateFlow()

    suspend fun getUserProfile(): UserProfile? {
        val profile = readCachedOrNull()
        _profile.value = profile
        return profile
    }

    suspend fun saveUserProfile(profile: UserProfile) {
        val now = Clock.System.now().toEpochMilliseconds()
        saveUserProfileWithUpdatedAt(profile, now)
    }

    internal suspend fun saveUserProfileWithUpdatedAt(
        profile: UserProfile,
        updatedAtEpochMillis: Long
    ) {
        val dto = UserProfileDto(
            displayName = profile.displayName,
            photoUrl = profile.photoUrl,
            email = profile.email,
            updatedAtEpochMillis = updatedAtEpochMillis
        )

        settings.putString(
            keyV1,
            json.encodeToString(UserProfileDto.serializer(), dto)
        )

        _profile.value = profile
    }

    internal fun getLocalUpdatedAtEpochMillisOrNull(): Long? {
        val raw = settings.getStringOrNull(keyV1)
            ?: return null

        return runCatching {
            json.decodeFromString(
                UserProfileDto.serializer(),
                raw
            ).updatedAtEpochMillis
        }.getOrNull()
    }

    private fun readCachedOrNull(): UserProfile? {
        val raw = settings.getStringOrNull(keyV1)
            ?: return null

        return runCatching {
            val dto = json.decodeFromString(
                UserProfileDto.serializer(),
                raw
            )

            UserProfile(
                displayName = dto.displayName,
                photoUrl = dto.photoUrl,
                email = dto.email
            )
        }.getOrNull()
    }

    @Serializable
    private data class UserProfileDto(
        val displayName: String?,
        val photoUrl: String?,
        val email: String?,
        val updatedAtEpochMillis: Long
    )
}