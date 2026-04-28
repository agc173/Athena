package com.agc.bwitch.presentation.astrology.horoscope

import com.agc.bwitch.domain.analytics.AnalyticsEvent
import com.agc.bwitch.domain.analytics.AnalyticsTracker
import com.agc.bwitch.domain.analytics.NoOpAnalyticsTracker
import com.agc.bwitch.domain.astrology.horoscope.GetDailyHoroscopeUseCase
import com.agc.bwitch.domain.astrology.horoscope.GetHoroscopeFutureDayCostUseCase
import com.agc.bwitch.domain.astrology.horoscope.GetMonthlyHoroscopeUseCase
import com.agc.bwitch.domain.astrology.horoscope.GetWeeklyHoroscopeUseCase
import com.agc.bwitch.domain.astrology.horoscope.HoroscopePullMarker
import com.agc.bwitch.domain.astrology.horoscope.HoroscopeUnlockRepository
import com.agc.bwitch.domain.astrology.horoscope.IsHoroscopeDayUnlockedUseCase
import com.agc.bwitch.domain.astrology.horoscope.ObserveMonthlyHoroscopeUseCase
import com.agc.bwitch.domain.astrology.horoscope.ObserveWeeklyHoroscopeUseCase
import com.agc.bwitch.domain.astrology.horoscope.ObserveDailyHoroscopeUseCase
import com.agc.bwitch.domain.astrology.horoscope.PullDailyHoroscopeUseCase
import com.agc.bwitch.domain.astrology.horoscope.UnlockHoroscopeFutureDayUseCase
import com.agc.bwitch.domain.astrology.horoscope.ZodiacSign
import com.agc.bwitch.domain.astrology.horoscope.ZodiacSignResolver
import com.agc.bwitch.domain.localization.AppLanguage
import com.agc.bwitch.domain.localization.ObserveCurrentLanguageUseCase
import com.agc.bwitch.domain.localization.ResolveCurrentLanguageUseCase
import com.agc.bwitch.domain.userprofile.ObserveUserProfileUseCase
import com.agc.bwitch.presentation.astrology.horoscope.HoroscopeUnlockErrorType.Backend
import com.agc.bwitch.presentation.astrology.horoscope.HoroscopeUnlockErrorType.InsufficientMoons
import com.agc.bwitch.presentation.economy.MoonUnlockFlowContext
import com.agc.bwitch.presentation.economy.UNLOCK_FLOW_ORIGIN_PREMIUM
import com.agc.bwitch.presentation.economy.UNLOCK_FLOW_ORIGIN_UNKNOWN
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

class HoroscopeViewModel(
    private val observeDailyHoroscopeUseCase: ObserveDailyHoroscopeUseCase,
    private val getDailyHoroscopeUseCase: GetDailyHoroscopeUseCase,
    private val observeWeeklyHoroscopeUseCase: ObserveWeeklyHoroscopeUseCase,
    private val getWeeklyHoroscopeUseCase: GetWeeklyHoroscopeUseCase,
    private val observeMonthlyHoroscopeUseCase: ObserveMonthlyHoroscopeUseCase,
    private val getMonthlyHoroscopeUseCase: GetMonthlyHoroscopeUseCase,
    private val pullDailyHoroscopeUseCase: PullDailyHoroscopeUseCase,
    private val pullMarker: HoroscopePullMarker,
    private val resolveCurrentLanguageUseCase: ResolveCurrentLanguageUseCase,
    private val observeCurrentLanguageUseCase: ObserveCurrentLanguageUseCase,
    private val observeUserProfileUseCase: ObserveUserProfileUseCase,
    private val getHoroscopeFutureDayCostUseCase: GetHoroscopeFutureDayCostUseCase,
    private val isHoroscopeDayUnlockedUseCase: IsHoroscopeDayUnlockedUseCase,
    private val unlockHoroscopeFutureDayUseCase: UnlockHoroscopeFutureDayUseCase,
    private val unlockRepository: HoroscopeUnlockRepository,
    private val analyticsTracker: AnalyticsTracker = NoOpAnalyticsTracker,
    private val dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private data class PendingUnlockTarget(
        val dateIso: String,
        val sign: ZodiacSign,
    )

    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    private val _uiState = MutableStateFlow(HoroscopeUiState(isLoading = true))
    val uiState: StateFlow<HoroscopeUiState> = _uiState.asStateFlow()
    private val currentLanguageCode = MutableStateFlow(AppLanguage.fallback.code)

    private var observeJob: Job? = null
    private var observeWeeklyJob: Job? = null
    private var observeMonthlyJob: Job? = null
    private var userHasManuallySelectedSign = false
    private var pendingUnlockTarget: PendingUnlockTarget? = null
    private val unlockedDatesSession = mutableSetOf<String>()
    private val unlockedWeekKeysSession = mutableSetOf<String>()
    private val unlockedMonthKeysSession = mutableSetOf<String>()

    init {
        scope.launch { runCatching { resolveCurrentLanguageUseCase() } }
        scope.launch { loadCostsAndPeriods() }

        scope.launch {
            observeCurrentLanguageUseCase()
                .map { it.code }
                .distinctUntilChanged()
                .collectLatest { languageCode ->
                    currentLanguageCode.value = languageCode
                    reloadForCurrentSelection()
                    pullTodayIfNeeded(languageCode)
                }
        }

        scope.launch {
            observeUserProfileUseCase()
                .map { profile -> profile?.zodiacSign ?: profile?.birthDate?.let(ZodiacSignResolver::fromBirthDate) }
                .collectLatest { profileSign ->
                    _uiState.update { it.copy(highlightedSign = profileSign) }
                    if (profileSign == null || userHasManuallySelectedSign || _uiState.value.selectedSign == profileSign) return@collectLatest
                    onSelectSign(profileSign, fromUserInteraction = false)
                }
        }

        reloadForCurrentSelection()
    }

    fun onSelectSign(sign: ZodiacSign) = onSelectSign(sign, fromUserInteraction = true)

    fun onSelectDate(dateIso: String) {
        pendingUnlockTarget = null
        _uiState.update { it.copy(selectedDateIso = dateIso, errorMessage = null, overlay = null) }
        rebuildDays()
        observeSelectedHoroscope()
    }

    fun onSelectWeek(period: HoroscopeWeekPeriod) {
        _uiState.update {
            it.copy(
                selectedWeek = period,
                selectedWeekKey = when (period) {
                    HoroscopeWeekPeriod.ThisWeek -> currentWeekKey()
                    HoroscopeWeekPeriod.NextWeek -> nextWeekKey()
                },
                lockCardMessage = null,
            )
        }
        refreshWeeklyMonthlyLocks()
        refreshSelectedPeriodContentAvailability()
    }

    fun onSelectMonth(period: HoroscopeMonthPeriod) {
        _uiState.update {
            it.copy(
                selectedMonth = period,
                selectedMonthKey = when (period) {
                    HoroscopeMonthPeriod.ThisMonth -> currentMonthKey()
                    HoroscopeMonthPeriod.NextMonth -> nextMonthKey()
                },
                lockCardMessage = null,
            )
        }
        refreshWeeklyMonthlyLocks()
        refreshSelectedPeriodContentAvailability()
    }

    fun onRefresh() {
        scope.launch {
            val selectedDateIso = _uiState.value.selectedDateIso.ifBlank { todayIso() }
            safePull(selectedDateIso, currentLanguageCode.value)
            if (_uiState.value.errorMessage == null) {
                _uiState.update { it.copy(infoMessage = HoroscopeFeedbackMessage.Updated) }
            }
        }
    }

    fun onOpenSign(sign: ZodiacSign) {
        when (_uiState.value.selectedTab) {
            HoroscopeTab.Daily -> openDailySign(sign)
            HoroscopeTab.Weekly -> openWeeklySign(sign)
            HoroscopeTab.Monthly -> openMonthlySign(sign)
        }
    }

    fun onCloseOverlay() {
        pendingUnlockTarget = null
        _uiState.update { it.copy(overlay = null) }
    }

    fun onUnlockDeferredToPaywall() {
        val state = _uiState.value
        val overlay = state.overlay
        if (overlay is HoroscopeOverlayUi.DailyOverlay && overlay.isLocked) {
            pendingUnlockTarget = PendingUnlockTarget(
                dateIso = state.selectedDateIso.ifBlank { todayIso() },
                sign = overlay.sign,
            )
        }
        _uiState.update { current ->
            current.copy(
                lockCardMessage = null,
                overlay = (current.overlay as? HoroscopeOverlayUi.DailyOverlay)?.copy(
                    unlockErrorMessage = null,
                    unlockErrorType = null,
                ),
            )
        }
        _uiState.update { it.copy(overlay = null) }
    }

    fun onUnlockWeekDeferredToPaywall() {
        _uiState.update { it.copy(lockCardMessage = null) }
    }

    fun onUnlockMonthDeferredToPaywall() {
        _uiState.update { it.copy(lockCardMessage = null) }
    }

    fun onUnlockSelectedDay(unlockFlowContext: MoonUnlockFlowContext? = null) {
        val state = _uiState.value
        val overlay = state.overlay as? HoroscopeOverlayUi.DailyOverlay
        val target = when {
            overlay != null && overlay.isLocked -> PendingUnlockTarget(
                dateIso = state.selectedDateIso.ifBlank { todayIso() },
                sign = overlay.sign,
            )
            else -> pendingUnlockTarget
        } ?: return
        pendingUnlockTarget = target

        scope.launch {
            analyticsTracker.track(
                AnalyticsEvent.ContentUnlockAttempt(
                    module = "horoscope_daily",
                    cost = _uiState.value.futureDayCost,
                    hasEnoughMoons = null,
                    isPremium = _uiState.value.hasPremiumAccess,
                    unlockFlowOrigin = resolveUnlockFlowOrigin(unlockFlowContext),
                    paywallImpressionId = unlockFlowContext?.paywallImpressionId,
                ),
            )
            _uiState.update {
                it.copy(
                    isUnlocking = true,
                    overlay = (it.overlay as? HoroscopeOverlayUi.DailyOverlay)?.copy(
                        unlockErrorMessage = null,
                        unlockErrorType = null,
                    ),
                )
            }
            if (unlockedDatesSession.contains(target.dateIso)) {
                pendingUnlockTarget = null
                _uiState.update { current ->
                    val updatedDays = current.days.map { day ->
                        if (day.dateIso == target.dateIso) day.copy(isLocked = false, isUnlocked = true) else day
                    }
                    current.copy(
                        days = updatedDays,
                        overlay = (current.overlay as? HoroscopeOverlayUi.DailyOverlay)?.copy(
                            isLocked = false,
                            unlockErrorMessage = null,
                            unlockErrorType = null,
                        ),
                    )
                }
                loadOverlayHoroscope(sign = target.sign, dateIso = target.dateIso)
                rebuildDays()
                _uiState.update { it.copy(isUnlocking = false) }
                return@launch
            }

            runCatching {
                unlockHoroscopeFutureDayUseCase(
                    dateIso = target.dateIso,
                    requestId = buildDailyRequestId(target.dateIso),
                    sign = target.sign,
                )
            }.onSuccess { unlockResult ->
                unlockedDatesSession += target.dateIso
                pendingUnlockTarget = null
                analyticsTracker.track(
                    AnalyticsEvent.ContentUnlocked(
                        module = "horoscope_daily",
                        method = resolveUnlockMethod(unlockResult, _uiState.value.hasPremiumAccess),
                        costCharged = unlockResult.costCharged,
                        balanceAfter = unlockResult.balanceAfter,
                        unlockFlowOrigin = resolveUnlockFlowOrigin(unlockFlowContext),
                        paywallImpressionId = unlockFlowContext?.paywallImpressionId,
                    ),
                )
                _uiState.update { current ->
                    val updatedDays = current.days.map { day ->
                        if (day.dateIso == target.dateIso) day.copy(isLocked = false, isUnlocked = true) else day
                    }
                    val updatedOverlay = (current.overlay as? HoroscopeOverlayUi.DailyOverlay)?.takeIf {
                        it.dateIso == target.dateIso && it.sign == target.sign
                    }?.copy(
                        isLocked = false,
                        isLoading = true,
                        horoscope = null,
                        unlockErrorMessage = null,
                        unlockErrorType = null,
                    )
                    current.copy(
                        infoMessage = HoroscopeFeedbackMessage.UnlockSuccess,
                        days = updatedDays,
                        overlay = updatedOverlay ?: current.overlay,
                    )
                }
                if (overlay != null && overlay.dateIso == target.dateIso && overlay.sign == target.sign) {
                    loadOverlayHoroscope(sign = target.sign, dateIso = target.dateIso)
                }
                rebuildDays()
            }.onFailure { error ->
                val isInsufficient = error.isInsufficientMoons()
                analyticsTracker.track(
                    AnalyticsEvent.ContentUnlockFailed(
                        module = "horoscope_daily",
                        reason = if (isInsufficient) "insufficient_moons" else (error.message ?: "backend_error"),
                        unlockFlowOrigin = resolveUnlockFlowOrigin(unlockFlowContext),
                        paywallImpressionId = unlockFlowContext?.paywallImpressionId,
                    ),
                )
                _uiState.update {
                    val unlockErrorMessage = if (isInsufficient) {
                        HoroscopeFeedbackMessage.UnlockInsufficientMoons
                    } else {
                        HoroscopeFeedbackMessage.UnlockFailed
                    }
                    val overlayMatchesTarget = (it.overlay as? HoroscopeOverlayUi.DailyOverlay)?.let { currentOverlay ->
                        currentOverlay.dateIso == target.dateIso && currentOverlay.sign == target.sign
                    } == true
                    if (overlayMatchesTarget) {
                        it.copy(
                            overlay = (it.overlay as? HoroscopeOverlayUi.DailyOverlay)?.copy(
                                unlockErrorMessage = unlockErrorMessage,
                                unlockErrorType = if (isInsufficient) InsufficientMoons else Backend,
                            ),
                        )
                    } else {
                        it.copy(errorMessage = unlockErrorMessage)
                    }
                }
            }
            _uiState.update { it.copy(isUnlocking = false) }
        }
    }

    fun onUnlockSelectedWeek(unlockFlowContext: MoonUnlockFlowContext? = null) {
        val state = _uiState.value
        val weekKey = state.selectedWeekKey
        if (weekKey.isBlank()) return

        scope.launch {
            analyticsTracker.track(
                AnalyticsEvent.ContentUnlockAttempt(
                    module = "horoscope_weekly",
                    cost = _uiState.value.weeklyCost,
                    hasEnoughMoons = null,
                    isPremium = _uiState.value.hasPremiumAccess,
                    unlockFlowOrigin = resolveUnlockFlowOrigin(unlockFlowContext),
                    paywallImpressionId = unlockFlowContext?.paywallImpressionId,
                ),
            )
            _uiState.update { it.copy(isUnlocking = true, lockCardMessage = null) }
            runCatching {
                unlockRepository.unlockWeek(
                    weekKey = weekKey,
                    requestId = buildWeeklyRequestId(weekKey),
                    sign = state.selectedSign,
                )
            }.onSuccess { unlockResult ->
                unlockedWeekKeysSession += weekKey
                analyticsTracker.track(
                    AnalyticsEvent.ContentUnlocked(
                        module = "horoscope_weekly",
                        method = resolveUnlockMethod(unlockResult, _uiState.value.hasPremiumAccess),
                        costCharged = unlockResult.costCharged,
                        balanceAfter = unlockResult.balanceAfter,
                        unlockFlowOrigin = resolveUnlockFlowOrigin(unlockFlowContext),
                        paywallImpressionId = unlockFlowContext?.paywallImpressionId,
                    ),
                )
                _uiState.update {
                    it.copy(
                        isUnlocking = false,
                        isWeekLocked = false,
                        infoMessage = HoroscopeFeedbackMessage.UnlockSuccess,
                        lockCardMessage = null,
                    )
                }
            }.onFailure { error ->
                analyticsTracker.track(
                    AnalyticsEvent.ContentUnlockFailed(
                        module = "horoscope_weekly",
                        reason = if (error.isInsufficientMoons()) "insufficient_moons" else (error.message ?: "backend_error"),
                        unlockFlowOrigin = resolveUnlockFlowOrigin(unlockFlowContext),
                        paywallImpressionId = unlockFlowContext?.paywallImpressionId,
                    ),
                )
                _uiState.update {
                    it.copy(
                        isUnlocking = false,
                        lockCardMessage = if (error.isInsufficientMoons()) {
                            HoroscopeFeedbackMessage.UnlockInsufficientMoons
                        } else {
                            HoroscopeFeedbackMessage.UnlockWeekFailed
                        },
                    )
                }
            }
        }
    }

    fun onUnlockSelectedMonth(unlockFlowContext: MoonUnlockFlowContext? = null) {
        val state = _uiState.value
        val monthKey = state.selectedMonthKey
        if (monthKey.isBlank()) return

        scope.launch {
            analyticsTracker.track(
                AnalyticsEvent.ContentUnlockAttempt(
                    module = "horoscope_monthly",
                    cost = _uiState.value.monthlyCost,
                    hasEnoughMoons = null,
                    isPremium = _uiState.value.hasPremiumAccess,
                    unlockFlowOrigin = resolveUnlockFlowOrigin(unlockFlowContext),
                    paywallImpressionId = unlockFlowContext?.paywallImpressionId,
                ),
            )
            _uiState.update { it.copy(isUnlocking = true, lockCardMessage = null) }
            runCatching {
                unlockRepository.unlockMonth(
                    monthKey = monthKey,
                    requestId = buildMonthlyRequestId(monthKey),
                    sign = state.selectedSign,
                )
            }.onSuccess { unlockResult ->
                unlockedMonthKeysSession += monthKey
                analyticsTracker.track(
                    AnalyticsEvent.ContentUnlocked(
                        module = "horoscope_monthly",
                        method = resolveUnlockMethod(unlockResult, _uiState.value.hasPremiumAccess),
                        costCharged = unlockResult.costCharged,
                        balanceAfter = unlockResult.balanceAfter,
                        unlockFlowOrigin = resolveUnlockFlowOrigin(unlockFlowContext),
                        paywallImpressionId = unlockFlowContext?.paywallImpressionId,
                    ),
                )
                _uiState.update {
                    it.copy(
                        isUnlocking = false,
                        isMonthLocked = false,
                        infoMessage = HoroscopeFeedbackMessage.UnlockSuccess,
                        lockCardMessage = null,
                    )
                }
            }.onFailure { error ->
                analyticsTracker.track(
                    AnalyticsEvent.ContentUnlockFailed(
                        module = "horoscope_monthly",
                        reason = if (error.isInsufficientMoons()) "insufficient_moons" else (error.message ?: "backend_error"),
                        unlockFlowOrigin = resolveUnlockFlowOrigin(unlockFlowContext),
                        paywallImpressionId = unlockFlowContext?.paywallImpressionId,
                    ),
                )
                _uiState.update {
                    it.copy(
                        isUnlocking = false,
                        lockCardMessage = if (error.isInsufficientMoons()) {
                            HoroscopeFeedbackMessage.UnlockInsufficientMoons
                        } else {
                            HoroscopeFeedbackMessage.UnlockMonthFailed
                        },
                    )
                }
            }
        }
    }

    fun onInfoShown() = _uiState.update { it.copy(infoMessage = null) }
    fun onErrorShown() = _uiState.update { it.copy(errorMessage = null) }

    fun onSelectTab(tab: HoroscopeTab) {
        _uiState.update { it.copy(selectedTab = tab, lockCardMessage = null) }
        if (tab != HoroscopeTab.Daily) refreshWeeklyMonthlyLocks()
        refreshSelectedPeriodContentAvailability()
    }

    fun onPremiumAccessChanged(hasPremiumAccess: Boolean) {
        _uiState.update { it.copy(hasPremiumAccess = hasPremiumAccess) }
        refreshWeeklyMonthlyLocks()
    }

    private fun onSelectSign(sign: ZodiacSign, fromUserInteraction: Boolean) {
        if (fromUserInteraction) userHasManuallySelectedSign = true
        pendingUnlockTarget = null
        _uiState.update { it.copy(selectedSign = sign) }
        rebuildDays()
        observeSelectedHoroscope()
        refreshSelectedPeriodContentAvailability()
    }

    private fun resolveUnlockFlowOrigin(context: MoonUnlockFlowContext?): String {
        return context?.unlockFlowOrigin ?: if (_uiState.value.hasPremiumAccess) {
            UNLOCK_FLOW_ORIGIN_PREMIUM
        } else {
            UNLOCK_FLOW_ORIGIN_UNKNOWN
        }
    }

    private fun reloadForCurrentSelection() {
        val today = todayDate()
        _uiState.update {
            it.copy(
                selectedDateIso = it.selectedDateIso.ifBlank { today.toString() },
                isLoading = true,
                overlay = null,
                selectedWeekKey = when (it.selectedWeek) {
                    HoroscopeWeekPeriod.ThisWeek -> currentWeekKey()
                    HoroscopeWeekPeriod.NextWeek -> nextWeekKey()
                },
                selectedMonthKey = when (it.selectedMonth) {
                    HoroscopeMonthPeriod.ThisMonth -> currentMonthKey()
                    HoroscopeMonthPeriod.NextMonth -> nextMonthKey()
                },
                currentMonthKey = currentMonthKey(),
                nextMonthKey = nextMonthKey(),
            )
        }
        rebuildDays()
        refreshWeeklyMonthlyLocks()
        observeSelectedHoroscope()
        refreshSelectedPeriodContentAvailability()
    }

    private suspend fun loadCostsAndPeriods() {
        val dailyCost = runCatching { getHoroscopeFutureDayCostUseCase() }.getOrDefault(1)
        val weeklyCost = runCatching { unlockRepository.getWeeklyCost() }.getOrDefault(2)
        val monthlyCost = runCatching { unlockRepository.getMonthlyCost() }.getOrDefault(3)

        _uiState.update {
            it.copy(
                futureDayCost = dailyCost,
                weeklyCost = weeklyCost,
                monthlyCost = monthlyCost,
                selectedWeekKey = when (it.selectedWeek) {
                    HoroscopeWeekPeriod.ThisWeek -> currentWeekKey()
                    HoroscopeWeekPeriod.NextWeek -> nextWeekKey()
                },
                selectedMonthKey = when (it.selectedMonth) {
                    HoroscopeMonthPeriod.ThisMonth -> currentMonthKey()
                    HoroscopeMonthPeriod.NextMonth -> nextMonthKey()
                },
                currentMonthKey = currentMonthKey(),
                nextMonthKey = nextMonthKey(),
            )
        }
        refreshWeeklyMonthlyLocks()
        refreshSelectedPeriodContentAvailability()
    }

    private fun refreshWeeklyMonthlyLocks() {
        scope.launch {
            val state = _uiState.value
            val currentWeek = currentWeekKey()
            val nextWeek = nextWeekKey()
            val currentMonth = currentMonthKey()
            val nextMonth = nextMonthKey()

            val remoteWeekUnlocked = runCatching {
                unlockRepository.getUnlockedWeeks(listOf(currentWeek, nextWeek))
            }.getOrDefault(emptySet())

            val remoteMonthUnlocked = runCatching {
                unlockRepository.getUnlockedMonths(listOf(currentMonth, nextMonth))
            }.getOrDefault(emptySet())

            unlockedWeekKeysSession += remoteWeekUnlocked
            unlockedMonthKeysSession += remoteMonthUnlocked

            val selectedWeekKey = state.selectedWeekKey.ifBlank {
                if (state.selectedWeek == HoroscopeWeekPeriod.ThisWeek) currentWeek else nextWeek
            }
            val selectedMonthKey = state.selectedMonthKey.ifBlank {
                if (state.selectedMonth == HoroscopeMonthPeriod.ThisMonth) currentMonth else nextMonth
            }

            val isWeekUnlocked = unlockedWeekKeysSession.contains(selectedWeekKey) || remoteWeekUnlocked.contains(selectedWeekKey)
            val isMonthUnlocked = unlockedMonthKeysSession.contains(selectedMonthKey) || remoteMonthUnlocked.contains(selectedMonthKey)

            _uiState.update {
                it.copy(
                    selectedWeekKey = selectedWeekKey,
                    selectedMonthKey = selectedMonthKey,
                    currentMonthKey = currentMonth,
                    nextMonthKey = nextMonth,
                    isWeekLocked = !it.hasPremiumAccess && !isWeekUnlocked,
                    isMonthLocked = !it.hasPremiumAccess && !isMonthUnlocked,
                )
            }
        }
    }

    private fun refreshSelectedPeriodContentAvailability() {
        scope.launch {
            val state = _uiState.value
            if (state.selectedTab == HoroscopeTab.Daily) {
                _uiState.update { it.copy(isContentAvailable = true, isCheckingContentAvailability = false) }
                return@launch
            }
            // En Weekly/Monthly mantenemos política conservadora: no bloquear unlock por heurísticas parciales
            // de disponibilidad (p.ej. signo/idioma concreto aún no backfilleado).
            _uiState.update { it.copy(isContentAvailable = true, isCheckingContentAvailability = false) }
        }
    }

    private fun observeSelectedHoroscope() {
        val state = _uiState.value
        val dateIso = state.selectedDateIso.ifBlank { todayIso() }
        val sign = state.selectedSign
        val languageCode = currentLanguageCode.value

        observeJob?.cancel()
        observeWeeklyJob?.cancel()
        observeMonthlyJob?.cancel()
        observeJob = scope.launch {
            observeDailyHoroscopeUseCase(dateIso, sign, languageCode).collectLatest { cached ->
                _uiState.update { current ->
                    val overlay = current.overlay
                    val updatedOverlay = if (overlay is HoroscopeOverlayUi.DailyOverlay &&
                        overlay.sign == sign &&
                        overlay.dateIso == dateIso &&
                        !overlay.isLocked
                    ) {
                        overlay.copy(horoscope = cached, isLoading = false)
                    } else {
                        overlay
                    }
                    current.copy(
                        isLoading = false,
                        overlay = updatedOverlay,
                    )
                }
            }
        }

        scope.launch {
            getDailyHoroscopeUseCase(dateIso, sign, languageCode)
            if (dateIso == todayIso()) pullTodayIfNeeded(languageCode)
        }
    }

    private fun loadOverlayHoroscope(sign: ZodiacSign, dateIso: String) {
        scope.launch {
            val languageCode = currentLanguageCode.value
            var loaded = runCatching { getDailyHoroscopeUseCase(dateIso, sign, languageCode) }.getOrNull()
            val shouldPullFutureContent = loaded == null && isDateUnlocked(dateIso)
            if (shouldPullFutureContent) {
                safePull(dateIso = dateIso, languageCode = languageCode, showGlobalError = false)
                loaded = runCatching { getDailyHoroscopeUseCase(dateIso, sign, languageCode) }.getOrNull()
            }
            _uiState.update { current ->
                val overlay = current.overlay ?: return@update current
                if (overlay !is HoroscopeOverlayUi.DailyOverlay) return@update current
                if (overlay.sign != sign || overlay.dateIso != dateIso) return@update current
                current.copy(overlay = overlay.copy(isLoading = false, horoscope = loaded))
            }
        }
    }

    private fun openDailySign(sign: ZodiacSign) {
        onSelectSign(sign)
        val state = _uiState.value
        val selectedDateIso = state.selectedDateIso.ifBlank { todayIso() }
        val isLocked = state.days.firstOrNull { it.dateIso == selectedDateIso }?.isLocked == true

        _uiState.update {
            it.copy(
                overlay = HoroscopeOverlayUi.DailyOverlay(
                    sign = sign,
                    dateIso = selectedDateIso,
                    isLocked = isLocked,
                    isLoading = !isLocked,
                    horoscope = null,
                    unlockErrorMessage = null,
                    unlockErrorType = null,
                ),
            )
        }
        pendingUnlockTarget = if (isLocked) PendingUnlockTarget(selectedDateIso, sign) else null
        if (isLocked) return
        loadOverlayHoroscope(sign = sign, dateIso = selectedDateIso)
    }

    private fun openWeeklySign(sign: ZodiacSign) {
        val state = _uiState.value
        if (state.isWeekLocked) return
        val weekKey = state.selectedWeekKey.ifBlank { currentWeekKey() }
        onSelectSign(sign)
        _uiState.update { it.copy(overlay = HoroscopeOverlayUi.WeeklyOverlay(sign = sign, weekKey = weekKey, isLoading = true, horoscope = null)) }
        observeWeeklyOverlay(sign = sign, weekKey = weekKey)
    }

    private fun openMonthlySign(sign: ZodiacSign) {
        val state = _uiState.value
        if (state.isMonthLocked) return
        val monthKey = state.selectedMonthKey.ifBlank { currentMonthKey() }
        onSelectSign(sign)
        _uiState.update { it.copy(overlay = HoroscopeOverlayUi.MonthlyOverlay(sign = sign, monthKey = monthKey, isLoading = true, horoscope = null)) }
        observeMonthlyOverlay(sign = sign, monthKey = monthKey)
    }

    private fun observeWeeklyOverlay(sign: ZodiacSign, weekKey: String) {
        observeWeeklyJob?.cancel()
        observeWeeklyJob = scope.launch {
            observeWeeklyHoroscopeUseCase(weekKey, sign, currentLanguageCode.value)
                .catch {
                    _uiState.update { current ->
                        val overlay = current.overlay as? HoroscopeOverlayUi.WeeklyOverlay ?: return@update current
                        if (overlay.sign != sign || overlay.weekKey != weekKey) return@update current
                        current.copy(overlay = overlay.copy(isLoading = false, horoscope = null))
                    }
                }
                .collectLatest { cached ->
                    _uiState.update { current ->
                        val overlay = current.overlay as? HoroscopeOverlayUi.WeeklyOverlay ?: return@update current
                        if (overlay.sign != sign || overlay.weekKey != weekKey) return@update current
                        current.copy(overlay = overlay.copy(isLoading = false, horoscope = cached))
                    }
                }
        }
    }

    private fun observeMonthlyOverlay(sign: ZodiacSign, monthKey: String) {
        observeMonthlyJob?.cancel()
        observeMonthlyJob = scope.launch {
            observeMonthlyHoroscopeUseCase(monthKey, sign, currentLanguageCode.value)
                .catch {
                    _uiState.update { current ->
                        val overlay = current.overlay as? HoroscopeOverlayUi.MonthlyOverlay ?: return@update current
                        if (overlay.sign != sign || overlay.monthKey != monthKey) return@update current
                        current.copy(overlay = overlay.copy(isLoading = false, horoscope = null))
                    }
                }
                .collectLatest { cached ->
                    _uiState.update { current ->
                        val overlay = current.overlay as? HoroscopeOverlayUi.MonthlyOverlay ?: return@update current
                        if (overlay.sign != sign || overlay.monthKey != monthKey) return@update current
                        current.copy(overlay = overlay.copy(isLoading = false, horoscope = cached))
                    }
                }
        }
    }

    private fun rebuildDays() {
        scope.launch {
            val state = _uiState.value
            val selectedIso = state.selectedDateIso.ifBlank { todayIso() }
            val today = todayDate()
            val cost = state.futureDayCost
            val dateIsoList = (0..6).map { offset ->
                today.plus(DatePeriod(days = offset)).toString()
            }
            val remoteUnlockedDates = runCatching {
                isHoroscopeDayUnlockedUseCase.getUnlockedDays(dateIsoList)
            }.getOrDefault(emptySet())

            val items = (0..6).map { offset ->
                val date = today.plus(DatePeriod(days = offset))
                val dateIso = date.toString()
                val unlocked = offset == 0 || unlockedDatesSession.contains(dateIso) || remoteUnlockedDates.contains(dateIso)
                HoroscopeDayItemUi(
                    dateIso = dateIso,
                    shortLabel = shortDateLabel(date),
                    isToday = offset == 0,
                    isSelected = dateIso == selectedIso,
                    isLocked = offset > 0 && !unlocked,
                    isUnlocked = unlocked,
                    cost = if (offset > 0) cost else 0,
                )
            }

            _uiState.update {
                it.copy(
                    days = items,
                    selectedDateIso = items.firstOrNull { day -> day.isSelected }?.dateIso ?: today.toString(),
                )
            }
        }
    }

    private suspend fun pullTodayIfNeeded(languageCode: String) {
        val today = todayIso()
        val lastPulled = pullMarker.getLastPulledDateIso(languageCode = languageCode)
        if (lastPulled == today) return
        safePull(today, languageCode)
        if (_uiState.value.errorMessage == null) {
            pullMarker.setLastPulledDateIso(today, languageCode = languageCode)
        }
    }

    private suspend fun safePull(dateIso: String, languageCode: String, showGlobalError: Boolean = true) {
        _uiState.update { it.copy(isRefreshing = true, errorMessage = if (showGlobalError) null else it.errorMessage) }
        try {
            pullDailyHoroscopeUseCase(dateIso, languageCode)
        } catch (error: Throwable) {
            if (showGlobalError) {
                _uiState.update { it.copy(errorMessage = HoroscopeFeedbackMessage.RefreshFailed) }
            }
        } finally {
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    private fun isDateUnlocked(dateIso: String): Boolean {
        val state = _uiState.value
        return unlockedDatesSession.contains(dateIso) || state.days.firstOrNull { it.dateIso == dateIso }?.isUnlocked == true
    }

    private fun resolveUnlockMethod(
        result: com.agc.bwitch.domain.astrology.horoscope.HoroscopeUnlockResult,
        hasPremiumAccess: Boolean,
    ): String = when {
        result.alreadyUnlocked -> "already_unlocked"
        result.costCharged > 0 -> "moons"
        result.costCharged == 0 && hasPremiumAccess -> "premium"
        else -> "unknown"
    }

    private fun todayDate(): LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    private fun madridTodayDate(): LocalDate = Clock.System.now().toLocalDateTime(TimeZone.of("Europe/Madrid")).date
    private fun todayIso(): String = todayDate().toString()
    private fun shortDateLabel(date: LocalDate): String = "${date.dayOfMonth}/${date.monthNumber}"

    private fun currentMonthKey(): String {
        val todayMadrid = madridTodayDate()
        return monthKey(todayMadrid.year, todayMadrid.monthNumber)
    }

    private fun nextMonthKey(): String {
        val todayMadrid = madridTodayDate().plus(DatePeriod(months = 1))
        return monthKey(todayMadrid.year, todayMadrid.monthNumber)
    }

    // ISO week helper kept local for commonMain compatibility.
    private fun currentWeekKey(): String = toIsoWeekKey(madridTodayDate())

    private fun nextWeekKey(): String = toIsoWeekKey(madridTodayDate().plus(DatePeriod(days = 7)))

    private fun toIsoWeekKey(date: LocalDate): String {
        val dayOfWeek = isoDayNumber(date)
        val dayOfYear = dayOfYear(date)
        val rawWeek = dayOfYear - dayOfWeek + 10
        var week: Int = rawWeek / 7
        var weekYear = date.year
        if (week < 1) {
            weekYear -= 1
            week = weeksInYear(weekYear)
        } else {
            val maxWeeks = weeksInYear(weekYear)
            if (week > maxWeeks) {
                weekYear += 1
                week = 1
            }
        }
        return weekKey(weekYear, week)
    }

    private fun weeksInYear(year: Int): Int {
        val jan1 = isoDayNumber(LocalDate(year, 1, 1))
        val dec31 = isoDayNumber(LocalDate(year, 12, 31))
        return if (jan1 == 4 || dec31 == 4 || (jan1 == 3 && isLeapYear(year))) 53 else 52
    }

    private fun twoDigits(value: Int): String = if (value in 0..9) "0$value" else value.toString()

    private fun fourDigits(value: Int): String = when {
        value in 0..9 -> "000$value"
        value in 10..99 -> "00$value"
        value in 100..999 -> "0$value"
        else -> value.toString()
    }

    private fun monthKey(year: Int, month: Int): String = "${fourDigits(year)}-${twoDigits(month)}"

    private fun weekKey(year: Int, week: Int): String = "${fourDigits(year)}-W${twoDigits(week)}"

    private fun isoDayNumber(date: LocalDate): Int {
        return when (date.dayOfWeek.name) {
            "MONDAY" -> 1
            "TUESDAY" -> 2
            "WEDNESDAY" -> 3
            "THURSDAY" -> 4
            "FRIDAY" -> 5
            "SATURDAY" -> 6
            "SUNDAY" -> 7
            else -> 1
        }
    }

    private fun isLeapYear(year: Int): Boolean {
        return (year % 4 == 0 && year % 100 != 0) || year % 400 == 0
    }

    private fun dayOfYear(date: LocalDate): Int {
        val monthLengths = intArrayOf(
            31, if (isLeapYear(date.year)) 29 else 28, 31, 30, 31, 30,
            31, 31, 30, 31, 30, 31,
        )
        var sum = 0
        for (index in 0 until (date.monthNumber - 1)) {
            sum += monthLengths[index]
        }
        return sum + date.dayOfMonth
    }

    private fun buildDailyRequestId(dateIso: String): String {
        return "horoscope-daily-unlock-${Clock.System.now().toEpochMilliseconds()}-$dateIso"
    }

    private fun buildWeeklyRequestId(weekKey: String): String {
        return "horoscope-weekly-unlock-${Clock.System.now().toEpochMilliseconds()}-$weekKey"
    }

    private fun buildMonthlyRequestId(monthKey: String): String {
        return "horoscope-monthly-unlock-${Clock.System.now().toEpochMilliseconds()}-$monthKey"
    }

}

private fun Throwable.isInsufficientMoons(): Boolean {
    val message = message.orEmpty().lowercase()
    return message.contains("insufficient") || message.contains("moon")
}
