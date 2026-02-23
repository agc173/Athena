package com.agc.bwitch.domain.userprofile

import kotlinx.coroutines.flow.Flow

class ObserveUserProfileUseCase(
    private val repository: UserProfileRepository
) {
    operator fun invoke(): Flow<UserProfile?> = repository.observeUserProfile()
}