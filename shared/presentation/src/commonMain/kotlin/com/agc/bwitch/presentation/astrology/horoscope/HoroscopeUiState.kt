package com.agc.bwitch.presentation.astrology.horoscope

import com.agc.bwitch.domain.astrology.horoscope.DailyHoroscope
import com.agc.bwitch.domain.astrology.horoscope.ZodiacSign

data class HoroscopeUiState(
    val selectedSign: ZodiacSign = ZodiacSign.aries,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val horoscope: DailyHoroscope? = null,
)
