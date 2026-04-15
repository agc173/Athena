package com.agc.bwitch.presentation.tarot

import com.agc.bwitch.domain.localization.AppLanguage
import com.agc.bwitch.domain.localization.AppLanguageRepository
import com.agc.bwitch.domain.localization.ObserveCurrentLanguageUseCase
import com.agc.bwitch.domain.localization.ResolveCurrentLanguageUseCase
import com.agc.bwitch.domain.shared.ApiError
import com.agc.bwitch.domain.shared.ApiResult
import com.agc.bwitch.domain.tarot.TarotDrawResponse
import com.agc.bwitch.domain.tarot.TarotRepository
import com.agc.bwitch.domain.tarot.TarotRequestType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class TarotViewModelTest {

    @Test
    fun `newRequest uses current app language for backend draw`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repo = FakeTarotRepository()
            val languageRepo = FakeLanguageRepository(MutableStateFlow(AppLanguage.German))
            val viewModel = TarotViewModel(
                tarotRepository = repo,
                resolveCurrentLanguageUseCase = ResolveCurrentLanguageUseCase(languageRepo),
                observeCurrentLanguageUseCase = ObserveCurrentLanguageUseCase(languageRepo),
            )

            viewModel.newRequest(TarotRequestType.TAROT_1)
            advanceUntilIdle()

            assertEquals("de", repo.lastLang)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private class FakeTarotRepository : TarotRepository {
        var lastLang: String? = null

        override suspend fun tarotDraw(
            requestId: String,
            type: TarotRequestType,
            lang: String?,
            question: String?,
        ): ApiResult<TarotDrawResponse> {
            lastLang = lang
            return ApiResult.Err(ApiError.Internal("mock error"))
        }
    }

    private class FakeLanguageRepository(
        private val state: MutableStateFlow<AppLanguage>,
    ) : AppLanguageRepository {
        override suspend fun resolveCurrentLanguage(): AppLanguage = state.value
        override suspend fun getCurrentLanguage(): AppLanguage = state.value
        override suspend fun setCurrentLanguage(language: AppLanguage) {
            state.value = language
        }
        override fun observeCurrentLanguage(): Flow<AppLanguage> = state
    }
}
