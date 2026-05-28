package com.agc.bwitch.domain.account

class RestoreAccountUseCase(
    private val repository: AccountDeletionRepository,
) {
    suspend operator fun invoke() = repository.restoreAccount()
}
