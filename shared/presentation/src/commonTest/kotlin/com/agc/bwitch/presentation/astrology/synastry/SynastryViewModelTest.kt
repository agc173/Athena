package com.agc.bwitch.presentation.astrology.synastry

import com.agc.bwitch.domain.astrology.horoscope.ZodiacSign
import com.agc.bwitch.domain.astrology.synastry.SynastryReadingGenerator
import com.agc.bwitch.domain.economy.EconomyBalance
import com.agc.bwitch.domain.economy.EconomyClaimResult
import com.agc.bwitch.domain.economy.EconomyClaimStatus
import com.agc.bwitch.domain.economy.EconomyModulePreview
import com.agc.bwitch.domain.economy.EconomyRepository
import com.agc.bwitch.domain.economy.EconomyStatus
import com.agc.bwitch.domain.economy.SynastryAuthorizationResult
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
        val fakeEconomyRepository = FakeEconomyRepository()
        val viewModel = SynastryViewModel(
            readingGenerator = SynastryReadingGenerator(),
            resolveCurrentLanguageUseCase = ResolveCurrentLanguageUseCase(languageRepository),
            observeCurrentLanguageUseCase = ObserveCurrentLanguageUseCase(languageRepository),
            economyRepository = fakeEconomyRepository,
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
        val fakeEconomyRepository = FakeEconomyRepository()
        val viewModel = SynastryViewModel(
            readingGenerator = SynastryReadingGenerator(),
            resolveCurrentLanguageUseCase = ResolveCurrentLanguageUseCase(languageRepository),
            observeCurrentLanguageUseCase = ObserveCurrentLanguageUseCase(languageRepository),
            economyRepository = fakeEconomyRepository,
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

private class FakeEconomyRepository : EconomyRepository {
    override suspend fun getBalance(): EconomyBalance = EconomyBalance(
        balance = 0,
        dailyLoginClaimed = false,
        rewardedAdsClaimed = 0,
        rewardedAdsRemaining = 0,
    )

    override suspend fun getStatus(): EconomyStatus = EconomyStatus(
        balance = 0,
        isPremium = false,
        todayDateIso = "2026-01-01",
    )

    override suspend fun claimDailyLogin(requestId: String): EconomyClaimResult = EconomyClaimResult(
        result = EconomyClaimStatus.ALREADY_CLAIMED,
        balance = 0,
        dailyLoginClaimed = false,
        rewardedAdsClaimed = 0,
        rewardedAdsRemaining = 0,
    )

    override suspend fun claimRewardedAd(
        requestId: String,
        adProof: String,
        placement: String?,
    ): EconomyClaimResult = EconomyClaimResult(
        result = EconomyClaimStatus.ALREADY_CLAIMED,
        balance = 0,
        dailyLoginClaimed = false,
        rewardedAdsClaimed = 0,
        rewardedAdsRemaining = 0,
    )

    override suspend fun getModulePreviews(modules: List<String>): List<EconomyModulePreview> = emptyList()

    override suspend fun authorizeSynastry(
        requestId: String,
        languageCode: String?,
    ): SynastryAuthorizationResult = SynastryAuthorizationResult(
        authorized = true,
        economyDisabled = true,
    )
}
