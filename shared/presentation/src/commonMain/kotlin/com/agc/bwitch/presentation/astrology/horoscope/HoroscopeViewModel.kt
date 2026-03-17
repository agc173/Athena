package com.agc.bwitch.presentation.astrology.horoscope

import com.agc.bwitch.domain.astrology.horoscope.GetDailyHoroscopeUseCase
import com.agc.bwitch.domain.astrology.horoscope.HoroscopePullMarker
import com.agc.bwitch.domain.astrology.horoscope.ObserveDailyHoroscopeUseCase
import com.agc.bwitch.domain.astrology.horoscope.PullDailyHoroscopeUseCase
import com.agc.bwitch.domain.astrology.horoscope.ZodiacSign
import com.agc.bwitch.domain.astrology.horoscope.ZodiacSignResolver
import com.agc.bwitch.domain.userprofile.ObserveUserProfileUseCase
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class HoroscopeViewModel(
    private val observeDailyHoroscopeUseCase: ObserveDailyHoroscopeUseCase,
    private val getDailyHoroscopeUseCase: GetDailyHoroscopeUseCase,
    private val pullDailyHoroscopeUseCase: PullDailyHoroscopeUseCase,
    private val pullMarker: HoroscopePullMarker,
    private val observeUserProfileUseCase: ObserveUserProfileUseCase,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    private val _uiState = MutableStateFlow(HoroscopeUiState(isLoading = true))
    val uiState: StateFlow<HoroscopeUiState> = _uiState.asStateFlow()

    private var observeJob: Job? = null
    private var userHasManuallySelectedSign = false

    init {
        start(sign = _uiState.value.selectedSign)

        scope.launch { pullTodayIfNeeded() }

        scope.launch {
            observeUserProfileUseCase()
                .map { profile ->
                    profile?.zodiacSign ?: profile?.birthDate?.let(ZodiacSignResolver::fromBirthDate)
                }
                .filterNotNull()
                .collectLatest { profileSign ->
                    if (userHasManuallySelectedSign) return@collectLatest
                    if (_uiState.value.selectedSign == profileSign) return@collectLatest
                    onSelectSign(profileSign, fromUserInteraction = false)
                }
        }
    }

    fun onSelectSign(sign: ZodiacSign) {
        onSelectSign(sign, fromUserInteraction = true)
    }

    fun onRefresh() {
        scope.launch {
            val today = currentDateIso()
            val lastPulled = pullMarker.getLastPulledDateIso()

            _uiState.update { it.copy(errorMessage = null, infoMessage = null) }

            val hasCached = _uiState.value.horoscope != null

            if (lastPulled == today && hasCached) {
                _uiState.update { it.copy(infoMessage = "Ya está actualizado") }
                return@launch
            }

            safePull(today)

            if (_uiState.value.errorMessage == null) {
                pullMarker.setLastPulledDateIso(today)
                _uiState.update { it.copy(infoMessage = "Actualizado") }
            }
        }
    }

    fun onInfoShown() {
        _uiState.update { it.copy(infoMessage = null) }
    }

    fun onErrorShown() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun onSelectSign(sign: ZodiacSign, fromUserInteraction: Boolean) {
        if (fromUserInteraction) {
            userHasManuallySelectedSign = true
        }
        _uiState.update { it.copy(selectedSign = sign, errorMessage = null, isLoading = true) }
        start(sign)
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

        scope.launch { getDailyHoroscopeUseCase(dateIso = dateIso, sign = sign) }
    }

    private suspend fun pullTodayIfNeeded() {
        val today = todayIso()
        val lastPulled = pullMarker.getLastPulledDateIso()

        if (lastPulled == today) return

        safePull(today)

        if (_uiState.value.errorMessage == null) {
            pullMarker.setLastPulledDateIso(today)
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
        return now.date.toString()
    }

    private fun currentDateIso(): String = todayIso()
}
