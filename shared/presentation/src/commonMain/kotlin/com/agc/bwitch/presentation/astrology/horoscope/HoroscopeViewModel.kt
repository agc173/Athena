package com.agc.bwitch.presentation.astrology.horoscope

import com.agc.bwitch.domain.astrology.horoscope.GetDailyHoroscopeUseCase
import com.agc.bwitch.domain.astrology.horoscope.HoroscopePullMarker
import com.agc.bwitch.domain.astrology.horoscope.ObserveDailyHoroscopeUseCase
import com.agc.bwitch.domain.astrology.horoscope.PullDailyHoroscopeUseCase
import com.agc.bwitch.domain.astrology.horoscope.ZodiacSign
import com.agc.bwitch.domain.astrology.horoscope.ZodiacSignResolver
import com.agc.bwitch.domain.localization.AppLanguage
import com.agc.bwitch.domain.localization.ObserveCurrentLanguageUseCase
import com.agc.bwitch.domain.localization.ResolveCurrentLanguageUseCase
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
import kotlinx.coroutines.flow.distinctUntilChanged
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
    private val resolveCurrentLanguageUseCase: ResolveCurrentLanguageUseCase,
    private val observeCurrentLanguageUseCase: ObserveCurrentLanguageUseCase,
    private val observeUserProfileUseCase: ObserveUserProfileUseCase,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    private val _uiState = MutableStateFlow(HoroscopeUiState(isLoading = true))
    val uiState: StateFlow<HoroscopeUiState> = _uiState.asStateFlow()
    private val currentLanguageCode = MutableStateFlow(AppLanguage.fallback.code)

    private var observeJob: Job? = null
    private var userHasManuallySelectedSign = false

    init {
        scope.launch {
            runCatching { resolveCurrentLanguageUseCase() }
        }

        scope.launch {
            observeCurrentLanguageUseCase()
                .map { it.code }
                .distinctUntilChanged()
                .collectLatest { languageCode ->
                    val hadPreviousLanguage = currentLanguageCode.value != languageCode
                    currentLanguageCode.value = languageCode
                    if (hadPreviousLanguage) {
                        _uiState.update { it.copy(horoscope = null, isLoading = true, errorMessage = null) }
                    }
                    start(sign = _uiState.value.selectedSign, languageCode = languageCode)
                    pullTodayIfNeeded(languageCode = languageCode)
                }
        }

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
            val languageCode = currentLanguageCode.value
            val lastPulled = pullMarker.getLastPulledDateIso(languageCode = languageCode)

            _uiState.update { it.copy(errorMessage = null, infoMessage = null) }

            val hasCached = _uiState.value.horoscope != null

            if (lastPulled == today && hasCached) {
                _uiState.update { it.copy(infoMessage = HoroscopeFeedbackMessage.AlreadyUpdated) }
                return@launch
            }

            safePull(today, languageCode)

            if (_uiState.value.errorMessage == null) {
                pullMarker.setLastPulledDateIso(today, languageCode = languageCode)
                _uiState.update { it.copy(infoMessage = HoroscopeFeedbackMessage.Updated) }
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
        start(sign, languageCode = currentLanguageCode.value)
    }

    private fun start(sign: ZodiacSign, languageCode: String) {
        val dateIso = todayIso()

        observeJob?.cancel()
        observeJob = scope.launch {
            observeDailyHoroscopeUseCase(dateIso = dateIso, sign = sign, languageCode = languageCode).collect { cached ->
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        horoscope = cached,
                        errorMessage = null,
                    )
                }
            }
        }

        scope.launch { getDailyHoroscopeUseCase(dateIso = dateIso, sign = sign, languageCode = languageCode) }
    }

    private suspend fun pullTodayIfNeeded(languageCode: String) {
        val today = todayIso()
        val lastPulled = pullMarker.getLastPulledDateIso(languageCode = languageCode)

        if (lastPulled == today) return

        safePull(today, languageCode)

        if (_uiState.value.errorMessage == null) {
            pullMarker.setLastPulledDateIso(today, languageCode = languageCode)
        }
    }

    private suspend fun safePull(dateIso: String, languageCode: String) {
        _uiState.update { it.copy(isRefreshing = true, errorMessage = null) }
        try {
            pullDailyHoroscopeUseCase(dateIso, languageCode)
        } catch (t: Throwable) {
            println("🔥 Horoscope pull failed: ${t.message}")
            t.printStackTrace()
            _uiState.update { it.copy(errorMessage = HoroscopeFeedbackMessage.RefreshFailed) }
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
