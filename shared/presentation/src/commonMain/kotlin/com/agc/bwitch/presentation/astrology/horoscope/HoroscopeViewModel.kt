package com.agc.bwitch.presentation.astrology.horoscope

import com.agc.bwitch.domain.astrology.horoscope.GetDailyHoroscopeUseCase
import com.agc.bwitch.domain.astrology.horoscope.GetHoroscopeFutureDayCostUseCase
import com.agc.bwitch.domain.astrology.horoscope.HoroscopePullMarker
import com.agc.bwitch.domain.astrology.horoscope.IsHoroscopeDayUnlockedUseCase
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
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
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
    private val pullDailyHoroscopeUseCase: PullDailyHoroscopeUseCase,
    private val pullMarker: HoroscopePullMarker,
    private val resolveCurrentLanguageUseCase: ResolveCurrentLanguageUseCase,
    private val observeCurrentLanguageUseCase: ObserveCurrentLanguageUseCase,
    private val observeUserProfileUseCase: ObserveUserProfileUseCase,
    private val getHoroscopeFutureDayCostUseCase: GetHoroscopeFutureDayCostUseCase,
    private val isHoroscopeDayUnlockedUseCase: IsHoroscopeDayUnlockedUseCase,
    private val unlockHoroscopeFutureDayUseCase: UnlockHoroscopeFutureDayUseCase,
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
    private var userHasManuallySelectedSign = false
    private var pendingUnlockTarget: PendingUnlockTarget? = null
    private val unlockedDatesSession = mutableSetOf<String>()

    init {
        scope.launch { runCatching { resolveCurrentLanguageUseCase() } }
        scope.launch {
            val cost = runCatching { getHoroscopeFutureDayCostUseCase() }.getOrDefault(1)
            _uiState.update { it.copy(futureDayCost = cost) }
        }

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
        onSelectSign(sign)
        val state = _uiState.value
        val selectedDateIso = state.selectedDateIso.ifBlank { todayIso() }
        val isLocked = state.days.firstOrNull { it.dateIso == selectedDateIso }?.isLocked == true

        _uiState.update {
            it.copy(
                overlay = HoroscopeOverlayUi(
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

    fun onCloseOverlay() {
        pendingUnlockTarget = null
        _uiState.update { it.copy(overlay = null) }
    }

    fun onUnlockDeferredToPaywall() {
        val state = _uiState.value
        val overlay = state.overlay
        if (overlay != null && overlay.isLocked) {
            pendingUnlockTarget = PendingUnlockTarget(
                dateIso = state.selectedDateIso.ifBlank { todayIso() },
                sign = overlay.sign,
            )
        }
        _uiState.update { current ->
            current.copy(
                overlay = current.overlay?.copy(
                    unlockErrorMessage = null,
                    unlockErrorType = null,
                ),
            )
        }
        _uiState.update { it.copy(overlay = null) }
    }

    fun onUnlockSelectedDay() {
        val state = _uiState.value
        val overlay = state.overlay
        val target = when {
            overlay != null && overlay.isLocked -> PendingUnlockTarget(
                dateIso = state.selectedDateIso.ifBlank { todayIso() },
                sign = overlay.sign,
            )
            else -> pendingUnlockTarget
        } ?: return
        pendingUnlockTarget = target

        scope.launch {
            _uiState.update {
                it.copy(
                    isUnlocking = true,
                    overlay = it.overlay?.copy(
                        unlockErrorMessage = null,
                        unlockErrorType = null,
                    ),
                )
            }
            if (unlockedDatesSession.contains(target.dateIso)) {
                println("[HoroscopeViewModel] skip unlock request; already unlocked in session dateIso=${target.dateIso}")
                pendingUnlockTarget = null
                _uiState.update { current ->
                    val updatedDays = current.days.map { day ->
                        if (day.dateIso == target.dateIso) day.copy(isLocked = false, isUnlocked = true) else day
                    }
                    current.copy(
                        days = updatedDays,
                        overlay = current.overlay?.copy(isLocked = false, unlockErrorMessage = null, unlockErrorType = null),
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
                    requestId = buildRequestId(target.dateIso),
                    sign = target.sign,
                )
            }.onSuccess { unlockResult ->
                println("[HoroscopeViewModel] unlock success dateIso=${target.dateIso} costCharged=${unlockResult.costCharged} alreadyUnlocked=${unlockResult.alreadyUnlocked}")
                unlockedDatesSession += target.dateIso
                pendingUnlockTarget = null
                _uiState.update { current ->
                    val updatedDays = current.days.map { day ->
                        if (day.dateIso == target.dateIso) day.copy(isLocked = false, isUnlocked = true) else day
                    }
                    val updatedOverlay = current.overlay?.takeIf {
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
                val isInsufficient = error.message.orEmpty().lowercase().contains("insufficient") ||
                    error.message.orEmpty().lowercase().contains("moon")
                _uiState.update {
                    val unlockErrorMessage = if (isInsufficient) {
                        HoroscopeFeedbackMessage.UnlockInsufficientMoons
                    } else {
                        HoroscopeFeedbackMessage.UnlockFailed
                    }
                    val overlayMatchesTarget = it.overlay?.dateIso == target.dateIso && it.overlay?.sign == target.sign
                    if (overlayMatchesTarget) {
                        it.copy(
                            overlay = it.overlay?.copy(
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

    fun onInfoShown() = _uiState.update { it.copy(infoMessage = null) }
    fun onErrorShown() = _uiState.update { it.copy(errorMessage = null) }

    fun onSelectTab(tab: HoroscopeTab) {
        if (tab == HoroscopeTab.Daily) {
            _uiState.update { it.copy(selectedTab = HoroscopeTab.Daily) }
            return
        }
        _uiState.update { it.copy(infoMessage = HoroscopeFeedbackMessage.ComingSoon) }
    }

    private fun onSelectSign(sign: ZodiacSign, fromUserInteraction: Boolean) {
        if (fromUserInteraction) userHasManuallySelectedSign = true
        pendingUnlockTarget = null
        _uiState.update { it.copy(selectedSign = sign) }
        rebuildDays()
        observeSelectedHoroscope()
    }

    private fun reloadForCurrentSelection() {
        val today = todayDate()
        _uiState.update {
            it.copy(
                selectedDateIso = it.selectedDateIso.ifBlank { today.toString() },
                isLoading = true,
                overlay = null,
            )
        }
        rebuildDays()
        observeSelectedHoroscope()
    }

    private fun observeSelectedHoroscope() {
        val state = _uiState.value
        val dateIso = state.selectedDateIso.ifBlank { todayIso() }
        val sign = state.selectedSign
        val languageCode = currentLanguageCode.value

        observeJob?.cancel()
        observeJob = scope.launch {
            observeDailyHoroscopeUseCase(dateIso, sign, languageCode).collectLatest { cached ->
                _uiState.update { current ->
                    val overlay = current.overlay
                    val updatedOverlay = if (overlay != null && overlay.sign == sign && overlay.dateIso == dateIso && !overlay.isLocked) {
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
                println("[HoroscopeViewModel] overlay missing cached horoscope; pulling dateIso=$dateIso")
                safePull(dateIso = dateIso, languageCode = languageCode, showGlobalError = false)
                loaded = runCatching { getDailyHoroscopeUseCase(dateIso, sign, languageCode) }.getOrNull()
                println("[HoroscopeViewModel] pull future content result dateIso=$dateIso found=${loaded != null}")
            }
            _uiState.update { current ->
                val overlay = current.overlay ?: return@update current
                if (overlay.sign != sign || overlay.dateIso != dateIso) return@update current
                current.copy(overlay = overlay.copy(isLoading = false, horoscope = loaded))
            }
        }
    }

    private fun rebuildDays() {
        scope.launch {
            val state = _uiState.value
            val selectedIso = state.selectedDateIso.ifBlank { todayIso() }
            val today = todayDate()
            val cost = state.futureDayCost

            val items = (0..6).map { offset ->
                val date = today.plus(DatePeriod(days = offset))
                val dateIso = date.toString()
                val remoteUnlocked = runCatching { isHoroscopeDayUnlockedUseCase(dateIso) }
                    .onSuccess { println("[HoroscopeViewModel] isUnlocked(dateIso=$dateIso) result=$it") }
                    .onFailure { println("[HoroscopeViewModel] isUnlocked(dateIso=$dateIso) failed: ${it.message}") }
                    .getOrDefault(false)
                val unlocked = offset == 0 || unlockedDatesSession.contains(dateIso) || remoteUnlocked
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
            } else {
                println("[HoroscopeViewModel] silent pull failed dateIso=$dateIso: ${error.message}")
            }
        } finally {
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    private fun isDateUnlocked(dateIso: String): Boolean {
        val state = _uiState.value
        return unlockedDatesSession.contains(dateIso) || state.days.firstOrNull { it.dateIso == dateIso }?.isUnlocked == true
    }

    private fun todayDate(): LocalDate = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()).date
    private fun todayIso(): String = todayDate().toString()
    private fun shortDateLabel(date: LocalDate): String = "${date.dayOfMonth}/${date.monthNumber}"

    private fun buildRequestId(dateIso: String): String {
        return "horoscope-unlock-${Clock.System.now().toEpochMilliseconds()}-$dateIso"
    }
}
