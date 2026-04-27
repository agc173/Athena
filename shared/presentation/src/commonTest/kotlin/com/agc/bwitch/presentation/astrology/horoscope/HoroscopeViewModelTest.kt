package com.agc.bwitch.presentation.astrology.horoscope

import com.agc.bwitch.domain.astrology.horoscope.DailyHoroscope
import com.agc.bwitch.domain.astrology.horoscope.GetDailyHoroscopeUseCase
import com.agc.bwitch.domain.astrology.horoscope.GetHoroscopeFutureDayCostUseCase
import com.agc.bwitch.domain.astrology.horoscope.HoroscopeDailySyncController
import com.agc.bwitch.domain.astrology.horoscope.HoroscopePullMarker
import com.agc.bwitch.domain.astrology.horoscope.HoroscopeRepository
import com.agc.bwitch.domain.astrology.horoscope.HoroscopeUnlockRepository
import com.agc.bwitch.domain.astrology.horoscope.HoroscopeUnlockResult
import com.agc.bwitch.domain.astrology.horoscope.IsHoroscopeDayUnlockedUseCase
import com.agc.bwitch.domain.astrology.horoscope.ObserveDailyHoroscopeUseCase
import com.agc.bwitch.domain.astrology.horoscope.PullDailyHoroscopeUseCase
import com.agc.bwitch.domain.astrology.horoscope.UnlockHoroscopeFutureDayUseCase
import com.agc.bwitch.domain.astrology.horoscope.ZodiacSign
import com.agc.bwitch.domain.localization.AppLanguage
import com.agc.bwitch.domain.localization.AppLanguageRepository
import com.agc.bwitch.domain.localization.ObserveCurrentLanguageUseCase
import com.agc.bwitch.domain.localization.ResolveCurrentLanguageUseCase
import com.agc.bwitch.domain.userprofile.ObserveUserProfileUseCase
import com.agc.bwitch.domain.userprofile.UserProfile
import com.agc.bwitch.domain.userprofile.UserProfileRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime

@OptIn(ExperimentalCoroutinesApi::class)
class HoroscopeViewModelTest {

    @Test
    fun initLoadsDefaultSignAndHoroscopeIsNotNull() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)

        val repo = FakeRepo()
        val observeUseCase = ObserveDailyHoroscopeUseCase(repo)
        val getUseCase = GetDailyHoroscopeUseCase(repo)
        val pullUseCase = PullDailyHoroscopeUseCase(FakeSync())
        val languageRepository = FakeAppLanguageRepository()
        val resolveLanguageUseCase = ResolveCurrentLanguageUseCase(languageRepository)
        val observeLanguageUseCase = ObserveCurrentLanguageUseCase(languageRepository)
        val observeUserProfileUseCase = ObserveUserProfileUseCase(FakeUserProfileRepo())
        val unlockRepository = FakeUnlockRepository()
        val getHoroscopeFutureDayCostUseCase = GetHoroscopeFutureDayCostUseCase(unlockRepository)
        val isHoroscopeDayUnlockedUseCase = IsHoroscopeDayUnlockedUseCase(unlockRepository)
        val unlockHoroscopeFutureDayUseCase = UnlockHoroscopeFutureDayUseCase(unlockRepository)

        val pullMarker = FakePullMarker(lastPulledDateIso = null) // fuerza pull (pero FakeSync no falla)

        // Pre-cargamos el valor que emitirá observe()
        repo.emit(
            DailyHoroscope(
                sign = ZodiacSign.aries,
                dateIso = "2026-02-25",
                languageCode = "es",
                text = "Texto para Aries",
                mood = "Positivo",
                luckyNumber = 7,
                luckyColor = "Azul",
            )
        )

        val viewModel = HoroscopeViewModel(
            observeDailyHoroscopeUseCase = observeUseCase,
            getDailyHoroscopeUseCase = getUseCase,
            pullDailyHoroscopeUseCase = pullUseCase,
            pullMarker = pullMarker,
            resolveCurrentLanguageUseCase = resolveLanguageUseCase,
            observeCurrentLanguageUseCase = observeLanguageUseCase,
            observeUserProfileUseCase = observeUserProfileUseCase,
            getHoroscopeFutureDayCostUseCase = getHoroscopeFutureDayCostUseCase,
            isHoroscopeDayUnlockedUseCase = isHoroscopeDayUnlockedUseCase,
            unlockHoroscopeFutureDayUseCase = unlockHoroscopeFutureDayUseCase,
            unlockRepository = unlockRepository,
            dispatcher = dispatcher,
        )

        advanceUntilIdle()
        viewModel.onOpenSign(ZodiacSign.aries)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.overlay?.horoscope)
        assertNull(state.errorMessage)
        assertEquals(false, state.isLoading)
    }

    @Test
    fun weekly_isLockedByDefault_whenNoUnlockExists() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val unlockRepository = FakeUnlockRepository()
        val viewModel = createViewModel(dispatcher, unlockRepository)

        advanceUntilIdle()
        viewModel.onSelectTab(HoroscopeTab.Weekly)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isWeekLocked)
    }

    @Test
    fun weekly_isUnlocked_whenRemoteReturnsSelectedWeekKey() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val unlockRepository = FakeUnlockRepository(unlockQueriedWeeks = true)
        val viewModel = createViewModel(dispatcher, unlockRepository)

        advanceUntilIdle()
        viewModel.onSelectTab(HoroscopeTab.Weekly)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isWeekLocked)
    }

    @Test
    fun unlockWeek_keepsSessionUnlocked_whenRemoteBatchFailsLater() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val unlockRepository = FakeUnlockRepository()
        val viewModel = createViewModel(dispatcher, unlockRepository)

        advanceUntilIdle()
        viewModel.onSelectTab(HoroscopeTab.Weekly)
        advanceUntilIdle()
        viewModel.onUnlockSelectedWeek()
        advanceUntilIdle()

        unlockRepository.failWeekBatchReads = true
        viewModel.onSelectWeek(HoroscopeWeekPeriod.ThisWeek)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isWeekLocked)
    }

    @Test
    fun unlockMonth_setsUnlockedState() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)
        val unlockRepository = FakeUnlockRepository()
        val viewModel = createViewModel(dispatcher, unlockRepository)

        advanceUntilIdle()
        viewModel.onSelectTab(HoroscopeTab.Monthly)
        advanceUntilIdle()
        viewModel.onUnlockSelectedMonth()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isMonthLocked)
    }

    @Test
    fun onSelectSignUpdatesSelectedSignAndHoroscope() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)

        val repo = FakeRepo()
        val observeUseCase = ObserveDailyHoroscopeUseCase(repo)
        val getUseCase = GetDailyHoroscopeUseCase(repo)
        val pullUseCase = PullDailyHoroscopeUseCase(FakeSync())
        val languageRepository = FakeAppLanguageRepository()
        val resolveLanguageUseCase = ResolveCurrentLanguageUseCase(languageRepository)
        val observeLanguageUseCase = ObserveCurrentLanguageUseCase(languageRepository)
        val observeUserProfileUseCase = ObserveUserProfileUseCase(FakeUserProfileRepo())
        val unlockRepository = FakeUnlockRepository()
        val getHoroscopeFutureDayCostUseCase = GetHoroscopeFutureDayCostUseCase(unlockRepository)
        val isHoroscopeDayUnlockedUseCase = IsHoroscopeDayUnlockedUseCase(unlockRepository)
        val unlockHoroscopeFutureDayUseCase = UnlockHoroscopeFutureDayUseCase(unlockRepository)

        val pullMarker = FakePullMarker(lastPulledDateIso = null)

        // Inicial: aries
        repo.emit(
            DailyHoroscope(
                sign = ZodiacSign.aries,
                dateIso = "2026-02-25",
                languageCode = "es",
                text = "Texto Aries",
                mood = "Positivo",
                luckyNumber = 7,
                luckyColor = "Azul",
            )
        )

        val viewModel = HoroscopeViewModel(
            observeDailyHoroscopeUseCase = observeUseCase,
            getDailyHoroscopeUseCase = getUseCase,
            pullDailyHoroscopeUseCase = pullUseCase,
            pullMarker = pullMarker,
            resolveCurrentLanguageUseCase = resolveLanguageUseCase,
            observeCurrentLanguageUseCase = observeLanguageUseCase,
            observeUserProfileUseCase = observeUserProfileUseCase,
            getHoroscopeFutureDayCostUseCase = getHoroscopeFutureDayCostUseCase,
            isHoroscopeDayUnlockedUseCase = isHoroscopeDayUnlockedUseCase,
            unlockHoroscopeFutureDayUseCase = unlockHoroscopeFutureDayUseCase,
            unlockRepository = unlockRepository,
            dispatcher = dispatcher,
        )

        advanceUntilIdle()

        // Cambiamos signo y emitimos el nuevo valor
        viewModel.onSelectSign(ZodiacSign.leo)
        repo.emit(
            DailyHoroscope(
                sign = ZodiacSign.leo,
                dateIso = "2026-02-25",
                languageCode = "es",
                text = "Texto Leo",
                mood = "Fuerte",
                luckyNumber = 1,
                luckyColor = "Rojo",
            )
        )

        advanceUntilIdle()
        viewModel.onOpenSign(ZodiacSign.leo)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(ZodiacSign.leo, state.selectedSign)
        assertEquals(ZodiacSign.leo, state.overlay?.horoscope?.sign)
        assertNull(state.errorMessage)
    }

    @Test
    fun initDoesNotPullIfAlreadyPulledToday_evenIfSyncWouldFail() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)

        val repo = FakeRepo()
        val observeUseCase = ObserveDailyHoroscopeUseCase(repo)
        val getUseCase = GetDailyHoroscopeUseCase(repo)
        val languageRepository = FakeAppLanguageRepository()
        val resolveLanguageUseCase = ResolveCurrentLanguageUseCase(languageRepository)
        val observeLanguageUseCase = ObserveCurrentLanguageUseCase(languageRepository)
        val observeUserProfileUseCase = ObserveUserProfileUseCase(FakeUserProfileRepo())
        val unlockRepository = FakeUnlockRepository()
        val getHoroscopeFutureDayCostUseCase = GetHoroscopeFutureDayCostUseCase(unlockRepository)
        val isHoroscopeDayUnlockedUseCase = IsHoroscopeDayUnlockedUseCase(unlockRepository)
        val unlockHoroscopeFutureDayUseCase = UnlockHoroscopeFutureDayUseCase(unlockRepository)

        // Este sync fallaría si se llamase pull()
        val pullUseCase = PullDailyHoroscopeUseCase(FailingSync())

        // Marcamos "ya se pulleó hoy" para que el init NO intente tirar de red
        // OJO: esto depende de la fecha real del sistema en el test. Para evitar flakiness,
        // el FakePullMarker se inicializa con el valor que el VM vaya a comparar:
        // como el VM usa todayIso() internamente, aquí lo dejamos null y comprobamos que el test NO
        // falle por pull si pre-cargamos el marker con el mismo todayIso (ver mejora abajo).
        //
        // Versión estable: calculamos hoy como lo hace el VM (simplemente no lo tenemos aquí).
        // Para que sea 100% estable, lo ideal es inyectar Clock al VM, pero por ahora hacemos un truco:
        val todayIso = currentSystemTodayIsoForTests()
        val pullMarker = FakePullMarker(lastPulledDateIso = todayIso)

        val viewModel = HoroscopeViewModel(
            observeDailyHoroscopeUseCase = observeUseCase,
            getDailyHoroscopeUseCase = getUseCase,
            pullDailyHoroscopeUseCase = pullUseCase,
            pullMarker = pullMarker,
            resolveCurrentLanguageUseCase = resolveLanguageUseCase,
            observeCurrentLanguageUseCase = observeLanguageUseCase,
            observeUserProfileUseCase = observeUserProfileUseCase,
            getHoroscopeFutureDayCostUseCase = getHoroscopeFutureDayCostUseCase,
            isHoroscopeDayUnlockedUseCase = isHoroscopeDayUnlockedUseCase,
            unlockHoroscopeFutureDayUseCase = unlockHoroscopeFutureDayUseCase,
            unlockRepository = unlockRepository,
            dispatcher = dispatcher,
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        // Si no se llamó pull(), no debe haber error aunque el sync sea failing
        assertNull(state.errorMessage)
    }


    @Test
    fun unlock_keepsFutureDayUnlockedInSession_whenRemoteReadReturnsFalse() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)

        val repo = FakeRepo()
        val observeUseCase = ObserveDailyHoroscopeUseCase(repo)
        val getUseCase = GetDailyHoroscopeUseCase(repo)
        val pullUseCase = PullDailyHoroscopeUseCase(FakeSync())
        val languageRepository = FakeAppLanguageRepository()
        val resolveLanguageUseCase = ResolveCurrentLanguageUseCase(languageRepository)
        val observeLanguageUseCase = ObserveCurrentLanguageUseCase(languageRepository)
        val observeUserProfileUseCase = ObserveUserProfileUseCase(FakeUserProfileRepo())
        val unlockRepository = FakeNonPersistingUnlockRepository()
        val getHoroscopeFutureDayCostUseCase = GetHoroscopeFutureDayCostUseCase(unlockRepository)
        val isHoroscopeDayUnlockedUseCase = IsHoroscopeDayUnlockedUseCase(unlockRepository)
        val unlockHoroscopeFutureDayUseCase = UnlockHoroscopeFutureDayUseCase(unlockRepository)

        val viewModel = HoroscopeViewModel(
            observeDailyHoroscopeUseCase = observeUseCase,
            getDailyHoroscopeUseCase = getUseCase,
            pullDailyHoroscopeUseCase = pullUseCase,
            pullMarker = FakePullMarker(lastPulledDateIso = currentSystemTodayIsoForTests()),
            resolveCurrentLanguageUseCase = resolveLanguageUseCase,
            observeCurrentLanguageUseCase = observeLanguageUseCase,
            observeUserProfileUseCase = observeUserProfileUseCase,
            getHoroscopeFutureDayCostUseCase = getHoroscopeFutureDayCostUseCase,
            isHoroscopeDayUnlockedUseCase = isHoroscopeDayUnlockedUseCase,
            unlockHoroscopeFutureDayUseCase = unlockHoroscopeFutureDayUseCase,
            unlockRepository = unlockRepository,
            dispatcher = dispatcher,
        )

        advanceUntilIdle()
        val futureDate = viewModel.uiState.value.days[1].dateIso

        viewModel.onSelectDate(futureDate)
        viewModel.onOpenSign(ZodiacSign.aries)
        viewModel.onUnlockSelectedDay()
        advanceUntilIdle()

        val dayAfterUnlock = viewModel.uiState.value.days.first { it.dateIso == futureDate }
        assertEquals(true, dayAfterUnlock.isUnlocked)
        assertEquals(false, dayAfterUnlock.isLocked)
        assertEquals(1, unlockRepository.unlockCalls)
    }

    @Test
    fun openUnlockedOverlay_pullsFutureContentSilently_whenCacheIsEmpty() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)

        val repo = FakeRepo()
        val observeUseCase = ObserveDailyHoroscopeUseCase(repo)
        val getUseCase = GetDailyHoroscopeUseCase(repo)
        val sync = PopulateOnPullSync(repo)
        val pullUseCase = PullDailyHoroscopeUseCase(sync)
        val languageRepository = FakeAppLanguageRepository()
        val resolveLanguageUseCase = ResolveCurrentLanguageUseCase(languageRepository)
        val observeLanguageUseCase = ObserveCurrentLanguageUseCase(languageRepository)
        val observeUserProfileUseCase = ObserveUserProfileUseCase(FakeUserProfileRepo())
        val unlockRepository = FakeUnlockRepository(unlockedDates = setOf(currentSystemTomorrowIsoForTests()))
        val getHoroscopeFutureDayCostUseCase = GetHoroscopeFutureDayCostUseCase(unlockRepository)
        val isHoroscopeDayUnlockedUseCase = IsHoroscopeDayUnlockedUseCase(unlockRepository)
        val unlockHoroscopeFutureDayUseCase = UnlockHoroscopeFutureDayUseCase(unlockRepository)

        val viewModel = HoroscopeViewModel(
            observeDailyHoroscopeUseCase = observeUseCase,
            getDailyHoroscopeUseCase = getUseCase,
            pullDailyHoroscopeUseCase = pullUseCase,
            pullMarker = FakePullMarker(lastPulledDateIso = currentSystemTodayIsoForTests()),
            resolveCurrentLanguageUseCase = resolveLanguageUseCase,
            observeCurrentLanguageUseCase = observeLanguageUseCase,
            observeUserProfileUseCase = observeUserProfileUseCase,
            getHoroscopeFutureDayCostUseCase = getHoroscopeFutureDayCostUseCase,
            isHoroscopeDayUnlockedUseCase = isHoroscopeDayUnlockedUseCase,
            unlockHoroscopeFutureDayUseCase = unlockHoroscopeFutureDayUseCase,
            unlockRepository = unlockRepository,
            dispatcher = dispatcher,
        )

        advanceUntilIdle()
        val futureDate = viewModel.uiState.value.days[1].dateIso

        viewModel.onSelectDate(futureDate)
        viewModel.onOpenSign(ZodiacSign.aries)
        advanceUntilIdle()

        assertNotNull(viewModel.uiState.value.overlay?.horoscope)
        assertNull(viewModel.uiState.value.errorMessage)
        assertEquals(1, sync.pullCalls)
    }

    @Test
    fun rebuildDays_marksTodayUnlocked_andFutureDayLockedWhenRepositoryReturnsFalse() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)

        val repo = FakeRepo()
        val observeUseCase = ObserveDailyHoroscopeUseCase(repo)
        val getUseCase = GetDailyHoroscopeUseCase(repo)
        val pullUseCase = PullDailyHoroscopeUseCase(FakeSync())
        val languageRepository = FakeAppLanguageRepository()
        val resolveLanguageUseCase = ResolveCurrentLanguageUseCase(languageRepository)
        val observeLanguageUseCase = ObserveCurrentLanguageUseCase(languageRepository)
        val observeUserProfileUseCase = ObserveUserProfileUseCase(FakeUserProfileRepo())
        val unlockRepository = FakeUnlockRepository(unlockedDates = emptySet())
        val getHoroscopeFutureDayCostUseCase = GetHoroscopeFutureDayCostUseCase(unlockRepository)
        val isHoroscopeDayUnlockedUseCase = IsHoroscopeDayUnlockedUseCase(unlockRepository)
        val unlockHoroscopeFutureDayUseCase = UnlockHoroscopeFutureDayUseCase(unlockRepository)

        val pullMarker = FakePullMarker(lastPulledDateIso = null)

        val viewModel = HoroscopeViewModel(
            observeDailyHoroscopeUseCase = observeUseCase,
            getDailyHoroscopeUseCase = getUseCase,
            pullDailyHoroscopeUseCase = pullUseCase,
            pullMarker = pullMarker,
            resolveCurrentLanguageUseCase = resolveLanguageUseCase,
            observeCurrentLanguageUseCase = observeLanguageUseCase,
            observeUserProfileUseCase = observeUserProfileUseCase,
            getHoroscopeFutureDayCostUseCase = getHoroscopeFutureDayCostUseCase,
            isHoroscopeDayUnlockedUseCase = isHoroscopeDayUnlockedUseCase,
            unlockHoroscopeFutureDayUseCase = unlockHoroscopeFutureDayUseCase,
            unlockRepository = unlockRepository,
            dispatcher = dispatcher,
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        val today = state.days.firstOrNull()
        val futureDay = state.days.getOrNull(1)

        assertNotNull(today)
        assertNotNull(futureDay)
        assertEquals(true, today.isUnlocked)
        assertEquals(false, futureDay.isUnlocked)
        assertEquals(true, futureDay.isLocked)
    }

    @Test
    fun rebuildDays_marksTomorrowUnlocked_whenRepositoryReturnsRemoteUnlock() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)

        val repo = FakeRepo()
        val observeUseCase = ObserveDailyHoroscopeUseCase(repo)
        val getUseCase = GetDailyHoroscopeUseCase(repo)
        val pullUseCase = PullDailyHoroscopeUseCase(FakeSync())
        val languageRepository = FakeAppLanguageRepository()
        val resolveLanguageUseCase = ResolveCurrentLanguageUseCase(languageRepository)
        val observeLanguageUseCase = ObserveCurrentLanguageUseCase(languageRepository)
        val observeUserProfileUseCase = ObserveUserProfileUseCase(FakeUserProfileRepo())
        val unlockRepository = FakeUnlockRepository(unlockedDates = setOf(currentSystemTomorrowIsoForTests()))
        val getHoroscopeFutureDayCostUseCase = GetHoroscopeFutureDayCostUseCase(unlockRepository)
        val isHoroscopeDayUnlockedUseCase = IsHoroscopeDayUnlockedUseCase(unlockRepository)
        val unlockHoroscopeFutureDayUseCase = UnlockHoroscopeFutureDayUseCase(unlockRepository)

        val viewModel = HoroscopeViewModel(
            observeDailyHoroscopeUseCase = observeUseCase,
            getDailyHoroscopeUseCase = getUseCase,
            pullDailyHoroscopeUseCase = pullUseCase,
            pullMarker = FakePullMarker(lastPulledDateIso = currentSystemTodayIsoForTests()),
            resolveCurrentLanguageUseCase = resolveLanguageUseCase,
            observeCurrentLanguageUseCase = observeLanguageUseCase,
            observeUserProfileUseCase = observeUserProfileUseCase,
            getHoroscopeFutureDayCostUseCase = getHoroscopeFutureDayCostUseCase,
            isHoroscopeDayUnlockedUseCase = isHoroscopeDayUnlockedUseCase,
            unlockHoroscopeFutureDayUseCase = unlockHoroscopeFutureDayUseCase,
            unlockRepository = unlockRepository,
            dispatcher = dispatcher,
        )

        advanceUntilIdle()

        val tomorrow = viewModel.uiState.value.days[1]
        assertEquals(true, tomorrow.isUnlocked)
        assertEquals(false, tomorrow.isLocked)
    }

    private class FakeRepo : HoroscopeRepository {
        private val state = MutableStateFlow<DailyHoroscope?>(null)

        fun emit(value: DailyHoroscope?) {
            state.value = value
        }

        override fun observeDaily(dateIso: String, sign: ZodiacSign, languageCode: String): Flow<DailyHoroscope?> {
            // Simplificación: ignoramos filtros (date/sign) y emitimos lo que toque.
            return state.asStateFlow()
        }

        override suspend fun getDaily(dateIso: String, sign: ZodiacSign, languageCode: String): DailyHoroscope? {
            return state.value
        }
    }

    private fun createViewModel(
        dispatcher: StandardTestDispatcher,
        unlockRepository: HoroscopeUnlockRepository,
    ): HoroscopeViewModel {
        val repo = FakeRepo()
        val observeUseCase = ObserveDailyHoroscopeUseCase(repo)
        val getUseCase = GetDailyHoroscopeUseCase(repo)
        val pullUseCase = PullDailyHoroscopeUseCase(FakeSync())
        val languageRepository = FakeAppLanguageRepository()
        val resolveLanguageUseCase = ResolveCurrentLanguageUseCase(languageRepository)
        val observeLanguageUseCase = ObserveCurrentLanguageUseCase(languageRepository)
        val observeUserProfileUseCase = ObserveUserProfileUseCase(FakeUserProfileRepo())
        val getHoroscopeFutureDayCostUseCase = GetHoroscopeFutureDayCostUseCase(unlockRepository)
        val isHoroscopeDayUnlockedUseCase = IsHoroscopeDayUnlockedUseCase(unlockRepository)
        val unlockHoroscopeFutureDayUseCase = UnlockHoroscopeFutureDayUseCase(unlockRepository)
        val pullMarker = FakePullMarker(lastPulledDateIso = currentSystemTodayIsoForTests())
        return HoroscopeViewModel(
            observeDailyHoroscopeUseCase = observeUseCase,
            getDailyHoroscopeUseCase = getUseCase,
            pullDailyHoroscopeUseCase = pullUseCase,
            pullMarker = pullMarker,
            resolveCurrentLanguageUseCase = resolveLanguageUseCase,
            observeCurrentLanguageUseCase = observeLanguageUseCase,
            observeUserProfileUseCase = observeUserProfileUseCase,
            getHoroscopeFutureDayCostUseCase = getHoroscopeFutureDayCostUseCase,
            isHoroscopeDayUnlockedUseCase = isHoroscopeDayUnlockedUseCase,
            unlockHoroscopeFutureDayUseCase = unlockHoroscopeFutureDayUseCase,
            unlockRepository = unlockRepository,
            dispatcher = dispatcher,
        )
    }

    private class FakeSync : HoroscopeDailySyncController {
        override suspend fun pull(dateIso: String, languageCode: String) {
            // NO-OP
        }
    }

    private class FailingSync : HoroscopeDailySyncController {
        override suspend fun pull(dateIso: String, languageCode: String) {
            throw RuntimeException("boom")
        }
    }

    private class FakePullMarker(
        private var lastPulledDateIso: String?
    ) : HoroscopePullMarker {

        override fun getLastPulledDateIso(languageCode: String): String? = lastPulledDateIso

        override fun setLastPulledDateIso(dateIso: String, languageCode: String) {
            lastPulledDateIso = dateIso
        }
    }

    private class FakeAppLanguageRepository : AppLanguageRepository {
        override suspend fun resolveCurrentLanguage(): AppLanguage = AppLanguage.Spanish
        override suspend fun getCurrentLanguage(): AppLanguage = AppLanguage.Spanish
        override suspend fun setCurrentLanguage(language: AppLanguage) = Unit
        override fun observeCurrentLanguage(): Flow<AppLanguage> = MutableStateFlow(AppLanguage.Spanish)
    }

    private class FakeUserProfileRepo : UserProfileRepository {
        override fun observeUserProfile(): Flow<UserProfile?> = MutableStateFlow(null)

        override suspend fun getUserProfile(): UserProfile? = null

        override suspend fun saveUserProfile(profile: UserProfile) = Unit
    }


    private class FakeNonPersistingUnlockRepository : HoroscopeUnlockRepository {
        var unlockCalls: Int = 0

        override suspend fun getFutureDayCost(): Int = 1

        override suspend fun isUnlocked(dateIso: String): Boolean = false

        override suspend fun getUnlockedDays(dateIsoList: List<String>): Set<String> = emptySet()

        override suspend fun getWeeklyCost(): Int = 2
        override suspend fun getMonthlyCost(): Int = 3

        override suspend fun unlockFutureDay(dateIso: String, requestId: String, sign: ZodiacSign): HoroscopeUnlockResult {
            unlockCalls += 1
            return HoroscopeUnlockResult(
                unlocked = true,
                alreadyUnlocked = false,
                balanceAfter = 0,
                costCharged = 1,
            )
        }
    }

    private class PopulateOnPullSync(
        private val repo: FakeRepo,
    ) : HoroscopeDailySyncController {
        var pullCalls: Int = 0

        override suspend fun pull(dateIso: String, languageCode: String) {
            pullCalls += 1
            repo.emit(
                DailyHoroscope(
                    sign = ZodiacSign.aries,
                    dateIso = dateIso,
                    languageCode = languageCode,
                    text = "Texto futuro",
                    mood = "Calmo",
                    luckyNumber = 9,
                    luckyColor = "Verde",
                )
            )
        }
    }
    private class FakeUnlockRepository(
        private val futureDayCost: Int = 1,
        unlockedDates: Set<String> = emptySet(),
        unlockedWeeks: Set<String> = emptySet(),
        unlockedMonths: Set<String> = emptySet(),
        private val unlockQueriedWeeks: Boolean = false,
        private val unlockQueriedMonths: Boolean = false,
    ) : HoroscopeUnlockRepository {
        private val unlocked = unlockedDates.toMutableSet()
        private val unlockedWeekKeys = unlockedWeeks.toMutableSet()
        private val unlockedMonthKeys = unlockedMonths.toMutableSet()
        var failWeekBatchReads: Boolean = false
        var failMonthBatchReads: Boolean = false

        override suspend fun getFutureDayCost(): Int = futureDayCost
        override suspend fun getWeeklyCost(): Int = 2
        override suspend fun getMonthlyCost(): Int = 3

        override suspend fun isUnlocked(dateIso: String): Boolean = unlocked.contains(dateIso)

        override suspend fun getUnlockedDays(dateIsoList: List<String>): Set<String> =
            dateIsoList.filterTo(mutableSetOf()) { unlocked.contains(it) }

        override suspend fun unlockFutureDay(dateIso: String, requestId: String, sign: ZodiacSign): HoroscopeUnlockResult {
            val alreadyUnlocked = unlocked.contains(dateIso)
            unlocked.add(dateIso)
            return HoroscopeUnlockResult(
                unlocked = true,
                alreadyUnlocked = alreadyUnlocked,
                balanceAfter = 0,
                costCharged = if (alreadyUnlocked) 0 else futureDayCost,
            )
        }

        override suspend fun getUnlockedWeeks(weekKeyList: List<String>): Set<String> {
            if (failWeekBatchReads) throw IllegalStateException("batch week read failed")
            if (unlockQueriedWeeks) return weekKeyList.toSet()
            return weekKeyList.filterTo(mutableSetOf()) { unlockedWeekKeys.contains(it) }
        }

        override suspend fun getUnlockedMonths(monthKeyList: List<String>): Set<String> {
            if (failMonthBatchReads) throw IllegalStateException("batch month read failed")
            if (unlockQueriedMonths) return monthKeyList.toSet()
            return monthKeyList.filterTo(mutableSetOf()) { unlockedMonthKeys.contains(it) }
        }

        override suspend fun unlockWeek(weekKey: String, requestId: String, sign: ZodiacSign): HoroscopeUnlockResult {
            val alreadyUnlocked = unlockedWeekKeys.contains(weekKey)
            unlockedWeekKeys += weekKey
            return HoroscopeUnlockResult(
                unlocked = true,
                alreadyUnlocked = alreadyUnlocked,
                balanceAfter = 0,
                costCharged = if (alreadyUnlocked) 0 else 2,
            )
        }

        override suspend fun unlockMonth(monthKey: String, requestId: String, sign: ZodiacSign): HoroscopeUnlockResult {
            val alreadyUnlocked = unlockedMonthKeys.contains(monthKey)
            unlockedMonthKeys += monthKey
            return HoroscopeUnlockResult(
                unlocked = true,
                alreadyUnlocked = alreadyUnlocked,
                balanceAfter = 0,
                costCharged = if (alreadyUnlocked) 0 else 3,
            )
        }
    }
}

/**
 * Helper de test para obtener yyyy-MM-dd igual que el VM (Clock.System + TimeZone.currentSystemDefault()).
 * Mantenerlo aquí evita meter Clock como dependencia del ViewModel solo por tests.
 */
private fun currentSystemTodayIsoForTests(): String {
    val now = kotlinx.datetime.Clock.System.now()
        .toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
    return now.date.toString()
}

private fun currentSystemTomorrowIsoForTests(): String {
    val now = kotlinx.datetime.Clock.System.now()
        .toLocalDateTime(kotlinx.datetime.TimeZone.currentSystemDefault())
        .date
    return now.plus(kotlinx.datetime.DatePeriod(days = 1)).toString()
}
