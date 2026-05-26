package com.agc.bwitch.domain.notifications

class UnregisterPushTokenUseCase(
    private val repository: PushRegistrationRepository,
) {
    suspend operator fun invoke(token: String, platform: PushPlatform) =
        repository.unregisterToken(token, platform)
}
