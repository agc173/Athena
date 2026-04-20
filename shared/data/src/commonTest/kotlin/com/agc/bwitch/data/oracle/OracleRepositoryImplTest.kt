package com.agc.bwitch.data.oracle

import com.agc.bwitch.data.functions.FunctionsClient
import com.agc.bwitch.data.oracle.dto.OracleAskRequestDto
import com.agc.bwitch.data.oracle.dto.OracleAskResponseDto
import com.agc.bwitch.domain.oracle.OracleAskRequest
import com.agc.bwitch.domain.shared.ApiResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.KSerializer
import kotlinx.coroutines.test.runTest

class OracleRepositoryImplTest {

    @Test
    fun `ask normalizes regional language code before calling backend`() = runTest {
        val client = CapturingFunctionsClient()
        client.nextResponse = ApiResult.Ok(
            OracleAskResponseDto(
                requestId = "req-1",
                status = "IN_PROGRESS",
            )
        )
        val repository = OracleRepositoryImpl(client)

        repository.ask(
            OracleAskRequest(
                requestId = "req-1",
                question = "Hola",
                lang = "pt-BR",
            )
        )

        assertEquals("pt", client.lastPayload?.lang)
    }

    @Test
    fun `ask falls back to es when language is unsupported`() = runTest {
        val client = CapturingFunctionsClient()
        client.nextResponse = ApiResult.Ok(
            OracleAskResponseDto(
                requestId = "req-2",
                status = "IN_PROGRESS",
            )
        )
        val repository = OracleRepositoryImpl(client)

        repository.ask(
            OracleAskRequest(
                requestId = "req-2",
                question = "Hello",
                lang = "xx",
            )
        )

        assertEquals("es", client.lastPayload?.lang)
    }

    private class CapturingFunctionsClient : FunctionsClient {
        var nextResponse: ApiResult<OracleAskResponseDto> =
            ApiResult.Ok(OracleAskResponseDto(requestId = "default", status = "IN_PROGRESS"))
        var lastPayload: OracleAskRequestDto? = null

        override suspend fun <Req : Any, Res : Any> call(
            name: String,
            data: Req,
            requestSerializer: KSerializer<Req>,
            responseSerializer: KSerializer<Res>,
        ): ApiResult<Res> {
            @Suppress("UNCHECKED_CAST")
            lastPayload = data as OracleAskRequestDto
            @Suppress("UNCHECKED_CAST")
            return nextResponse as ApiResult<Res>
        }
    }
}
