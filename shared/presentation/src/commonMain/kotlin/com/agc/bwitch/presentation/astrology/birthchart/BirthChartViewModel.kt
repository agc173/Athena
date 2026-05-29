package com.agc.bwitch.presentation.astrology.birthchart

import com.agc.bwitch.domain.astrology.birthchart.BirthEssenceDraft
import com.agc.bwitch.domain.astrology.birthchart.BirthEssenceInput
import com.agc.bwitch.domain.astrology.birthchart.GenerateBirthEssenceUseCase
import com.agc.bwitch.domain.astrology.birthchart.GetBirthEssenceUseCase
import com.agc.bwitch.domain.astrology.birthchart.ObserveBirthEssenceUseCase
import com.agc.bwitch.domain.astrology.birthchart.PullBirthEssenceUseCase
import com.agc.bwitch.domain.astrology.birthchart.SaveBirthEssenceUseCase
import com.agc.bwitch.domain.astrology.horoscope.ZodiacSign
import com.agc.bwitch.domain.localization.ObserveCurrentLanguageUseCase
import com.agc.bwitch.domain.localization.ResolveCurrentLanguageUseCase
import com.agc.bwitch.domain.model.DeckCardUnlockReward
import com.agc.bwitch.domain.shared.ApiError
import com.agc.bwitch.domain.shared.ApiResult
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BirthChartViewModel(
    private val observeBirthEssence: ObserveBirthEssenceUseCase,
    private val getBirthEssence: GetBirthEssenceUseCase,
    private val saveBirthEssence: SaveBirthEssenceUseCase,
    private val pullBirthEssence: PullBirthEssenceUseCase,
    private val generateBirthEssence: GenerateBirthEssenceUseCase,
    private val resolveCurrentLanguageUseCase: ResolveCurrentLanguageUseCase,
    private val observeCurrentLanguageUseCase: ObserveCurrentLanguageUseCase,
) {
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _uiState = MutableStateFlow(BirthChartUiState(isLoading = true))
    val uiState: StateFlow<BirthChartUiState> = _uiState
    private val _uiEffects = MutableSharedFlow<BirthChartUiEffect>(extraBufferCapacity = 16)
    val uiEffects: SharedFlow<BirthChartUiEffect> = _uiEffects.asSharedFlow()

    init {
        scope.launch {
            runCatching { resolveCurrentLanguageUseCase() }
        }

        scope.launch {
            observeCurrentLanguageUseCase()
                .map { it.code }
                .distinctUntilChanged()
                .collectLatest { languageCode ->
                    _uiState.update {
                        it.copy(
                            currentLanguageCode = languageCode,
                            generatedInterpretation = null,
                            generatedArchetype = null,
                            generatedDeckCardUnlockRewards = emptyList(),
                            generatedSunSign = null,
                            generatedMoonSign = null,
                            generatedRisingSign = null,
                            generatedLanguageCode = languageCode,
                            requestId = null,
                            inProgress = false,
                            error = null,
                        )
                    }
                    getBirthEssence()
                }
        }

        scope.launch {
            observeBirthEssence().collectLatest { essence ->
                _uiState.update { s ->
                    val matchesCurrentLanguage = essence?.languageCode == null ||
                        essence.languageCode == s.currentLanguageCode
                    s.copy(
                        isLoading = false,
                        selectedSunSign = essence?.sunSign ?: s.selectedSunSign,
                        selectedMoonSign = essence?.moonSign ?: s.selectedMoonSign,
                        selectedRisingSign = essence?.risingSign ?: s.selectedRisingSign,
                        generatedInterpretation = if (matchesCurrentLanguage) {
                            essence?.interpretation?.sanitizeInterpretation() ?: s.generatedInterpretation
                        } else {
                            s.generatedInterpretation
                        },
                        generatedArchetype = if (matchesCurrentLanguage) {
                            essence?.archetype ?: s.generatedArchetype
                        } else {
                            s.generatedArchetype
                        },
                        generatedSunSign = if (matchesCurrentLanguage) {
                            essence?.sunSign ?: s.generatedSunSign
                        } else {
                            s.generatedSunSign
                        },
                        generatedMoonSign = if (matchesCurrentLanguage) {
                            essence?.moonSign ?: s.generatedMoonSign
                        } else {
                            s.generatedMoonSign
                        },
                        generatedRisingSign = if (matchesCurrentLanguage) {
                            essence?.risingSign ?: s.generatedRisingSign
                        } else {
                            s.generatedRisingSign
                        },
                        generatedLanguageCode = if (matchesCurrentLanguage) {
                            essence?.languageCode ?: s.generatedLanguageCode
                        } else {
                            s.generatedLanguageCode
                        },
                        hasSavedEssence = matchesCurrentLanguage && essence != null,
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
                    localAfterSync == null -> BIRTH_CHART_SYNC_NO_ESSENCE_KEY
                    localBeforeSync == null -> BIRTH_CHART_SYNC_REMOTE_LOADED_KEY
                    localAfterSync.updatedAtEpochMillis > (localBeforeSync.updatedAtEpochMillis) ->
                        BIRTH_CHART_SYNC_UPDATED_KEY

                    else -> BIRTH_CHART_SYNC_UP_TO_DATE_KEY
                }
                _uiState.update { it.copy(savedSummary = summary) }
            }
            .onFailure { e -> _uiState.update { it.copy(error = e.message ?: BIRTH_CHART_REFRESH_ERROR_KEY) } }

        _uiState.update { it.copy(isRefreshing = false) }
    }

    fun onSunSignChange(value: ZodiacSign) =
        _uiState.update {
            it.copy(
                selectedSunSign = value,
                requestId = null,
                inProgress = false,
                error = null,
                savedSummary = null,
            )
        }

    fun onMoonSignChange(value: ZodiacSign) =
        _uiState.update {
            it.copy(
                selectedMoonSign = value,
                requestId = null,
                inProgress = false,
                error = null,
                savedSummary = null,
            )
        }

    fun onRisingSignChange(value: ZodiacSign) =
        _uiState.update {
            it.copy(
                selectedRisingSign = value,
                requestId = null,
                inProgress = false,
                error = null,
                savedSummary = null,
            )
        }

    fun discoverEssence() {
        val s = _uiState.value
        if (s.isBusy) return

        val requestId = if (s.requestId != null && (s.error != null || s.inProgress)) {
            s.requestId
        } else {
            generateRequestId()
        }

        scope.launch {
            _uiState.update {
                it.copy(
                    isGenerating = true,
                    requestId = requestId,
                    inProgress = false,
                    error = null,
                    savedSummary = null,
                )
            }

            when (
                val result = generateBirthEssence(
                    BirthEssenceInput(
                        sunSign = s.selectedSunSign,
                        moonSign = s.selectedMoonSign,
                        risingSign = s.selectedRisingSign,
                        languageCode = s.currentLanguageCode,
                        requestId = requestId,
                    )
                )
            ) {
                is ApiResult.Ok -> {
                    val reading = result.value
                    if (reading.status == "IN_PROGRESS" || reading.status == "PROCESSING") {
                        _uiState.update { it.copy(inProgress = true, error = null) }
                    } else {
                        emitDeckUnlockRewardsIfNeeded(reading.deckCardUnlockRewards)
                        _uiState.update {
                            it.copy(
                                generatedInterpretation = reading.interpretation.sanitizeInterpretation(),
                                generatedLanguageCode = reading.languageCode,
                                generatedArchetype = reading.archetype,
                                generatedDeckCardUnlockRewards = reading.deckCardUnlockRewards,
                                generatedSunSign = s.selectedSunSign,
                                generatedMoonSign = s.selectedMoonSign,
                                generatedRisingSign = s.selectedRisingSign,
                                inProgress = false,
                            )
                        }
                    }
                }

                is ApiResult.Err -> {
                    _uiState.update {
                        it.copy(inProgress = false, error = mapGenerateError(result.error))
                    }
                }
            }

            _uiState.update { it.copy(isGenerating = false) }
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun generateRequestId(): String = Uuid.random().toString()

    private fun emitDeckUnlockRewardsIfNeeded(rewards: List<DeckCardUnlockReward>) {
        if (rewards.isEmpty()) return
        _uiEffects.tryEmit(BirthChartUiEffect.ShowDeckCardUnlockRewards(rewards))
    }

    fun saveActiveEssence() {
        val s = _uiState.value
        val interpretation = s.generatedInterpretation ?: run {
            _uiState.update { it.copy(error = BIRTH_CHART_GENERATE_FIRST_ERROR_KEY) }
            return
        }

        if (s.isBusy) return

        scope.launch {
            _uiState.update { it.copy(isSaving = true, error = null, savedSummary = null) }
            runCatching {
                saveBirthEssence(
                    BirthEssenceDraft(
                        sunSign = s.generatedSunSign ?: s.selectedSunSign,
                        moonSign = s.generatedMoonSign ?: s.selectedMoonSign,
                        risingSign = s.generatedRisingSign ?: s.selectedRisingSign,
                        interpretation = interpretation,
                        languageCode = s.generatedLanguageCode,
                        archetype = s.generatedArchetype,
                    )
                )
            }
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            hasSavedEssence = true,
                            savedSummary = BIRTH_CHART_SAVE_SUCCESS_SUMMARY_KEY,
                        )
                    }
                }
                .onFailure { e ->
                    _uiState.update { it.copy(error = e.message ?: BIRTH_CHART_SAVE_ERROR_KEY) }
                }

            _uiState.update { it.copy(isSaving = false) }
        }
    }

    private fun mapGenerateError(error: ApiError): String {
        val rawMessage = error.message.orEmpty()
        if (rawMessage.contains("not_found", ignoreCase = true)) {
            return BIRTH_CHART_GENERATE_UNAVAILABLE_KEY
        }
        if (
            rawMessage.contains("insufficient_moons", ignoreCase = true) ||
            rawMessage.contains("INSUFFICIENT_MOON_BALANCE", ignoreCase = true)
        ) {
            return "insufficient_moons"
        }

        return rawMessage.ifBlank { BIRTH_CHART_GENERATE_ERROR_FALLBACK_KEY }
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

sealed interface BirthChartUiEffect {
    data class ShowDeckCardUnlockRewards(
        val rewards: List<DeckCardUnlockReward>,
    ) : BirthChartUiEffect
}

const val BIRTH_CHART_SYNC_NO_ESSENCE_KEY = "birth_chart.sync.no_essence"
const val BIRTH_CHART_SYNC_REMOTE_LOADED_KEY = "birth_chart.sync.remote_loaded"
const val BIRTH_CHART_SYNC_UPDATED_KEY = "birth_chart.sync.updated"
const val BIRTH_CHART_SYNC_UP_TO_DATE_KEY = "birth_chart.sync.up_to_date"
const val BIRTH_CHART_REFRESH_ERROR_KEY = "birth_chart.error.refresh"
const val BIRTH_CHART_GENERATE_FIRST_ERROR_KEY = "birth_chart.error.generate_first"
const val BIRTH_CHART_SAVE_SUCCESS_SUMMARY_KEY = "birth_chart.save.success"
const val BIRTH_CHART_SAVE_ERROR_KEY = "birth_chart.error.save"
const val BIRTH_CHART_GENERATE_UNAVAILABLE_KEY = "birth_chart.error.generate_unavailable"
const val BIRTH_CHART_GENERATE_ERROR_FALLBACK_KEY = "birth_chart.error.generate_fallback"
