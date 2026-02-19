package com.agc.bwitch.presentation.astrology.birthchart

import com.agc.bwitch.domain.astrology.birthchart.BirthData
import com.agc.bwitch.domain.astrology.birthchart.GetBirthDataUseCase
import com.agc.bwitch.domain.astrology.birthchart.ObserveBirthDataUseCase
import com.agc.bwitch.domain.astrology.birthchart.SaveBirthDataUseCase
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
    private val saveBirthData: SaveBirthDataUseCase
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

        // 2) Forzar lectura inicial (y dispara sync pull en SyncBirthChartRepository)
        scope.launch { getBirthData() }
    }

    fun onDateChange(v: String) =
        _uiState.update { it.copy(dateText = v, error = null, savedMessage = null) }

    fun onTimeChange(v: String) =
        _uiState.update { it.copy(timeText = v, error = null, savedMessage = null) }

    fun onPlaceChange(v: String) =
        _uiState.update { it.copy(placeText = v, error = null, savedMessage = null) }

    fun onSave() {
        val s = _uiState.value
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
            _uiState.update { it.copy(isLoading = true, error = null, savedMessage = null) }
            saveBirthData(parsed)
            _uiState.update { it.copy(isLoading = false, savedMessage = "Guardado ✅") }
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

