package com.agc.bwitch.domain.session

class ClearLocalUserDataUseCase(
    private val repo: LocalUserDataRepository
) {
    suspend operator fun invoke() = repo.clear()
}