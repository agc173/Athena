package com.agc.bwitch.data.account

import com.agc.bwitch.data.functions.FunctionsClient
import com.agc.bwitch.domain.account.AccountDeletionRepository
import com.agc.bwitch.domain.account.AccountDeletionStatus
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.FirebaseFirestore
import dev.gitlive.firebase.firestore.firestore

class FunctionsAccountDeletionRepository(
    functionsClient: FunctionsClient,
    private val firestore: FirebaseFirestore = Firebase.firestore,
) : AccountDeletionRepository {

    private val callableDataSource = AccountDeletionCallableDataSource(functionsClient)

    override suspend fun getStatus(uid: String): AccountDeletionStatus? {
        val snapshot = firestore.collection(ACCOUNT_STATUS_COLLECTION)
            .document(uid)
            .get()
        if (!snapshot.exists) return null
        return snapshot.data(AccountDeletionStatusDto.serializer()).toDomain()
    }

    override suspend fun requestAccountDeletion() {
        callableDataSource.requestAccountDeletion()
    }

    override suspend fun restoreAccount() {
        callableDataSource.restoreAccount()
    }

    private companion object {
        const val ACCOUNT_STATUS_COLLECTION = "userAccountStatus"
    }
}
