package com.agc.bwitch.domain.notifications

class RegisterPushTokenUseCase(
    private val repository: PushRegistrationRepository,
) {
    suspend operator fun invoke(payload: PushTokenRegistration) = repository.registerToken(payload)
}
