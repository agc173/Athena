package com.agc.bwitch.presentation.astrology.birthchart

import com.agc.bwitch.domain.astrology.birthchart.BirthEssenceDraft
import com.agc.bwitch.domain.astrology.birthchart.BirthEssenceInput
import com.agc.bwitch.domain.astrology.birthchart.GenerateBirthEssenceUseCase
import com.agc.bwitch.domain.astrology.birthchart.GetBirthEssenceUseCase
import com.agc.bwitch.domain.astrology.birthchart.ObserveBirthEssenceUseCase
import com.agc.bwitch.domain.astrology.birthchart.PullBirthEssenceUseCase
import com.agc.bwitch.domain.astrology.birthchart.SaveBirthEssenceUseCase
import com.agc.bwitch.domain.astrology.horoscope.ZodiacSign
import com.agc.bwitch.domain.shared.ApiError
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
                        generatedInterpretation = essence?.interpretation?.sanitizeInterpretation()
                            ?: s.generatedInterpretation,
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

        val localBeforeSync = runCatching { getBirthEssence() }.getOrNull()

        runCatching { pullBirthEssence() }
            .onSuccess {
                val localAfterSync = runCatching { getBirthEssence() }.getOrNull()
                val summary = when {
                    localAfterSync == null -> "No había esencia para sincronizar"
                    localBeforeSync == null -> "Esencia remota cargada"
                    localAfterSync.updatedAtEpochMillis > (localBeforeSync.updatedAtEpochMillis) ->
                        "Esencia actualizada desde sincronización"

                    else -> "Tu esencia ya estaba al día"
                }
                _uiState.update { it.copy(savedSummary = summary) }
            }
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
                            generatedInterpretation = result.value.interpretation.sanitizeInterpretation(),
                            generatedArchetype = result.value.archetype,
                        )
                    }
                }

                is ApiResult.Err -> {
                    _uiState.update {
                        it.copy(error = mapGenerateError(result.error))
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

    private fun mapGenerateError(error: ApiError): String {
        val rawMessage = error.message.orEmpty()
        if (rawMessage.contains("not_found", ignoreCase = true)) {
            return "La generación de esencia no está disponible todavía"
        }

        return rawMessage.ifBlank { "No se pudo generar la esencia" }
    }

    private fun String.sanitizeInterpretation(): String {
        val interpretationPrefixRegex =
            Regex("^\\s*interpretaci[oó]n\\b\\s*[:\\-–—]?\\s*", RegexOption.IGNORE_CASE)
        val interpretationJsonKeyValueRegex =
            Regex("^\\s*[\"']?\\s*interpretaci[oó]n\\s*[\"']?\\s*:\\s*(.+?)\\s*$", RegexOption.IGNORE_CASE)
        return this
            .replace("\r\n", "\n")
            .replace('\r', '\n')
            .trim()
            .removeSurrounding("{", "}")
            .lineSequence()
            .map { it.trim() }
            .mapNotNull { line ->
                when {
                    line.startsWith("ARQUETIPO", ignoreCase = true) -> null
                    else -> {
                        val extractedValue =
                            interpretationJsonKeyValueRegex.matchEntire(line)?.groupValues?.get(1)
                                ?: line.replace(interpretationPrefixRegex, "")

                        extractedValue.trim().removeSurroundingQuote().ifBlank { null }
                    }
                }
            }
            .joinToString("\n")
            .trim()
            .removeSurroundingQuote()
    }

    private fun String.removeSurroundingQuote(): String =
        when {
            length >= 2 && startsWith("\"") && endsWith("\"") -> substring(1, length - 1).trim()
            length >= 2 && startsWith("'") && endsWith("'") -> substring(1, length - 1).trim()
            else -> this
        }
}
