package com.agc.bwitch.presentation.astrology.birthchart

import com.agc.bwitch.domain.astrology.birthchart.BirthChartRepository
import com.agc.bwitch.domain.astrology.birthchart.BirthChartSyncController
import com.agc.bwitch.domain.astrology.birthchart.BirthEssenceDraft
import com.agc.bwitch.domain.astrology.birthchart.BirthEssenceInput
import com.agc.bwitch.domain.astrology.birthchart.BirthEssenceProfile
import com.agc.bwitch.domain.astrology.birthchart.BirthEssenceReading
import com.agc.bwitch.domain.astrology.birthchart.GenerateBirthEssenceUseCase
import com.agc.bwitch.domain.astrology.birthchart.GetBirthEssenceUseCase
import com.agc.bwitch.domain.astrology.birthchart.ObserveBirthEssenceUseCase
import com.agc.bwitch.domain.astrology.birthchart.PullBirthEssenceUseCase
import com.agc.bwitch.domain.astrology.birthchart.SaveBirthEssenceUseCase
import com.agc.bwitch.domain.localization.AppLanguage
import com.agc.bwitch.domain.localization.AppLanguageRepository
import com.agc.bwitch.domain.localization.ObserveCurrentLanguageUseCase
import com.agc.bwitch.domain.localization.ResolveCurrentLanguageUseCase
import com.agc.bwitch.domain.shared.ApiError
import com.agc.bwitch.domain.shared.ApiResult
import kotlin.collections.ArrayDeque
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
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
class BirthChartViewModelTest {

    @Test
    fun `in progress retry reuses same requestId`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repository = FakeBirthChartRepository(
                scriptedResults = listOf(
                    ApiResult.Ok(BirthEssenceReading(interpretation = "", requestId = "server-a", status = "IN_PROGRESS")),
                    ApiResult.Ok(BirthEssenceReading(interpretation = "Complete", requestId = "server-a")),
                ),
            )
            val viewModel = birthChartViewModel(repository)

            viewModel.discoverEssence()
            advanceUntilIdle()
            val firstRequestId = repository.requests[0].requestId

            viewModel.discoverEssence()
            advanceUntilIdle()

            assertEquals(firstRequestId, repository.requests[1].requestId)
            assertEquals("Complete", viewModel.uiState.value.generatedInterpretation)
            assertEquals(false, viewModel.uiState.value.inProgress)
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `terminal error clears requestId so retry sends a new requestId`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        Dispatchers.setMain(dispatcher)
        try {
            val repository = FakeBirthChartRepository(
                scriptedResults = listOf(
                    ApiResult.Err(ApiError.FailedPrecondition("requestId already failed; retry with a new requestId")),
                    ApiResult.Ok(BirthEssenceReading(interpretation = "Recovered")),
                ),
            )
            val viewModel = birthChartViewModel(repository)

            viewModel.discoverEssence()
            advanceUntilIdle()
            val failedRequestId = repository.requests[0].requestId

            assertNull(viewModel.uiState.value.requestId)
            assertEquals(BIRTH_CHART_GENERATE_TEMPORARY_ATHENA_KEY, viewModel.uiState.value.error)

            viewModel.discoverEssence()
            advanceUntilIdle()

            assertNotEquals(failedRequestId, repository.requests[1].requestId)
            assertEquals("Recovered", viewModel.uiState.value.generatedInterpretation)
            assertNull(viewModel.uiState.value.error)
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun birthChartViewModel(repository: FakeBirthChartRepository): BirthChartViewModel {
        val languageRepository = FakeLanguageRepository(MutableStateFlow(AppLanguage.English))
        return BirthChartViewModel(
            observeBirthEssence = ObserveBirthEssenceUseCase(repository),
            getBirthEssence = GetBirthEssenceUseCase(repository),
            saveBirthEssence = SaveBirthEssenceUseCase(repository),
            pullBirthEssence = PullBirthEssenceUseCase(FakeBirthChartSyncController()),
            generateBirthEssence = GenerateBirthEssenceUseCase(repository),
            resolveCurrentLanguageUseCase = ResolveCurrentLanguageUseCase(languageRepository),
            observeCurrentLanguageUseCase = ObserveCurrentLanguageUseCase(languageRepository),
        )
    }
}

private class FakeBirthChartRepository(
    scriptedResults: List<ApiResult<BirthEssenceReading>>,
) : BirthChartRepository {
    private val results = ArrayDeque(scriptedResults)
    val requests = mutableListOf<BirthEssenceInput>()
    private val essence = MutableStateFlow<BirthEssenceProfile?>(null)

    override fun observeBirthEssence(): Flow<BirthEssenceProfile?> = essence
    override suspend fun getBirthEssence(): BirthEssenceProfile? = essence.value
    override suspend fun saveBirthEssence(draft: BirthEssenceDraft) = Unit

    override suspend fun generateBirthEssence(input: BirthEssenceInput): ApiResult<BirthEssenceReading> {
        requests += input
        assertTrue(!input.requestId.isNullOrBlank())
        return results.removeFirst()
    }
}

private class FakeBirthChartSyncController : BirthChartSyncController {
    override suspend fun pull() = Unit
}

private class FakeLanguageRepository(
    private val language: MutableStateFlow<AppLanguage>,
) : AppLanguageRepository {
    override suspend fun resolveCurrentLanguage(): AppLanguage = language.value
    override suspend fun getCurrentLanguage(): AppLanguage = language.value
    override suspend fun setCurrentLanguage(language: AppLanguage) {
        this.language.value = language
    }
    override fun observeCurrentLanguage(): Flow<AppLanguage> = language
}
