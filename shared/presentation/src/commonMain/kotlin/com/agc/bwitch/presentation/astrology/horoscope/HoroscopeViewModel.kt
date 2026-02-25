package com.agc.bwitch.presentation.astrology.horoscope

import com.agc.bwitch.domain.astrology.horoscope.GetDailyHoroscopeUseCase
import com.agc.bwitch.domain.astrology.horoscope.ObserveDailyHoroscopeUseCase
import com.agc.bwitch.domain.astrology.horoscope.PrefetchDailyHoroscopeUseCase
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
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class HoroscopeViewModel(
    private val observeDailyHoroscopeUseCase: ObserveDailyHoroscopeUseCase,
    private val getDailyHoroscopeUseCase: GetDailyHoroscopeUseCase,
    private val pullDailyHoroscopeUseCase: PullDailyHoroscopeUseCase,
    private val prefetchDailyHoroscopeUseCase: PrefetchDailyHoroscopeUseCase,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    private val _uiState = MutableStateFlow(HoroscopeUiState(isLoading = true))
    val uiState: StateFlow<HoroscopeUiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null

    init {
        // 1) Arranca observación del día actual para el signo seleccionado
        start(sign = _uiState.value.selectedSign)

        // 2) Prefetch optimizado: hoy + próximos 7 días (incluye hoy)
        scope.launch {
            safePrefetch(daysAhead = 7)
        }
    }

    fun onSelectSign(sign: ZodiacSign) {
        _uiState.update { it.copy(selectedSign = sign, errorMessage = null, isLoading = true) }
        start(sign)
    }

    fun onRefresh() {
        // Refresca solo el día actual (lo típico en pull-to-refresh)
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

        // Warmup local (no bloquea, útil para cargar rápido si ya estaba cacheado)
        scope.launch { getDailyHoroscopeUseCase(dateIso = dateIso, sign = sign) }

        // OJO: aquí ya NO hacemos pull inicial.
        // El prefetch del init ya incluye "hoy".
    }

    private suspend fun safePrefetch(daysAhead: Int) {
        try {
            prefetchDailyHoroscopeUseCase(daysAhead = daysAhead)
        } catch (t: Throwable) {
            // Yo lo dejo silencioso para que un fallo puntual de red
            // no pinte error al arrancar la app.
            println("🔥 Horoscope prefetch failed: ${t.message}")
            t.printStackTrace()
        }
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
