package com.agc.bwitch.domain.userprofile

import kotlinx.coroutines.flow.Flow

interface UserProfileRepository {

    fun observeUserProfile(): Flow<UserProfile?>

    suspend fun getUserProfile(): UserProfile?

    suspend fun saveUserProfile(profile: UserProfile)
}