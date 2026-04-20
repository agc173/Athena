package com.agc.bwitch.presentation.astrology.synastry

import com.agc.bwitch.domain.astrology.horoscope.ZodiacSign
import com.agc.bwitch.domain.astrology.synastry.SynastryReadingGenerator
import com.agc.bwitch.domain.localization.AppLanguage
import com.agc.bwitch.domain.localization.AppLanguageRepository
import com.agc.bwitch.domain.localization.ObserveCurrentLanguageUseCase
import com.agc.bwitch.domain.localization.ResolveCurrentLanguageUseCase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest

class SynastryViewModelTest {

    @Test
    fun `generate uses current language code from app language`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val languageRepository = FakeLanguageRepository(MutableStateFlow(AppLanguage.English))
        val viewModel = SynastryViewModel(
            readingGenerator = SynastryReadingGenerator(),
            resolveCurrentLanguageUseCase = ResolveCurrentLanguageUseCase(languageRepository),
            observeCurrentLanguageUseCase = ObserveCurrentLanguageUseCase(languageRepository),
            dispatcher = dispatcher,
        )

        viewModel.onPersonASunSignChange(ZodiacSign.aries)
        viewModel.onPersonBSunSignChange(ZodiacSign.libra)
        advanceUntilIdle()
        viewModel.generate()
        advanceUntilIdle()

        assertEquals("en", viewModel.uiState.value.currentLanguageCode)
        assertTrue(viewModel.uiState.value.reading?.narrative?.contains(" and ") == true)
    }

    @Test
    fun `generate keeps required sun sign validation`() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val languageRepository = FakeLanguageRepository(MutableStateFlow(AppLanguage.Spanish))
        val viewModel = SynastryViewModel(
            readingGenerator = SynastryReadingGenerator(),
            resolveCurrentLanguageUseCase = ResolveCurrentLanguageUseCase(languageRepository),
            observeCurrentLanguageUseCase = ObserveCurrentLanguageUseCase(languageRepository),
            dispatcher = dispatcher,
        )

        viewModel.generate()
        advanceUntilIdle()

        assertEquals("required_sun_signs_error", viewModel.uiState.value.error)
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
    override fun observeCurrentLanguage() = state
}
