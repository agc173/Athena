package com.agc.bwitch.data.account

import com.agc.bwitch.data.functions.FunctionsClient
import com.agc.bwitch.domain.shared.ApiError
import com.agc.bwitch.domain.shared.ApiResult

internal class AccountDeletionCallableDataSource(
    private val functionsClient: FunctionsClient,
) {
    suspend fun requestAccountDeletion() {
        call(REQUEST_ACCOUNT_DELETION_CALLABLE, "No se pudo solicitar la eliminación de la cuenta")
    }

    suspend fun restoreAccount() {
        call(RESTORE_ACCOUNT_CALLABLE, "No se pudo restaurar la cuenta")
    }

    private suspend fun call(name: String, fallback: String) {
        when (val result = functionsClient.call(
            name = name,
            data = AccountDeletionEmptyRequestDto(),
            requestSerializer = AccountDeletionEmptyRequestDto.serializer(),
            responseSerializer = AccountDeletionResponseDto.serializer(),
        )) {
            is ApiResult.Ok -> return
            is ApiResult.Err -> throw result.error.toAccountDeletionException(fallback)
        }
    }

    private fun ApiError.toAccountDeletionException(fallback: String): IllegalStateException =
        IllegalStateException(message ?: fallback)

    private companion object {
        const val REQUEST_ACCOUNT_DELETION_CALLABLE = "requestAccountDeletion"
        const val RESTORE_ACCOUNT_CALLABLE = "restoreAccount"
    }
}
