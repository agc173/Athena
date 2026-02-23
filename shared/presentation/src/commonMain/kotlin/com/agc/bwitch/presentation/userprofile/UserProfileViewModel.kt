package com.agc.bwitch.presentation.userprofile

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
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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

    // Evita “seed” repetidos (por ejemplo si se recrea la pantalla)
    private var seededForUid: String? = null

    init {
        // 1) observar cambios (source of truth)
        scope.launch {
            observe()
                .catch { e ->
                    _uiState.update { it.copy(isLoading = false, error = e.message) }
                    _snackbarEvents.tryEmit(e.message ?: "Error cargando el perfil")
                }
                .collectLatest { profile ->
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
            sessionVm.uiState
                .map { it.uid }
                .distinctUntilChanged()
                .collectLatest { uid ->
                    if (uid.isNullOrBlank()) return@collectLatest
                    if (seededForUid == uid) return@collectLatest
                    seededForUid = uid

                    // Si ya hay perfil (local/remote), no hacemos nada
                    val existing = runCatching { get() }.getOrNull()
                    if (!existing.isNullOrBlankProfile()) return@collectLatest

                    val session = sessionVm.uiState.value
                    val seeded = UserProfile(
                        displayName = session.displayName?.trim().takeUnless { it.isNullOrBlank() },
                        photoUrl = session.photoUrl?.trim().takeUnless { it.isNullOrBlank() },
                        email = session.email?.trim().takeUnless { it.isNullOrBlank() }
                    )

                    if (seeded.isNullOrBlankProfile()) return@collectLatest

                    runCatching { save(seeded) }
                        .onFailure { e ->
                            // Seed no es crítico: lo avisamos por snackbar pero no rompemos UI
                            _snackbarEvents.tryEmit(e.message ?: "No se pudo inicializar el perfil")
                        }
                }
        }
    }

    fun updateAndSave(displayName: String?, photoUrl: String?, email: String?) = scope.launch {
        val profile = UserProfile(
            displayName = displayName?.trim().takeUnless { it.isNullOrBlank() },
            photoUrl = photoUrl?.trim().takeUnless { it.isNullOrBlank() },
            email = email?.trim().takeUnless { it.isNullOrBlank() }
        )

        runCatching { save(profile) }
            .onSuccess { _snackbarEvents.tryEmit("Guardado correctamente") }
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

    private fun UserProfile?.isNullOrBlankProfile(): Boolean {
        if (this == null) return true
        val emptyName = displayName.isNullOrBlank()
        val emptyPhoto = photoUrl.isNullOrBlank()
        val emptyEmail = email.isNullOrBlank()
        return emptyName && emptyPhoto && emptyEmail
    }

    private fun setError(e: Throwable) {
        // adapta a tu state
        // _uiState.update { it.copy(error = e.message) }
        println("UserProfileViewModel error: ${e.message}")
    }
}