package com.agc.bwitch.presentation.astrology.horoscope

import com.agc.bwitch.domain.astrology.horoscope.GetDailyHoroscopeUseCase
import com.agc.bwitch.domain.astrology.horoscope.ZodiacSign
import com.agc.bwitch.domain.model.ApiResult
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class HoroscopeViewModel(
    private val getDailyHoroscopeUseCase: GetDailyHoroscopeUseCase,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    private val _uiState = MutableStateFlow(HoroscopeUiState(isLoading = true))
    val uiState: StateFlow<HoroscopeUiState> = _uiState.asStateFlow()

    init {
        loadHoroscope(sign = _uiState.value.selectedSign)
    }

    fun onSelectSign(sign: ZodiacSign) {
        _uiState.update { current -> current.copy(selectedSign = sign) }
        loadHoroscope(sign)
    }

    fun onRefresh() {
        loadHoroscope(sign = _uiState.value.selectedSign)
    }

    private fun loadHoroscope(sign: ZodiacSign) {
        scope.launch {
            _uiState.update { current ->
                current.copy(
                    isLoading = true,
                    errorMessage = null,
                )
            }

            when (val result = getDailyHoroscopeUseCase(sign = sign)) {
                is ApiResult.Success -> {
                    _uiState.update { current ->
                        current.copy(
                            isLoading = false,
                            horoscope = result.data,
                            errorMessage = null,
                        )
                    }
                }

                is ApiResult.Failure -> {
                    _uiState.update { current ->
                        current.copy(
                            isLoading = false,
                            horoscope = null,
                            errorMessage = "No se pudo cargar el horóscopo",
                        )
                    }
                }
            }
        }
    }
}
