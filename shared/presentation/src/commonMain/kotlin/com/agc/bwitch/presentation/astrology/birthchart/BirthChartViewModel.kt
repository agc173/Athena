package com.agc.bwitch.presentation.astrology.birthchart

import com.agc.bwitch.domain.astrology.birthchart.BirthData
import com.agc.bwitch.domain.astrology.birthchart.GetBirthDataUseCase
import com.agc.bwitch.domain.astrology.birthchart.ObserveBirthDataUseCase
import com.agc.bwitch.domain.astrology.birthchart.SaveBirthDataUseCase
import com.agc.bwitch.domain.astrology.birthchart.PullBirthChartUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalTime

class BirthChartViewModel(
    private val observeBirthData: ObserveBirthDataUseCase,
    private val getBirthData: GetBirthDataUseCase,
    private val saveBirthData: SaveBirthDataUseCase,
    private val pullBirthChart: PullBirthChartUseCase
) {
    private val scope = CoroutineScope(Dispatchers.Main)

    private val _uiState = MutableStateFlow(BirthChartUiState(isLoading = true))
    val uiState: StateFlow<BirthChartUiState> = _uiState

    init {
        // 1) Reactivo: escucha cambios locales (incluye pull remoto que pisa Settings)
        scope.launch {
            observeBirthData().collectLatest { data ->
                _uiState.update { s ->
                    s.copy(
                        isLoading = false,
                        dateText = data?.date?.toString().orEmpty(),
                        timeText = data?.time?.toString()?.take(5).orEmpty(),
                        placeText = data?.placeName.orEmpty()
                    )
                }
            }
        }

        // 2) Lectura inicial (puede disparar pull en sync repo)
        scope.launch { getBirthData() }
    }

    fun refresh() = scope.launch {
        val s = _uiState.value
        if (s.isBusy) return@launch

        _uiState.update { it.copy(isRefreshing = true, error = null, savedMessage = null) }

        runCatching { pullBirthChart() }
            .onSuccess { _uiState.update { it.copy(savedMessage = "Actualizado ✅") } }
            .onFailure { e -> _uiState.update { it.copy(error = e.message ?: "Error refrescando") } }

        _uiState.update { it.copy(isRefreshing = false) }
    }

    fun onDateChange(v: String) =
        _uiState.update { it.copy(dateText = v, error = null, savedMessage = null) }

    fun onTimeChange(v: String) =
        _uiState.update { it.copy(timeText = v, error = null, savedMessage = null) }

    fun onPlaceChange(v: String) =
        _uiState.update { it.copy(placeText = v, error = null, savedMessage = null) }

    fun onSave() {
        val s = _uiState.value
        if (s.isBusy) return

        val parsed = parse(s.dateText, s.timeText, s.placeText)
        if (parsed == null) {
            _uiState.update {
                it.copy(
                    error = "Revisa fecha (YYYY-MM-DD), hora (HH:MM) y lugar.",
                    savedMessage = null
                )
            }
            return
        }

        scope.launch {
            _uiState.update { it.copy(isSaving = true, error = null, savedMessage = null) }
            runCatching { saveBirthData(parsed) }
                .onSuccess { _uiState.update { it.copy(savedMessage = "Guardado ✅") } }
                .onFailure { e -> _uiState.update { it.copy(error = e.message ?: "Error guardando") } }
            _uiState.update { it.copy(isSaving = false) }
        }
    }

    private fun parse(date: String, time: String, place: String): BirthData? {
        if (place.isBlank()) return null
        return try {
            val d = LocalDate.parse(date.trim())
            val t = LocalTime.parse(time.trim())
            BirthData(date = d, time = t, placeName = place.trim())
        } catch (_: Throwable) {
            null
        }
    }
}

