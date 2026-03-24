package com.agc.bwitch.presentation.userprofile

import com.agc.bwitch.domain.astrology.birthchart.BirthEssenceProfile
import com.agc.bwitch.domain.astrology.birthchart.ObserveBirthEssenceUseCase
import com.agc.bwitch.domain.astrology.horoscope.DeriveZodiacSignUseCase
import com.agc.bwitch.domain.userprofile.GetUserProfileUseCase
import com.agc.bwitch.domain.userprofile.ObserveUserProfileUseCase
import com.agc.bwitch.domain.userprofile.PullUserProfileUseCase
import com.agc.bwitch.domain.userprofile.SaveUserProfileUseCase
import com.agc.bwitch.domain.userprofile.UploadAvatarUseCase
import com.agc.bwitch.domain.userprofile.UserProfile
import com.agc.bwitch.domain.userprofile.UsernameRules
import com.agc.bwitch.presentation.auth.SessionViewModel
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
import kotlinx.datetime.LocalDate

data class UserProfileUiState(
    val isInitialLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isUploadingAvatar: Boolean = false,
    val isRefreshing: Boolean = false,
    val profile: UserProfile? = null,
    val savedBirthEssence: BirthEssenceProfile? = null,
    val error: String? = null
) {
    val isBusy: Boolean get() = isSaving || isUploadingAvatar || isRefreshing
}

class UserProfileViewModel(
    private val observe: ObserveUserProfileUseCase,
    private val get: GetUserProfileUseCase,
    private val save: SaveUserProfileUseCase,
    private val sessionVm: SessionViewModel,
    private val uploadAvatar: UploadAvatarUseCase,
    private val pull: PullUserProfileUseCase,
    private val deriveZodiacSign: DeriveZodiacSignUseCase,
    private val observeBirthEssence: ObserveBirthEssenceUseCase,
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
            observe()
                .catch { e ->
                    _uiState.update { it.copy(isInitialLoading = false, error = e.message) }
                    _snackbarEvents.tryEmit(e.message ?: "Error cargando el perfil")
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
                    _snackbarEvents.tryEmit(e.message ?: "Error cargando el perfil")
                }
        }

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
                                _snackbarEvents.tryEmit(e.message ?: "No se pudo inicializar el perfil")
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
            _uiState.update { it.copy(isSaving = false, error = "Fecha inválida. Usa YYYY-MM-DD") }
            _snackbarEvents.tryEmit("Fecha inválida. Usa YYYY-MM-DD")
            return@launch
        }

        val zodiacSign = parsedBirthDate?.let(deriveZodiacSign::invoke)

        val normalizedUsername = UsernameRules.normalize(username)
        if (normalizedUsername != null && !UsernameRules.isValid(normalizedUsername)) {
            _uiState.update {
                it.copy(
                    isSaving = false,
                    error = "Username inválido. Usa 3-30 caracteres: letras, números, punto o guion bajo"
                )
            }
            _snackbarEvents.tryEmit("Username inválido")
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
            .onSuccess { _snackbarEvents.tryEmit("Guardado correctamente") }
            .onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
                _snackbarEvents.tryEmit(e.message ?: "Error guardando el perfil")
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
                zodiacSign = current?.zodiacSign ?: current?.birthDate?.let(deriveZodiacSign::invoke)
            )

            save(updated)
            _snackbarEvents.tryEmit("Avatar actualizado")
        }.onFailure { e ->
            _snackbarEvents.tryEmit(e.message ?: "Error subiendo avatar")
            _uiState.update { it.copy(error = e.message) }
        }

        _uiState.update { it.copy(isUploadingAvatar = false) }
    }

    fun refresh() = scope.launch {
        if (uiState.value.isBusy) return@launch

        _uiState.update { it.copy(isRefreshing = true, error = null) }

        runCatching { pull() }
            .onSuccess { _snackbarEvents.tryEmit("Perfil actualizado") }
            .onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
                _snackbarEvents.tryEmit(e.message ?: "Error refrescando perfil")
            }

        _uiState.update { it.copy(isRefreshing = false) }
    }

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
