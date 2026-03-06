package com.agc.bwitch.presentation.tarot

import com.agc.bwitch.domain.shared.ApiResult
import com.agc.bwitch.domain.tarot.TarotDrawResponse
import com.agc.bwitch.domain.tarot.TarotRepository
import com.agc.bwitch.domain.tarot.TarotRequestType
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

data class TarotUiState(
    val requestId: String? = null,
    val selectedType: TarotRequestType = TarotRequestType.TAROT_1,
    val isLoading: Boolean = false,
    val response: TarotDrawResponse? = null,
    val error: String? = null,
)

class TarotViewModel(
    private val tarotRepository: TarotRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _uiState = MutableStateFlow(TarotUiState())
    val uiState: StateFlow<TarotUiState> = _uiState.asStateFlow()

    fun newRequest(type: TarotRequestType) {
        val requestId = generateRequestId()
        _uiState.update {
            it.copy(
                requestId = requestId,
                selectedType = type,
                response = null,
                error = null,
            )
        }
        draw(requestId, type)
    }

    fun retry() {
        val currentState = _uiState.value
        val requestId = currentState.requestId ?: return
        draw(requestId, currentState.selectedType)
    }

    private fun draw(requestId: String, type: TarotRequestType) {
        scope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }

            when (
                val result = tarotRepository.tarotDraw(
                    requestId = requestId,
                    type = type,
                )
            ) {
                is ApiResult.Ok -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            response = result.value,
                            error = null,
                        )
                    }
                }

                is ApiResult.Err -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            response = null,
                            error = result.error.message ?: "Unknown error",
                        )
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun generateRequestId(): String = Uuid.random().toString()
}
