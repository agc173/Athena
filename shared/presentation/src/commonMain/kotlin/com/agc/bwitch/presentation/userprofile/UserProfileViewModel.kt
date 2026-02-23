package com.agc.bwitch.presentation.userprofile

import com.agc.bwitch.domain.userprofile.GetUserProfileUseCase
import com.agc.bwitch.domain.userprofile.ObserveUserProfileUseCase
import com.agc.bwitch.domain.userprofile.SaveUserProfileUseCase
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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.agc.bwitch.domain.userprofile.UploadAvatarUseCase

data class UserProfileUiState(
    val isLoading: Boolean = true,
    val profile: UserProfile? = null,
    val error: String? = null
)

class UserProfileViewModel(
    private val observe: ObserveUserProfileUseCase,
    private val get: GetUserProfileUseCase,
    private val save: SaveUserProfileUseCase,
    private val sessionVm: SessionViewModel,
    private val uploadAvatar: UploadAvatarUseCase
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

    init {
        // 1) observar cambios (source of truth)
        scope.launch {
            observe()
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                    _snackbarEvents.tryEmit(e.message ?: "Error cargando el perfil")
                }
                .collect { profile ->
                    _uiState.update { it.copy(isLoading = false, profile = profile, error = null) }
                }
        }

        // 2) warm up (provoca pull en sync repo)
        scope.launch {
            runCatching { get() }
                .onFailure { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                    _snackbarEvents.tryEmit(e.message ?: "Error cargando el perfil")
                }
        }

        // 3) seed inicial desde sesión si aún no existe perfil
        scope.launch {
            val existing = runCatching { get() }.getOrNull()
            if (existing != null) return@launch

            val session = sessionVm.uiState.value
            val seeded = UserProfile(
                displayName = session.displayName,
                photoUrl = session.photoUrl,
                email = session.email
            )

            if (seeded.displayName != null || seeded.photoUrl != null || seeded.email != null) {
                runCatching { save(seeded) }
                    .onFailure { e ->
                        // no “molestamos” demasiado si el seed falla, pero lo dejamos visible
                        _snackbarEvents.tryEmit(e.message ?: "No se pudo inicializar el perfil")
                    }
            }
        }
    }

    fun updateAndSave(displayName: String?, photoUrl: String?, email: String?) = scope.launch {
        val profile = UserProfile(
            displayName = displayName,
            photoUrl = photoUrl,
            email = email
        )

        runCatching { save(profile) }
            .onSuccess {
                _snackbarEvents.tryEmit("Guardado correctamente")
            }
            .onFailure { e ->
                _uiState.update { it.copy(error = e.message) }
                _snackbarEvents.tryEmit(e.message ?: "Error guardando el perfil")
            }
    }

    fun uploadAvatarAndSave(fileUri: String, mimeType: String? = null) = scope.launch {
        runCatching {
            val url = uploadAvatar(fileUri, mimeType)

            val current = uiState.value.profile
            val updated = UserProfile(
                displayName = current?.displayName,
                photoUrl = url,
                email = current?.email
            )

            save(updated)
            _snackbarEvents.tryEmit("Avatar actualizado")
        }.onFailure { e ->
            _snackbarEvents.tryEmit(e.message ?: "Error subiendo avatar")
        }
    }
}