package com.agc.bwitch.presentation.astrology.horoscope

import com.agc.bwitch.domain.astrology.horoscope.DailyHoroscope
import com.agc.bwitch.domain.astrology.horoscope.ZodiacSign

data class HoroscopeUiState(
    val selectedSign: ZodiacSign = ZodiacSign.entries.first(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val horoscope: DailyHoroscope? = null,
    val errorMessage: String? = null,
)
