package com.agc.bwitch.domain.userprofile

class PullUserProfileUseCase(
    private val sync: UserProfileSyncController
) {
    suspend operator fun invoke() = sync.pull()
}