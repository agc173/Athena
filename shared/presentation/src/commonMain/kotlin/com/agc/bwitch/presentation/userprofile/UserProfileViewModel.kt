package com.agc.bwitch.presentation.userprofile

import com.agc.bwitch.domain.astrology.birthchart.BirthEssenceProfile
import com.agc.bwitch.domain.astrology.birthchart.ObserveBirthEssenceUseCase
import com.agc.bwitch.domain.astrology.horoscope.DeriveZodiacSignUseCase
import com.agc.bwitch.domain.moons.ObserveMoonBalanceUseCase
import com.agc.bwitch.domain.rituals.DailyRitualRepository
import com.agc.bwitch.domain.rituals.HabitBadgeType
import com.agc.bwitch.domain.rituals.HabitsRepository
import com.agc.bwitch.domain.rituals.habitBadgeTypeForCycles
import com.agc.bwitch.domain.userprofile.GetUserProfileUseCase
import com.agc.bwitch.domain.userprofile.ObserveUserProfileUseCase
import com.agc.bwitch.domain.userprofile.PullUserProfileUseCase
import com.agc.bwitch.domain.userprofile.SaveUserProfileUseCase
import com.agc.bwitch.domain.userprofile.UploadAvatarUseCase
import com.agc.bwitch.domain.userprofile.UserProfile
import com.agc.bwitch.domain.userprofile.UsernameRules
import com.agc.bwitch.presentation.auth.SessionViewModel
import com.agc.bwitch.presentation.rituals.HabitsGlowLevel
import com.agc.bwitch.presentation.rituals.toHabitsGlowLevel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.atStartOfDayIn
import kotlinx.datetime.toLocalDateTime

data class UserProfileUiState(
    val isInitialLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isUploadingAvatar: Boolean = false,
    val isRefreshing: Boolean = false,
    val profile: UserProfile? = null,
    val savedBirthEssence: BirthEssenceProfile? = null,
    val moonBalance: Int = 0,
    val habitsProgress: ProfileHabitsProgressUi = ProfileHabitsProgressUi(),
    val error: String? = null
) {
    val isBusy: Boolean get() = isSaving || isUploadingAvatar || isRefreshing
}

data class ProfileHabitsProgressUi(
    val currentCyclePoints: Int = 0,
    val cycleTarget: Int = 60,
    val completedCycles: Int = 0,
    val hasStarted: Boolean = false,
    val activeBadgeType: HabitBadgeType = HabitBadgeType.Tree,
    val glowLevel: HabitsGlowLevel = HabitsGlowLevel.Base,
)

class UserProfileViewModel(
    private val observe: ObserveUserProfileUseCase,
    private val get: GetUserProfileUseCase,
    private val save: SaveUserProfileUseCase,
    private val sessionVm: SessionViewModel,
    private val uploadAvatar: UploadAvatarUseCase,
    private val pull: PullUserProfileUseCase,
    private val deriveZodiacSign: DeriveZodiacSignUseCase,
    private val observeBirthEssence: ObserveBirthEssenceUseCase,
    private val observeMoonBalanceUseCase: ObserveMoonBalanceUseCase,
    private val habitsRepository: HabitsRepository,
    private val dailyRitualRepository: DailyRitualRepository,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _uiState = MutableStateFlow(UserProfileUiState())
    val uiState: StateFlow<UserProfileUiState> = _uiState

    private val _snackbarEvents = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val snackbarEvents: SharedFlow<String> = _snackbarEvents.asSharedFlow()

    private var seededForUid: String? = null
    private var isSeeding: Boolean = false

    init {
        scope.launch {
            observeBirthEssence()
                .catch { e ->
                    _uiState.update { it.copy(error = e.message) }
                }
                .collectLatest { essence ->
                    _uiState.update { it.copy(savedBirthEssence = essence) }
                }
        }

        scope.launch {
            observeMoonBalanceUseCase()
                .catch { }
                .collectLatest { moonBalance ->
                    _uiState.update { it.copy(moonBalance = moonBalance.amount) }
                }
        }

        scope.launch {
            observe()
                .catch { e ->
                    _uiState.update { it.copy(isInitialLoading = false, error = e.message) }
                    _snackbarEvents.tryEmit(e.message ?: PROFILE_INIT_ERROR_KEY)
                }
                .collectLatest { profile ->
                    _uiState.update {
                        it.copy(
                            isInitialLoading = false,
                            profile = profile,
                            error = null
                        )
                    }
                }
        }

        scope.launch {
            runCatching { get() }
                .onFailure { e ->
                    _uiState.update { it.copy(isInitialLoading = false, error = e.message) }
                    _snackbarEvents.tryEmit(e.message ?: PROFILE_INIT_ERROR_KEY)
                }
        }

        loadHabitsProgress()

        scope.launch {
            sessionVm.uiState
                .map { it.uid }
                .distinctUntilChanged()
                .collectLatest { uid ->
                    if (uid.isNullOrBlank()) return@collectLatest
                    if (seededForUid == uid) return@collectLatest
                    if (isSeeding) return@collectLatest
                    isSeeding = true

                    try {
                        seededForUid = uid
                        val existing = runCatching { get() }.getOrNull()
                        if (!existing.isNullOrBlankProfile()) return@collectLatest

                        val session = sessionVm.uiState.value
                        val seeded = UserProfile(
                            displayName = session.displayName?.trim().takeUnless { it.isNullOrBlank() },
                            photoUrl = session.photoUrl?.trim().takeUnless { it.isNullOrBlank() },
                            email = session.email?.trim().takeUnless { it.isNullOrBlank() },
                            username = null,
                            birthDate = null,
                            zodiacSign = null
                        )

                        if (seeded.isNullOrBlankProfile()) return@collectLatest

                        runCatching { save(seeded) }
                            .onFailure { e ->
                                _snackbarEvents.tryEmit(e.message ?: PROFILE_INIT_ERROR_KEY)
                            }
                    } finally {
                        isSeeding = false
                    }
                }
        }
    }

    fun updateAndSave(
        displayName: String?,
        photoUrl: String?,
        email: String?,
        username: String?,
        birthDateText: String?
    ) = scope.launch {
        if (uiState.value.isBusy) return@launch

        _uiState.update { it.copy(isSaving = true, error = null) }

        val parsedBirthDate = birthDateText
            ?.trim()
            ?.takeUnless { it.isBlank() }
            ?.let { runCatching { LocalDate.parse(it) }.getOrNull() }

        if (!birthDateText.isNullOrBlank() && parsedBirthDate == null) {
            _uiState.update { it.copy(isSaving = false, error = PROFILE_BIRTH_DATE_INVALID_ERROR_KEY) }
            _snackbarEvents.tryEmit(PROFILE_BIRTH_DATE_INVALID_ERROR_KEY)
            return@launch
        }

        val zodiacSign = parsedBirthDate?.let(deriveZodiacSign::invoke)

        val normalizedUsername = UsernameRules.normalize(username)
        if (normalizedUsername != null && !UsernameRules.isValid(normalizedUsername)) {
            _uiState.update {
                it.copy(
                    isSaving = false,
                    error = PROFILE_USERNAME_INVALID_ERROR_KEY
                )
            }
            _snackbarEvents.tryEmit(PROFILE_USERNAME_INVALID_ERROR_KEY)
            return@launch
        }

        val profile = UserProfile(
            displayName = displayName?.trim().takeUnless { it.isNullOrBlank() },
            photoUrl = photoUrl?.trim().takeUnless { it.isNullOrBlank() },
            email = email?.trim().takeUnless { it.isNullOrBlank() },
            username = normalizedUsername,
            birthDate = parsedBirthDate,
            zodiacSign = zodiacSign
        )

        runCatching { save(profile) }
            .onSuccess { _snackbarEvents.tryEmit(PROFILE_SAVE_SUCCESS_MESSAGE_KEY) }
            .onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
                _snackbarEvents.tryEmit(e.message ?: PROFILE_SAVE_ERROR_KEY)
            }

        _uiState.update { it.copy(isSaving = false) }
    }

    fun uploadAvatarAndSave(fileUri: String, mimeType: String? = null) = scope.launch {
        if (uiState.value.isBusy) return@launch

        _uiState.update { it.copy(isUploadingAvatar = true, error = null) }

        runCatching {
            val current = uiState.value.profile
            val previousUrl = current?.photoUrl
            val url = uploadAvatar(fileUri, mimeType, previousUrl)

            val updated = UserProfile(
                displayName = current?.displayName,
                photoUrl = url,
                email = current?.email
                    ?: sessionVm.uiState.value.email?.trim().takeUnless { it.isNullOrBlank() },
                username = current?.username,
                birthDate = current?.birthDate,
                zodiacSign = current?.zodiacSign ?: current?.birthDate?.let(deriveZodiacSign::invoke),
                description = current?.description
            )

            save(updated)
            _snackbarEvents.tryEmit(PROFILE_AVATAR_UPDATED_MESSAGE_KEY)
        }.onFailure { e ->
            _snackbarEvents.tryEmit(e.message ?: PROFILE_AVATAR_UPLOAD_ERROR_KEY)
            _uiState.update { it.copy(error = e.message) }
        }

        _uiState.update { it.copy(isUploadingAvatar = false) }
    }

    suspend fun saveEditableProfile(description: String?, birthDateText: String?): Boolean {
        if (uiState.value.isBusy) return false
        _uiState.update { it.copy(isSaving = true, error = null) }
        val current = uiState.value.profile
        val birthInput = birthDateText?.trim().orEmpty()
        val parsedBirthDate = if (birthInput.isBlank()) current?.birthDate else runCatching { LocalDate.parse(birthInput) }.getOrNull()
        if (birthInput.isNotBlank() && parsedBirthDate == null) {
            _uiState.update { it.copy(isSaving = false, error = PROFILE_BIRTH_DATE_INVALID_ERROR_KEY) }
            _snackbarEvents.tryEmit(PROFILE_BIRTH_DATE_INVALID_ERROR_KEY)
            return false
        }
        if (parsedBirthDate != null && parsedBirthDate.atStartOfDayIn(TimeZone.UTC) > Clock.System.now()) {
            _uiState.update { it.copy(isSaving = false, error = PROFILE_BIRTH_DATE_IN_FUTURE_ERROR_KEY) }
            _snackbarEvents.tryEmit(PROFILE_BIRTH_DATE_IN_FUTURE_ERROR_KEY)
            return false
        }
        val normalizedDescription = description
            ?.replace(Regex("\\s+"), " ")
            ?.trim()
            ?.takeIf { it.isNotBlank() }
        if ((normalizedDescription?.length ?: 0) > 160) {
            _uiState.update { it.copy(isSaving = false, error = PROFILE_DESCRIPTION_TOO_LONG_ERROR_KEY) }
            _snackbarEvents.tryEmit(PROFILE_DESCRIPTION_TOO_LONG_ERROR_KEY)
            return false
        }
        val zodiacSign = if (birthInput.isBlank()) current?.zodiacSign else parsedBirthDate?.let(deriveZodiacSign::invoke)
        val updated = UserProfile(
            displayName = current?.displayName,
            photoUrl = current?.photoUrl,
            email = current?.email ?: sessionVm.uiState.value.email?.trim().takeUnless { it.isNullOrBlank() },
            username = current?.username,
            birthDate = parsedBirthDate,
            zodiacSign = zodiacSign,
            description = normalizedDescription,
            birthEssenceSummary = current?.birthEssenceSummary,
        )
        val success = runCatching { save(updated) }
            .onSuccess { _snackbarEvents.tryEmit(PROFILE_SAVE_SUCCESS_MESSAGE_KEY) }
            .onFailure { e -> _uiState.update { it.copy(error = e.message) }; _snackbarEvents.tryEmit(e.message ?: PROFILE_SAVE_ERROR_KEY) }
            .isSuccess
        _uiState.update { it.copy(isSaving = false) }
        return success
    }

    fun refresh() = scope.launch {
        if (uiState.value.isBusy) return@launch

        _uiState.update { it.copy(isRefreshing = true, error = null) }

        runCatching { pull() }
            .onSuccess { _snackbarEvents.tryEmit(PROFILE_REFRESH_SUCCESS_MESSAGE_KEY) }
            .onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
                _snackbarEvents.tryEmit(e.message ?: PROFILE_SAVE_ERROR_KEY)
            }

        loadHabitsProgress()
        _uiState.update { it.copy(isRefreshing = false) }
    }

    private fun loadHabitsProgress() {
        runCatching {
            val progress = habitsRepository.getProgress()
            val streak = dailyRitualRepository.getStreakForDate(todayDate())
            progress to streak
        }
            .onSuccess { (progress, streak) ->
                _uiState.update {
                    it.copy(
                        habitsProgress = ProfileHabitsProgressUi(
                            currentCyclePoints = progress.currentCyclePoints,
                            cycleTarget = progress.cycleTarget,
                            completedCycles = progress.completedCycles,
                            hasStarted = progress.currentCyclePoints > 0 || progress.completedCycles > 0,
                            activeBadgeType = habitBadgeTypeForCycles(progress.completedCycles),
                            glowLevel = streak.toHabitsGlowLevel(),
                        )
                    )
                }
            }
    }

    private fun todayDate(): LocalDate = Clock.System.now()
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .date

    private fun UserProfile?.isNullOrBlankProfile(): Boolean {
        if (this == null) return true
        val emptyName = displayName.isNullOrBlank()
        val emptyPhoto = photoUrl.isNullOrBlank()
        val emptyEmail = email.isNullOrBlank()
        val emptyUsername = username.isNullOrBlank()
        val emptyBirthDate = birthDate == null
        val emptySign = zodiacSign == null
        return emptyName && emptyPhoto && emptyEmail && emptyUsername && emptyBirthDate && emptySign
    }
}

const val PROFILE_INIT_ERROR_KEY = "profile.error.init"
const val PROFILE_BIRTH_DATE_INVALID_ERROR_KEY = "profile.error.birth_date_invalid"
const val PROFILE_USERNAME_INVALID_ERROR_KEY = "profile.error.username_invalid"
const val PROFILE_SAVE_SUCCESS_MESSAGE_KEY = "profile.info.saved_success"
const val PROFILE_REFRESH_SUCCESS_MESSAGE_KEY = "profile.info.refresh_success"
const val PROFILE_SAVE_ERROR_KEY = "profile.error.save"
const val PROFILE_AVATAR_UPDATED_MESSAGE_KEY = "profile.info.avatar_updated"
const val PROFILE_AVATAR_UPLOAD_ERROR_KEY = "profile.error.avatar_upload"
const val PROFILE_DESCRIPTION_TOO_LONG_ERROR_KEY = "profile.error.description_too_long"
const val PROFILE_BIRTH_DATE_IN_FUTURE_ERROR_KEY = "profile.error.birth_date_in_future"
