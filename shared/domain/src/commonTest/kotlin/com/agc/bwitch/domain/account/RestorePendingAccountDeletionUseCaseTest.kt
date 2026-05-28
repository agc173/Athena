package com.agc.bwitch.domain.account

import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class RestorePendingAccountDeletionUseCaseTest {
    @Test
    fun `login with pending deletion restores account`() = runTest {
        val repository = FakeAccountDeletionRepository(AccountDeletionStatus(pendingDeletion = true))
        val useCase = RestorePendingAccountDeletionUseCase(repository)

        val restored = useCase("uid-1")

        assertTrue(restored)
        assertTrue(repository.restoreCalled)
    }

    @Test
    fun `login without pending deletion does not restore account`() = runTest {
        val repository = FakeAccountDeletionRepository(AccountDeletionStatus(pendingDeletion = false))
        val useCase = RestorePendingAccountDeletionUseCase(repository)

        val restored = useCase("uid-1")

        assertFalse(restored)
        assertFalse(repository.restoreCalled)
    }

    private class FakeAccountDeletionRepository(
        private val status: AccountDeletionStatus?,
    ) : AccountDeletionRepository {
        var restoreCalled = false

        override suspend fun getStatus(uid: String): AccountDeletionStatus? = status
        override suspend fun requestAccountDeletion() = Unit
        override suspend fun restoreAccount() {
            restoreCalled = true
        }
    }
}
