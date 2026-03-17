package com.agc.bwitch.presentation.userprofile

import com.agc.bwitch.domain.astrology.horoscope.DeriveZodiacSignUseCase
import com.agc.bwitch.domain.userprofile.GetUserProfileUseCase
import com.agc.bwitch.domain.userprofile.ObserveUserProfileUseCase
import com.agc.bwitch.domain.userprofile.SaveUserProfileUseCase
import com.agc.bwitch.domain.userprofile.UploadAvatarUseCase
import com.agc.bwitch.domain.userprofile.UserProfile
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

data class OnboardingProfileUiState(
    val isInitialLoading: Boolean = true,
    val isSaving: Boolean = false,
    val isUploadingAvatar: Boolean = false,
    val profile: UserProfile? = null,
    val error: String? = null,
    val authDisplayName: String? = null,
    val authEmail: String? = null,
) {
    val isBusy: Boolean get() = isSaving || isUploadingAvatar
}

class OnboardingProfileViewModel(
    private val observe: ObserveUserProfileUseCase,
    private val get: GetUserProfileUseCase,
    private val save: SaveUserProfileUseCase,
    private val uploadAvatar: UploadAvatarUseCase,
    private val deriveZodiacSign: DeriveZodiacSignUseCase,
    private val sessionVm: SessionViewModel,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    private val _uiState = MutableStateFlow(
        OnboardingProfileUiState(
            authDisplayName = sessionVm.uiState.value.displayName,
            authEmail = sessionVm.uiState.value.email,
        )
    )
    val uiState: StateFlow<OnboardingProfileUiState> = _uiState

    private val _snackbarEvents = MutableSharedFlow<String>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val snackbarEvents: SharedFlow<String> = _snackbarEvents.asSharedFlow()

    init {
        scope.launch {
            observe()
                .catch { e ->
                    _uiState.update { it.copy(isInitialLoading = false, error = e.message) }
                    _snackbarEvents.tryEmit(e.message ?: "Error cargando perfil")
                }
                .collectLatest { profile ->
                    _uiState.update {
                        it.copy(
                            isInitialLoading = false,
                            profile = profile,
                            error = null,
                            authDisplayName = sessionVm.uiState.value.displayName,
                            authEmail = sessionVm.uiState.value.email,
                        )
                    }
                }
        }

        scope.launch {
            runCatching { get() }
                .onFailure { e ->
                    _uiState.update { it.copy(isInitialLoading = false, error = e.message) }
                    _snackbarEvents.tryEmit(e.message ?: "Error cargando perfil")
                }
        }
    }

    fun completeOnboarding(usernameText: String, birthDateText: String) = scope.launch {
        if (uiState.value.isBusy) return@launch

        val username = usernameText.trim().removePrefix("@").takeUnless { it.isBlank() }
        if (username == null) {
            _uiState.update { it.copy(error = "El username es obligatorio") }
            return@launch
        }

        val birthDate = runCatching { LocalDate.parse(birthDateText.trim()) }.getOrNull()
        if (birthDate == null) {
            _uiState.update { it.copy(error = "Fecha inválida. Usa YYYY-MM-DD") }
            return@launch
        }

        _uiState.update { it.copy(isSaving = true, error = null) }

        val base = currentBaseProfile()
        val updated = base.copy(
            username = username,
            birthDate = birthDate,
            zodiacSign = deriveZodiacSign(birthDate)
        )

        runCatching { save(updated) }
            .onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
                _snackbarEvents.tryEmit(e.message ?: "No se pudo guardar el perfil")
            }

        _uiState.update { it.copy(isSaving = false) }
    }

    fun uploadAvatarAndSave(fileUri: String, mimeType: String? = null) = scope.launch {
        if (uiState.value.isBusy) return@launch

        _uiState.update { it.copy(isUploadingAvatar = true, error = null) }

        runCatching {
            val current = currentBaseProfile()
            val uploadedUrl = uploadAvatar(
                fileUri = fileUri,
                mimeType = mimeType,
                previousUrl = current.photoUrl
            )
            save(current.copy(photoUrl = uploadedUrl))
            _snackbarEvents.tryEmit("Avatar actualizado")
        }.onFailure { e ->
            _uiState.update { it.copy(error = e.message) }
            _snackbarEvents.tryEmit(e.message ?: "No se pudo subir el avatar")
        }

        _uiState.update { it.copy(isUploadingAvatar = false) }
    }

    private fun currentBaseProfile(): UserProfile {
        val current = uiState.value.profile
        val session = sessionVm.uiState.value
        return UserProfile(
            displayName = current?.displayName ?: session.displayName?.trim().takeUnless { it.isNullOrBlank() },
            photoUrl = current?.photoUrl,
            email = current?.email ?: session.email?.trim().takeUnless { it.isNullOrBlank() },
            username = current?.username,
            birthDate = current?.birthDate,
            zodiacSign = current?.zodiacSign,
        )
    }
}
