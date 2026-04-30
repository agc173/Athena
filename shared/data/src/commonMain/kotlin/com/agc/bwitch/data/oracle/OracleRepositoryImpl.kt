package com.agc.bwitch.data.oracle

import com.agc.bwitch.data.functions.FunctionsClient
import com.agc.bwitch.data.oracle.dto.AdUnlockDto
import com.agc.bwitch.data.oracle.dto.OracleAskRequestDto
import com.agc.bwitch.data.oracle.dto.OracleAskResponseDto
import com.agc.bwitch.data.platform.BuildInfo
import com.agc.bwitch.domain.localization.AppLanguage
import com.agc.bwitch.domain.oracle.OracleAnswer
import com.agc.bwitch.domain.oracle.OracleAskRequest
import com.agc.bwitch.domain.oracle.OracleAskResult
import com.agc.bwitch.domain.oracle.OracleQuotaSnapshot
import com.agc.bwitch.domain.oracle.OracleRepository
import com.agc.bwitch.domain.oracle.SystemMode
import com.agc.bwitch.domain.shared.ApiError
import com.agc.bwitch.domain.shared.ApiResult
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer

class OracleRepositoryImpl(
    private val functionsClient: FunctionsClient,
) : OracleRepository {

    override suspend fun getStatus(): ApiResult<SystemMode> {
        return when (
            val result = functionsClient.call(
                name = "oracleGetStatus",
                data = Unit,
                requestSerializer = Unit.serializer(),
                responseSerializer = OracleStatusResponse.serializer(),
            )
        ) {
            is ApiResult.Err -> result
            is ApiResult.Ok -> {
                val modeRaw = result.value.mode

                val mode = runCatching { SystemMode.valueOf(modeRaw) }.getOrNull()
                    ?: return ApiResult.Err(ApiError.Internal("Invalid response: mode=$modeRaw"))

                ApiResult.Ok(mode)
            }
        }
    }

    override suspend fun ask(request: OracleAskRequest): ApiResult<OracleAskResult> {
        val payload = OracleAskRequestDto(
            requestType = REQUEST_TYPE_ORACLE_1Q,
            requestId = request.requestId,
            question = request.question,
            topic = request.topic?.name,
            lang = normalizeLanguageCode(request.lang),
            adUnlock = if (BuildInfo.isDebug) AdUnlockDto(rewardedProof = "dev-test-proof") else null,
        )

        return when (
            val result = functionsClient.call(
                name = "oracleAsk",
                data = payload,
                requestSerializer = OracleAskRequestDto.serializer(),
                responseSerializer = OracleAskResponseDto.serializer(),
            )
        ) {
            is ApiResult.Err -> result
            is ApiResult.Ok -> parseAskResponse(result.value)
        }
    }

    private fun parseAskResponse(response: OracleAskResponseDto): ApiResult<OracleAskResult> {
        return when (response.status) {
            "IN_PROGRESS", "PROCESSING" -> {
                ApiResult.Ok(OracleAskResult.InProgress(requestId = response.requestId, status = response.status))
            }

            "FAILED" -> {
                ApiResult.Err(ApiError.Internal(response.error?.message ?: "Oracle request failed"))
            }

            "COMPLETED_SUCCESS" -> {
                val answer = response.answer
                    ?: return ApiResult.Err(ApiError.Internal("Invalid response: missing answer"))
                val guidance = answer.guidance
                    ?: return ApiResult.Err(ApiError.Internal("Invalid response: missing guidance"))
                val coreGuidance = guidance.core?.trim().orEmpty()
                if (coreGuidance.isBlank()) {
                    return ApiResult.Err(ApiError.Internal("Invalid response: missing guidance.core"))
                }

                val parsedSystemMode = response.systemMode
                    ?.let { raw -> runCatching { SystemMode.valueOf(raw) }.getOrNull() }

                ApiResult.Ok(
                    OracleAskResult.CompletedSuccess(
                        requestId = response.requestId,
                        answer = OracleAnswer(
                            title = answer.title?.takeIf { it.isNotBlank() },
                            coreGuidance = coreGuidance,
                            doList = guidance.doList.orEmpty().filter { it.isNotBlank() },
                            avoidList = guidance.avoidList.orEmpty().filter { it.isNotBlank() },
                            reflection = guidance.reflection?.takeIf { it.isNotBlank() },
                        ),
                        systemMode = parsedSystemMode,
                        quotaSnapshot = response.quotaSnapshot?.let {
                            OracleQuotaSnapshot(
                                maxRequestsRemaining = it.maxRequestsRemaining,
                                adUnlockRemaining = it.adUnlockRemaining,
                            )
                        },
                    )
                )
            }

            else -> ApiResult.Err(ApiError.Internal("Invalid response: unknown status=${response.status}"))
        }
    }

    @Serializable
    private data class OracleStatusResponse(
        val mode: String,
    )

    private companion object {
        const val REQUEST_TYPE_ORACLE_1Q = "ORACLE_1Q"
        const val ORACLE_FALLBACK_LANGUAGE_CODE = "es"
    }

    private fun normalizeLanguageCode(raw: String?): String {
        return AppLanguage.fromCodeOrNull(raw)?.code ?: ORACLE_FALLBACK_LANGUAGE_CODE
    }
}
