package com.agc.bwitch.presentation.oracle

import com.agc.bwitch.domain.oracle.OracleAnswer
import com.agc.bwitch.domain.oracle.OracleAskRequest
import com.agc.bwitch.domain.oracle.OracleAskResult
import com.agc.bwitch.domain.oracle.OracleQuotaSnapshot
import com.agc.bwitch.domain.oracle.OracleRepository
import com.agc.bwitch.domain.oracle.OracleTopic
import com.agc.bwitch.domain.shared.ApiError
import com.agc.bwitch.domain.shared.ApiResult
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val MAX_QUESTION_LENGTH = 400

data class OracleAskUiState(
    val question: String = "",
    val isLoading: Boolean = false,
    val requestId: String? = null,
    val result: OracleAskResult? = null,
    val answer: OracleAnswer? = null,
    val quotaSnapshot: OracleQuotaSnapshot? = null,
    val inProgress: Boolean = false,
    val error: OracleAskMessage? = null,
)

data class OracleAskMessage(
    val id: OracleAskMessageId,
    val rawMessage: String? = null,
)

enum class OracleAskMessageId {
    EmptyQuestion,
    Unauthenticated,
    PermissionDenied,
    ResourceExhaustedWithAdUnlock,
    ResourceExhaustedGeneric,
    FailedPreconditionWithAdUnlock,
    FailedPreconditionTemporaryUnavailable,
    FailedPreconditionGeneric,
    InvalidArgumentFallback,
    InternalTemporaryUnavailable,
    InternalGeneric,
    UnknownFallback,
    RawBackendMessage,
}

class OracleAskViewModel(
    private val oracleRepository: OracleRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _uiState = MutableStateFlow(OracleAskUiState())
    val uiState: StateFlow<OracleAskUiState> = _uiState.asStateFlow()

    fun onQuestionChange(value: String) {
        _uiState.update {
            it.copy(
                question = value,
                error = null,
                answer = null,
                quotaSnapshot = null,
                result = null,
                inProgress = false,
            )
        }
    }

    fun ask(topic: OracleTopic? = null, lang: String? = null) {
        val trimmedQuestion = _uiState.value.question.trim().take(MAX_QUESTION_LENGTH)
        if (trimmedQuestion.isBlank()) {
            _uiState.update {
                it.copy(
                    error = OracleAskMessage(
                        id = OracleAskMessageId.EmptyQuestion,
                    )
                )
            }
            return
        }

        val requestId = generateRequestId()
        _uiState.update {
            it.copy(
                question = trimmedQuestion,
                requestId = requestId,
                isLoading = true,
                error = null,
                inProgress = false,
                result = null,
                answer = null,
                quotaSnapshot = null,
            )
        }

        scope.launch {
            when (
                val result = oracleRepository.ask(
                    OracleAskRequest(
                        requestId = requestId,
                        question = trimmedQuestion,
                        topic = topic,
                        lang = lang,
                    )
                )
            ) {
                is ApiResult.Ok -> {
                    val oracleResult = result.value
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            result = oracleResult,
                            answer = (oracleResult as? OracleAskResult.CompletedSuccess)?.answer,
                            quotaSnapshot = (oracleResult as? OracleAskResult.CompletedSuccess)?.quotaSnapshot,
                            inProgress = oracleResult is OracleAskResult.InProgress,
                            error = null,
                        )
                    }
                }

                is ApiResult.Err -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            result = null,
                            answer = null,
                            quotaSnapshot = null,
                            inProgress = false,
                            error = result.error.toUserMessage(),
                        )
                    }
                }
            }
        }
    }

    fun retry(topic: OracleTopic? = null, lang: String? = null) {
        ask(topic = topic, lang = lang)
    }

    fun startNewConsultation() {
        _uiState.value = OracleAskUiState()
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun generateRequestId(): String = Uuid.random().toString()

    private fun ApiError.toUserMessage(): OracleAskMessage {
        val backendMessage = message.orEmpty()
        return when (this) {
            is ApiError.Unauthenticated -> OracleAskMessage(id = OracleAskMessageId.Unauthenticated)
            is ApiError.PermissionDenied -> OracleAskMessage(id = OracleAskMessageId.PermissionDenied)
            is ApiError.ResourceExhausted -> when {
                backendMessage.hasAdUnlockHint() -> OracleAskMessage(id = OracleAskMessageId.ResourceExhaustedWithAdUnlock)
                else -> OracleAskMessage(id = OracleAskMessageId.ResourceExhaustedGeneric)
            }
            is ApiError.FailedPrecondition -> when {
                backendMessage.hasAdUnlockHint() -> OracleAskMessage(id = OracleAskMessageId.FailedPreconditionWithAdUnlock)
                backendMessage.isTemporaryUnavailabilityHint() -> OracleAskMessage(id = OracleAskMessageId.FailedPreconditionTemporaryUnavailable)
                else -> OracleAskMessage(id = OracleAskMessageId.FailedPreconditionGeneric)
            }
            is ApiError.InvalidArgument -> {
                if (message.isNullOrBlank()) {
                    OracleAskMessage(id = OracleAskMessageId.InvalidArgumentFallback)
                } else {
                    OracleAskMessage(id = OracleAskMessageId.RawBackendMessage, rawMessage = message)
                }
            }
            is ApiError.Internal -> when {
                backendMessage.isTemporaryUnavailabilityHint() -> OracleAskMessage(id = OracleAskMessageId.InternalTemporaryUnavailable)
                else -> OracleAskMessage(id = OracleAskMessageId.InternalGeneric)
            }
            is ApiError.Unknown -> {
                if (message.isNullOrBlank()) {
                    OracleAskMessage(id = OracleAskMessageId.UnknownFallback)
                } else {
                    OracleAskMessage(id = OracleAskMessageId.RawBackendMessage, rawMessage = message)
                }
            }
        }
    }

    private fun String.hasAdUnlockHint(): Boolean {
        val normalized = lowercase()
        return normalized.contains("rewardedproof") ||
            normalized.contains("ad unlock") ||
            normalized.contains("unlock by ad") ||
            normalized.contains("rewarded")
    }

    private fun String.isTemporaryUnavailabilityHint(): Boolean {
        val normalized = lowercase()
        return normalized.contains("temporar") ||
            normalized.contains("unavailable") ||
            normalized.contains("not available") ||
            normalized.contains("maintenance") ||
            normalized.contains("try again later")
    }
}
