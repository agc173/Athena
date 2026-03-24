package com.agc.bwitch.presentation.astrology.synastry

import com.agc.bwitch.domain.astrology.horoscope.ZodiacSign
import com.agc.bwitch.domain.astrology.synastry.SynastryInput
import com.agc.bwitch.domain.astrology.synastry.SynastryPersonInput
import com.agc.bwitch.domain.astrology.synastry.SynastryReadingGenerator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class SynastryViewModel(
    private val readingGenerator: SynastryReadingGenerator,
) {
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _uiState = MutableStateFlow(SynastryUiState())
    val uiState: StateFlow<SynastryUiState> = _uiState

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
            _uiState.update { it.copy(error = "El signo solar es obligatorio para ambas personas") }
            return
        }

        val personASun = state.personA.sunSign ?: return
        val personBSun = state.personB.sunSign ?: return

        scope.launch {
            _uiState.update { it.copy(isGenerating = true, error = null) }

            runCatching {
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
                    )
                )
            }.onSuccess { reading ->
                _uiState.update { it.copy(reading = reading, isGenerating = false, error = null) }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isGenerating = false,
                        error = throwable.message ?: "No se pudo generar la lectura de sinastría",
                    )
                }
            }
        }
    }
}
