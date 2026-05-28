package com.agc.bwitch.data.astrology.horoscope

import com.agc.bwitch.data.functions.FunctionsClient
import com.agc.bwitch.domain.auth.AuthRepository
import com.agc.bwitch.domain.auth.AuthUser
import com.agc.bwitch.domain.shared.ApiError
import com.agc.bwitch.domain.shared.ApiResult
import com.russhwolf.settings.MapSettings
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.KSerializer

class BackendFirstConstellationProgressRepositoryTest {
    @Test
    fun refreshProgress_backendSuccess_updatesLocalCache() = runTest {
        val local = SettingsConstellationProgressRepository(MapSettings())
        local.saveTotalProgress(1)
        val repository = BackendFirstConstellationProgressRepository(
            localRepository = local,
            authRepository = FakeAuthRepository(AuthUser(uid = "u1", email = null, isAnonymous = false, displayName = null, photoUrl = null)),
            functionsClient = FakeFunctionsClient(ApiResult.Ok(GetConstellationProgressResponseDto(7, "2026-05-28", false)))
        )
        val result = repository.refreshProgress()
        assertEquals(7, result)
        assertEquals(7, local.getTotalProgress())
        assertEquals("2026-05-28", local.getLastRewardDateIso())
    }

    @Test
    fun refreshProgress_backendFailure_keepsLocalFallback() = runTest {
        val local = SettingsConstellationProgressRepository(MapSettings())
        local.saveTotalProgress(5)
        val repository = BackendFirstConstellationProgressRepository(
            localRepository = local,
            authRepository = FakeAuthRepository(AuthUser(uid = "u1", email = null, isAnonymous = false, displayName = null, photoUrl = null)),
            functionsClient = FakeFunctionsClient(ApiResult.Err(ApiError.Internal("boom")))
        )
        val result = repository.refreshProgress()
        assertEquals(5, result)
        assertEquals(5, local.getTotalProgress())
    }

    @Test
    fun refreshProgress_noAuth_usesLocal() = runTest {
        val local = SettingsConstellationProgressRepository(MapSettings())
        local.saveTotalProgress(6)
        val repository = BackendFirstConstellationProgressRepository(
            localRepository = local,
            authRepository = FakeAuthRepository(null),
            functionsClient = FakeFunctionsClient(ApiResult.Err(ApiError.Internal("unused")))
        )
        val result = repository.refreshProgress()
        assertEquals(6, result)
    }

    @Test
    fun backendSuccess_updatesLocalCache() = runTest {
        val local = SettingsConstellationProgressRepository(MapSettings())
        val repository = BackendFirstConstellationProgressRepository(
            localRepository = local,
            authRepository = FakeAuthRepository(AuthUser(uid = "u1", email = null, isAnonymous = false, displayName = null, photoUrl = null)),
            functionsClient = FakeFunctionsClient(
                ApiResult.Ok(
                    ClaimDailyConstellationProgressResponseDto(4, 3, rewarded = true, isComplete = false)
                )
            )
        )

        val result = repository.claimDailyProgress("2026-05-28", 10)

        assertTrue(result.rewarded)
        assertEquals(4, local.getTotalProgress())
        assertEquals("2026-05-28", local.getLastRewardDateIso())
    }

    @Test
    fun backendFailure_fallsBackToLocal() = runTest {
        val local = SettingsConstellationProgressRepository(MapSettings())
        local.saveTotalProgress(3)
        val repository = BackendFirstConstellationProgressRepository(
            localRepository = local,
            authRepository = FakeAuthRepository(AuthUser(uid = "u1", email = null, isAnonymous = false, displayName = null, photoUrl = null)),
            functionsClient = FakeFunctionsClient(ApiResult.Err(ApiError.Internal("boom")))
        )

        val result = repository.claimDailyProgress("2026-05-28", 10)

        assertTrue(result.rewarded)
        assertEquals(4, result.totalProgress)
    }

    @Test
    fun sameDay_returnsRewardedFalse() = runTest {
        val local = SettingsConstellationProgressRepository(MapSettings())
        local.saveTotalProgress(4)
        local.saveLastRewardDateIso("2026-05-28")
        val repository = BackendFirstConstellationProgressRepository(
            localRepository = local,
            authRepository = FakeAuthRepository(null),
            functionsClient = FakeFunctionsClient(ApiResult.Err(ApiError.Internal("unused")))
        )

        val result = repository.claimDailyProgress("2026-05-28", 10)

        assertFalse(result.rewarded)
        assertEquals(4, result.totalProgress)
    }

    private class FakeFunctionsClient(
        private val result: ApiResult<out Any>
    ) : FunctionsClient {
        override suspend fun <Req : Any, Res : Any> call(
            name: String,
            data: Req,
            requestSerializer: KSerializer<Req>,
            responseSerializer: KSerializer<Res>,
        ): ApiResult<Res> = result as ApiResult<Res>
    }

    private class FakeAuthRepository(currentUser: AuthUser?) : AuthRepository {
        override val authState: Flow<AuthUser?> = MutableStateFlow(currentUser)
        override suspend fun signInWithEmail(email: String, password: String) = Unit
        override suspend fun signUpWithEmail(email: String, password: String) = Unit
        override suspend fun signOut() = Unit
        override suspend fun signInWithGoogleIdToken(idToken: String) = Unit
    }
}
