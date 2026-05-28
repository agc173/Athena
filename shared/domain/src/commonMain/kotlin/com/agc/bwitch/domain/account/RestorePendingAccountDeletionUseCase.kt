package com.agc.bwitch.domain.account

class RestorePendingAccountDeletionUseCase(
    private val repository: AccountDeletionRepository,
) {
    suspend operator fun invoke(uid: String): Boolean {
        val status = repository.getStatus(uid) ?: return false
        if (!status.pendingDeletion) return false
        repository.restoreAccount()
        return true
    }
}
