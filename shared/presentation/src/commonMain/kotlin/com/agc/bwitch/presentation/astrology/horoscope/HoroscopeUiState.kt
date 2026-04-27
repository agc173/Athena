package com.agc.bwitch.presentation.astrology.horoscope

import com.agc.bwitch.domain.astrology.horoscope.DailyHoroscope
import com.agc.bwitch.domain.astrology.horoscope.ZodiacSign

data class HoroscopeUiState(
    val selectedTab: HoroscopeTab = HoroscopeTab.Daily,
    val selectedSign: ZodiacSign = ZodiacSign.values().first(),
    val selectedDateIso: String = "",
    val days: List<HoroscopeDayItemUi> = emptyList(),
    val isLoading: Boolean = false,
    val isRefreshing: Boolean = false,
    val isUnlocking: Boolean = false,
    val futureDayCost: Int = 0,
    val highlightedSign: ZodiacSign? = null,
    val overlay: HoroscopeOverlayUi? = null,
    val errorMessage: HoroscopeFeedbackMessage? = null,
    val infoMessage: HoroscopeFeedbackMessage? = null,
)

enum class HoroscopeTab { Daily, Weekly, Monthly }

data class HoroscopeDayItemUi(
    val dateIso: String,
    val shortLabel: String,
    val isToday: Boolean,
    val isSelected: Boolean,
    val isLocked: Boolean,
    val isUnlocked: Boolean,
    val cost: Int,
)

data class HoroscopeOverlayUi(
    val sign: ZodiacSign,
    val dateIso: String,
    val isLocked: Boolean,
    val isLoading: Boolean,
    val horoscope: DailyHoroscope?,
    val unlockErrorMessage: HoroscopeFeedbackMessage? = null,
    val unlockErrorType: HoroscopeUnlockErrorType? = null,
)

enum class HoroscopeUnlockErrorType {
    InsufficientMoons,
    Backend,
}

enum class HoroscopeFeedbackMessage {
    AlreadyUpdated,
    Updated,
    RefreshFailed,
    ComingSoon,
    UnlockFailed,
    UnlockInsufficientMoons,
    UnlockSuccess,
}
