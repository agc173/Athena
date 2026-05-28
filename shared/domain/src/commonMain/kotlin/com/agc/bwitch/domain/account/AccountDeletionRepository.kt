package com.agc.bwitch.domain.account

interface AccountDeletionRepository {
    suspend fun getStatus(uid: String): AccountDeletionStatus?
    suspend fun requestAccountDeletion()
    suspend fun restoreAccount()
}
