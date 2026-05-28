package com.agc.bwitch.data.account

import com.agc.bwitch.data.functions.FunctionsClient
import com.agc.bwitch.domain.shared.ApiResult
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.KSerializer
import kotlin.test.Test
import kotlin.test.assertEquals

class AccountDeletionCallableDataSourceTest {
    @Test
    fun `request deletion calls backend callable`() = runTest {
        val client = CapturingFunctionsClient()
        val dataSource = AccountDeletionCallableDataSource(client)

        dataSource.requestAccountDeletion()

        assertEquals("requestAccountDeletion", client.lastName)
    }

    @Test
    fun `restore clears pending through backend callable`() = runTest {
        val client = CapturingFunctionsClient()
        val dataSource = AccountDeletionCallableDataSource(client)

        dataSource.restoreAccount()

        assertEquals("restoreAccount", client.lastName)
    }

    private class CapturingFunctionsClient : FunctionsClient {
        var lastName: String? = null

        override suspend fun <Req : Any, Res : Any> call(
            name: String,
            data: Req,
            requestSerializer: KSerializer<Req>,
            responseSerializer: KSerializer<Res>,
        ): ApiResult<Res> {
            lastName = name
            @Suppress("UNCHECKED_CAST")
            return ApiResult.Ok(AccountDeletionResponseDto(success = true) as Res)
        }
    }
}
