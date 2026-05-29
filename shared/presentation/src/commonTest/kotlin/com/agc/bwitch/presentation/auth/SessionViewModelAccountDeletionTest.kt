package com.agc.bwitch.presentation.auth

import com.agc.bwitch.domain.account.AccountDeletionRepository
import com.agc.bwitch.domain.account.AccountDeletionStatus
import com.agc.bwitch.domain.account.RestorePendingAccountDeletionUseCase
import com.agc.bwitch.domain.auth.AuthRepository
import com.agc.bwitch.domain.auth.AuthUser
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SessionViewModelAccountDeletionTest {
    @Test
    fun `bootstrap with pending deletion restores account before exposing logged in state`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val accountRepository = FakeAccountDeletionRepository(AccountDeletionStatus(pendingDeletion = true))
            val authRepository = FakeAuthRepository(
                AuthUser(uid = "uid-1", email = "a@b.com", isAnonymous = false),
            )

            val viewModel = SessionViewModel(
                authRepository = authRepository,
                restorePendingAccountDeletion = RestorePendingAccountDeletionUseCase(accountRepository),
            )

            advanceUntilIdle()

            assertTrue(accountRepository.restoreCalled)
            assertTrue(viewModel.uiState.value.isLoggedIn)
            assertFalse(authRepository.signOutCalled)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `generic pending deletion check failure does not sign out normal users`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val accountRepository = FakeAccountDeletionRepository(
                status = null,
                getStatusError = IllegalStateException("temporary network error"),
            )
            val authRepository = FakeAuthRepository(
                AuthUser(uid = "uid-1", email = "a@b.com", isAnonymous = false),
            )

            val viewModel = SessionViewModel(
                authRepository = authRepository,
                restorePendingAccountDeletion = RestorePendingAccountDeletionUseCase(accountRepository),
            )

            advanceUntilIdle()

            assertTrue(viewModel.uiState.value.isLoggedIn)
            assertFalse(authRepository.signOutCalled)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `explicit expired deletion window signs out`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val accountRepository = FakeAccountDeletionRepository(
                status = null,
                getStatusError = IllegalStateException("account_deletion_window_expired"),
            )
            val authRepository = FakeAuthRepository(
                AuthUser(uid = "uid-1", email = "a@b.com", isAnonymous = false),
            )

            val viewModel = SessionViewModel(
                authRepository = authRepository,
                restorePendingAccountDeletion = RestorePendingAccountDeletionUseCase(accountRepository),
            )

            advanceUntilIdle()

            assertFalse(viewModel.uiState.value.isLoggedIn)
            assertTrue(authRepository.signOutCalled)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `sign out job completes only after repository sign out completes`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val signOutGate = CompletableDeferred<Unit>()
            val authRepository = FakeAuthRepository(currentUser = null, signOutGate = signOutGate)
            val viewModel = SessionViewModel(authRepository = authRepository)

            advanceUntilIdle()

            val signOutJob = viewModel.signOut()
            advanceUntilIdle()

            assertTrue(authRepository.signOutStarted)
            assertFalse(signOutJob.isCompleted)

            signOutGate.complete(Unit)
            advanceUntilIdle()

            assertTrue(signOutJob.isCompleted)
            assertTrue(authRepository.signOutCalled)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private class FakeAuthRepository(
        currentUser: AuthUser?,
        private val signOutGate: CompletableDeferred<Unit>? = null,
    ) : AuthRepository {
        override val authState: Flow<AuthUser?> = MutableStateFlow(currentUser)
        var signOutStarted = false
        var signOutCalled = false

        override suspend fun signInWithEmail(email: String, password: String) = Unit
        override suspend fun signUpWithEmail(email: String, password: String) = Unit
        override suspend fun signOut() {
            signOutStarted = true
            signOutGate?.await()
            signOutCalled = true
        }
        override suspend fun signInWithGoogleIdToken(idToken: String) = Unit
    }

    private class FakeAccountDeletionRepository(
        private val status: AccountDeletionStatus?,
        private val getStatusError: Throwable? = null,
        private val restoreError: Throwable? = null,
    ) : AccountDeletionRepository {
        var restoreCalled = false
        override suspend fun getStatus(uid: String): AccountDeletionStatus? {
            getStatusError?.let { throw it }
            return status
        }
        override suspend fun requestAccountDeletion() = Unit
        override suspend fun restoreAccount() {
            restoreCalled = true
            restoreError?.let { throw it }
        }
    }
}
