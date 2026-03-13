package com.agc.bwitch.presentation.oracle

import com.agc.bwitch.domain.oracle.OracleAnswer
import com.agc.bwitch.domain.oracle.OracleAskRequest
import com.agc.bwitch.domain.oracle.OracleAskResult
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
    val inProgress: Boolean = false,
    val error: String? = null,
)

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
                result = null,
                inProgress = false,
            )
        }
    }

    fun ask(topic: OracleTopic? = null, lang: String? = null) {
        val trimmedQuestion = _uiState.value.question.trim().take(MAX_QUESTION_LENGTH)
        if (trimmedQuestion.isBlank()) {
            _uiState.update { it.copy(error = "Escribe una pregunta para consultar al Oráculo") }
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

    private fun ApiError.toUserMessage(): String {
        return when (this) {
            is ApiError.Unauthenticated -> "Necesitas iniciar sesión para usar el Oráculo"
            is ApiError.PermissionDenied -> "No tienes permisos para completar esta consulta"
            is ApiError.ResourceExhausted -> message ?: "Has alcanzado el límite diario del Oráculo"
            is ApiError.FailedPrecondition -> message ?: "No se pudo completar la consulta en este momento"
            is ApiError.InvalidArgument -> message ?: "La pregunta no es válida"
            is ApiError.Internal -> message ?: "Error interno del Oráculo"
            is ApiError.Unknown -> message ?: "No se pudo conectar con el Oráculo"
        }
    }
}
