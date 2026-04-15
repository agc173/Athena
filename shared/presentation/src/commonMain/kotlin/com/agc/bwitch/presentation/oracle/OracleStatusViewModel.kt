package com.agc.bwitch.presentation.oracle

import com.agc.bwitch.domain.shared.ApiResult
import com.agc.bwitch.domain.oracle.OracleRepository
import com.agc.bwitch.domain.oracle.SystemMode
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OracleStatusUiState(
    val isLoading: Boolean = false,
    val mode: SystemMode? = null,
    val error: OracleStatusErrorMessage? = null,
)

data class OracleStatusErrorMessage(
    val id: OracleStatusErrorMessageId,
    val rawMessage: String? = null,
)

enum class OracleStatusErrorMessageId {
    UnknownFallback,
    RawBackendMessage,
}

class OracleStatusViewModel(
    private val oracleRepository: OracleRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _uiState = MutableStateFlow(OracleStatusUiState())
    val uiState: StateFlow<OracleStatusUiState> = _uiState.asStateFlow()

    fun refresh() {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (val result = oracleRepository.getStatus()) {
                is ApiResult.Ok -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            mode = result.value,
                            error = null,
                        )
                    }
                }

                is ApiResult.Err -> {
                    _uiState.update {
                        val rawMessage = result.error.message
                        it.copy(
                            isLoading = false,
                            error = if (rawMessage.isNullOrBlank()) {
                                OracleStatusErrorMessage(id = OracleStatusErrorMessageId.UnknownFallback)
                            } else {
                                OracleStatusErrorMessage(
                                    id = OracleStatusErrorMessageId.RawBackendMessage,
                                    rawMessage = rawMessage,
                                )
                            },
                        )
                    }
                }
            }
        }
    }
}
