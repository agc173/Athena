package com.agc.bwitch.presentation.astrology.synastry

import com.agc.bwitch.domain.astrology.horoscope.ZodiacSign
import com.agc.bwitch.domain.astrology.synastry.SynastryInput
import com.agc.bwitch.domain.astrology.synastry.SynastryPersonInput
import com.agc.bwitch.domain.astrology.synastry.SynastryReadingGenerator
import com.agc.bwitch.domain.economy.EconomyRepository
import com.agc.bwitch.domain.localization.ObserveCurrentLanguageUseCase
import com.agc.bwitch.domain.localization.ResolveCurrentLanguageUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class SynastryViewModel(
    private val readingGenerator: SynastryReadingGenerator,
    private val economyRepository: EconomyRepository,
    private val resolveCurrentLanguageUseCase: ResolveCurrentLanguageUseCase,
    private val observeCurrentLanguageUseCase: ObserveCurrentLanguageUseCase,
    dispatcher: CoroutineDispatcher = Dispatchers.Main,
) {
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    private val _uiState = MutableStateFlow(SynastryUiState())
    val uiState: StateFlow<SynastryUiState> = _uiState

    init {
        scope.launch {
            runCatching { resolveCurrentLanguageUseCase() }
                .onSuccess { _uiState.update { state -> state.copy(currentLanguageCode = it.code) } }
        }
        scope.launch {
            observeCurrentLanguageUseCase()
                .distinctUntilChanged()
                .collect { language ->
                    _uiState.update { it.copy(currentLanguageCode = language.code) }
                }
        }
    }

    fun onPersonASunSignChange(value: ZodiacSign?) =
        _uiState.update { it.copy(personA = it.personA.copy(sunSign = value), error = null) }

    fun onPersonAMoonSignChange(value: ZodiacSign?) =
        _uiState.update { it.copy(personA = it.personA.copy(moonSign = value), error = null) }

    fun onPersonARisingSignChange(value: ZodiacSign?) =
        _uiState.update { it.copy(personA = it.personA.copy(risingSign = value), error = null) }

    fun onPersonBSunSignChange(value: ZodiacSign?) =
        _uiState.update { it.copy(personB = it.personB.copy(sunSign = value), error = null) }

    fun onPersonBMoonSignChange(value: ZodiacSign?) =
        _uiState.update { it.copy(personB = it.personB.copy(moonSign = value), error = null) }

    fun onPersonBRisingSignChange(value: ZodiacSign?) =
        _uiState.update { it.copy(personB = it.personB.copy(risingSign = value), error = null) }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun generate() {
        val state = _uiState.value
        if (!state.canGenerate) {
            _uiState.update { it.copy(error = "required_sun_signs_error") }
            return
        }

        val personASun = state.personA.sunSign ?: return
        val personBSun = state.personB.sunSign ?: return

        scope.launch {
            _uiState.update { it.copy(isGenerating = true, error = null) }

            runCatching {
                val requestId = generateRequestId()
                val authorization = economyRepository.authorizeSynastry(
                    requestId = requestId,
                    languageCode = state.currentLanguageCode,
                )
                if (!authorization.authorized) {
                    val status = authorization.status
                    if (status == "IN_PROGRESS") {
                        throw IllegalStateException("synastry_in_progress")
                    }
                    throw IllegalStateException(status ?: "synastry_not_authorized")
                }
                readingGenerator(
                    SynastryInput(
                        personA = SynastryPersonInput(
                            sunSign = personASun,
                            moonSign = state.personA.moonSign,
                            risingSign = state.personA.risingSign,
                        ),
                        personB = SynastryPersonInput(
                            sunSign = personBSun,
                            moonSign = state.personB.moonSign,
                            risingSign = state.personB.risingSign,
                        ),
                        languageCode = state.currentLanguageCode,
                    )
                )
            }.onSuccess { reading ->
                _uiState.update { it.copy(reading = reading, isGenerating = false, error = null) }
            }.onFailure { throwable ->
                val normalized = throwable.message?.lowercase().orEmpty()
                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        error = when {
                            normalized.contains("synastry_in_progress") -> null
                            normalized.contains("daily_limit") ||
                                normalized.contains("synastry_daily_limit_reached") ||
                                normalized.contains("daily") -> "daily_limit"
                            normalized.contains("insufficient_moons") ||
                                normalized.contains("insufficient_moon_balance") ||
                                normalized.contains("failed-precondition") ||
                                normalized.contains("moon") ||
                                normalized.contains("resource-exhausted") -> "insufficient_moons"
                            else -> "generic_generate_error"
                        },
                    )
                }
            }
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun generateRequestId(): String = Uuid.random().toString()
}
