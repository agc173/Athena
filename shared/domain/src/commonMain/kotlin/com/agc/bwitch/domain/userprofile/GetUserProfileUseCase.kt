package com.agc.bwitch.domain.userprofile

class GetUserProfileUseCase(
    private val repository: UserProfileRepository
) {
    suspend operator fun invoke(): UserProfile? = repository.getUserProfile()
}