package com.agc.bwitch.presentation.astrology.birthchart

import com.agc.bwitch.domain.astrology.birthchart.BirthEssenceDraft
import com.agc.bwitch.domain.astrology.birthchart.BirthEssenceInput
import com.agc.bwitch.domain.astrology.birthchart.GenerateBirthEssenceUseCase
import com.agc.bwitch.domain.astrology.birthchart.GetBirthEssenceUseCase
import com.agc.bwitch.domain.astrology.birthchart.ObserveBirthEssenceUseCase
import com.agc.bwitch.domain.astrology.birthchart.PullBirthEssenceUseCase
import com.agc.bwitch.domain.astrology.birthchart.SaveBirthEssenceUseCase
import com.agc.bwitch.domain.astrology.horoscope.ZodiacSign
import com.agc.bwitch.domain.shared.ApiResult
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BirthChartViewModel(
    private val observeBirthEssence: ObserveBirthEssenceUseCase,
    private val getBirthEssence: GetBirthEssenceUseCase,
    private val saveBirthEssence: SaveBirthEssenceUseCase,
    private val pullBirthEssence: PullBirthEssenceUseCase,
    private val generateBirthEssence: GenerateBirthEssenceUseCase,
) {
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _uiState = MutableStateFlow(BirthChartUiState(isLoading = true))
    val uiState: StateFlow<BirthChartUiState> = _uiState

    init {
        scope.launch {
            observeBirthEssence().collectLatest { essence ->
                _uiState.update { s ->
                    s.copy(
                        isLoading = false,
                        selectedSunSign = essence?.sunSign ?: s.selectedSunSign,
                        selectedMoonSign = essence?.moonSign ?: s.selectedMoonSign,
                        selectedRisingSign = essence?.risingSign ?: s.selectedRisingSign,
                        generatedInterpretation = essence?.interpretation ?: s.generatedInterpretation,
                        generatedArchetype = essence?.archetype ?: s.generatedArchetype,
                        hasSavedEssence = essence != null,
                    )
                }
            }
        }

        scope.launch { getBirthEssence() }
    }

    fun refresh() = scope.launch {
        val s = _uiState.value
        if (s.isBusy) return@launch

        _uiState.update { it.copy(isRefreshing = true, error = null, savedSummary = null) }

        runCatching { pullBirthEssence() }
            .onSuccess { _uiState.update { it.copy(savedSummary = "Esencia sincronizada") } }
            .onFailure { e -> _uiState.update { it.copy(error = e.message ?: "No se pudo refrescar") } }

        _uiState.update { it.copy(isRefreshing = false) }
    }

    fun onSunSignChange(value: ZodiacSign) =
        _uiState.update { it.copy(selectedSunSign = value, error = null, savedSummary = null) }

    fun onMoonSignChange(value: ZodiacSign) =
        _uiState.update { it.copy(selectedMoonSign = value, error = null, savedSummary = null) }

    fun onRisingSignChange(value: ZodiacSign) =
        _uiState.update { it.copy(selectedRisingSign = value, error = null, savedSummary = null) }

    fun discoverEssence() {
        val s = _uiState.value
        if (s.isBusy) return

        scope.launch {
            _uiState.update { it.copy(isGenerating = true, error = null, savedSummary = null) }

            when (
                val result = generateBirthEssence(
                    BirthEssenceInput(
                        sunSign = s.selectedSunSign,
                        moonSign = s.selectedMoonSign,
                        risingSign = s.selectedRisingSign,
                    )
                )
            ) {
                is ApiResult.Ok -> {
                    _uiState.update {
                        it.copy(
                            generatedInterpretation = result.value.interpretation,
                            generatedArchetype = result.value.archetype,
                        )
                    }
                }

                is ApiResult.Err -> {
                    _uiState.update {
                        it.copy(error = result.error.message ?: "No se pudo generar la esencia")
                    }
                }
            }

            _uiState.update { it.copy(isGenerating = false) }
        }
    }

    fun saveActiveEssence() {
        val s = _uiState.value
        val interpretation = s.generatedInterpretation ?: run {
            _uiState.update { it.copy(error = "Primero genera una esencia") }
            return
        }

        if (s.isBusy) return

        scope.launch {
            _uiState.update { it.copy(isSaving = true, error = null, savedSummary = null) }
            runCatching {
                saveBirthEssence(
                    BirthEssenceDraft(
                        sunSign = s.selectedSunSign,
                        moonSign = s.selectedMoonSign,
                        risingSign = s.selectedRisingSign,
                        interpretation = interpretation,
                        archetype = s.generatedArchetype,
                    )
                )
            }
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            hasSavedEssence = true,
                            savedSummary = "Esencia guardada como activa",
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message ?: "No se pudo guardar") }
                }

            _uiState.update { it.copy(isSaving = false) }
        }
    }
}
