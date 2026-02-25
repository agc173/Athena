package com.agc.bwitch.presentation.astrology.horoscope

import com.agc.bwitch.domain.astrology.horoscope.GetDailyHoroscopeUseCase
import com.agc.bwitch.domain.astrology.horoscope.ObserveDailyHoroscopeUseCase
import com.agc.bwitch.domain.astrology.horoscope.PullDailyHoroscopeUseCase
import com.agc.bwitch.domain.astrology.horoscope.ZodiacSign
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime


class HoroscopeViewModel(
    private val observeDailyHoroscopeUseCase: ObserveDailyHoroscopeUseCase,
    private val getDailyHoroscopeUseCase: GetDailyHoroscopeUseCase,
    private val pullDailyHoroscopeUseCase: PullDailyHoroscopeUseCase,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    private val _uiState = MutableStateFlow(HoroscopeUiState(isLoading = true))
    val uiState: StateFlow<HoroscopeUiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null

    init {
        start(sign = _uiState.value.selectedSign)
    }

    fun onSelectSign(sign: ZodiacSign) {
        _uiState.update { it.copy(selectedSign = sign, errorMessage = null, isLoading = true) }
        start(sign)
    }

    fun onRefresh() {
        scope.launch { safePull(currentDateIso()) }
    }

    private fun start(sign: ZodiacSign) {
        val dateIso = todayIso()

        observeJob?.cancel()
        observeJob = scope.launch {
            observeDailyHoroscopeUseCase(dateIso = dateIso, sign = sign).collect { cached ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        horoscope = cached,
                        errorMessage = null,
                    )
                }
            }
        }

        // warmup local (opcional, pero útil para “cargar rápido”)
        scope.launch { getDailyHoroscopeUseCase(dateIso = dateIso, sign = sign) }

        // pull inicial (puedes quitarlo si prefieres solo pull manual)
        scope.launch { safePull(dateIso) }
    }

    private suspend fun safePull(dateIso: String) {
        _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
        try {
            pullDailyHoroscopeUseCase(dateIso)
        } catch (t: Throwable) {
            println("🔥 Horoscope pull failed: ${t.message}")
            t.printStackTrace()
            _uiState.update { it.copy(errorMessage = "No se pudo actualizar el horóscopo") }
        } finally {
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    private fun todayIso(): String {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        return now.date.toString() // yyyy-MM-dd
    }

    private fun currentDateIso(): String = todayIso()
}
