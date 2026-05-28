package com.agc.bwitch.domain.account

class RequestAccountDeletionUseCase(
    private val repository: AccountDeletionRepository,
) {
    suspend operator fun invoke() = repository.requestAccountDeletion()
}
