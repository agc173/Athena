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
    val weeklyCost: Int = 0,
    val monthlyCost: Int = 0,
    val selectedWeek: HoroscopeWeekPeriod = HoroscopeWeekPeriod.ThisWeek,
    val selectedMonth: HoroscopeMonthPeriod = HoroscopeMonthPeriod.ThisMonth,
    val selectedWeekKey: String = "",
    val selectedMonthKey: String = "",
    val currentMonthKey: String = "",
    val nextMonthKey: String = "",
    val isWeekLocked: Boolean = true,
    val isMonthLocked: Boolean = true,
    val isContentAvailable: Boolean = true,
    val isCheckingContentAvailability: Boolean = false,
    val lockCardMessage: HoroscopeFeedbackMessage? = null,
    val hasPremiumAccess: Boolean = false, // TODO wire reliable premium source in HoroscopeViewModel.
    val highlightedSign: ZodiacSign? = null,
    val overlay: HoroscopeOverlayUi? = null,
    val errorMessage: HoroscopeFeedbackMessage? = null,
    val infoMessage: HoroscopeFeedbackMessage? = null,
)

enum class HoroscopeTab { Daily, Weekly, Monthly }

enum class HoroscopeWeekPeriod { ThisWeek, NextWeek }
enum class HoroscopeMonthPeriod { ThisMonth, NextMonth }

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
    UnlockWeekFailed,
    UnlockMonthFailed,
    ContentInPreparation,
}
