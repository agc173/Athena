package com.agc.bwitch.presentation.astrology.horoscope

import com.agc.bwitch.domain.astrology.horoscope.DailyHoroscope
import com.agc.bwitch.domain.astrology.horoscope.GetDailyHoroscopeUseCase
import com.agc.bwitch.domain.astrology.horoscope.HoroscopeDailySyncController
import com.agc.bwitch.domain.astrology.horoscope.HoroscopePullMarker
import com.agc.bwitch.domain.astrology.horoscope.HoroscopeRepository
import com.agc.bwitch.domain.astrology.horoscope.ObserveDailyHoroscopeUseCase
import com.agc.bwitch.domain.astrology.horoscope.PullDailyHoroscopeUseCase
import com.agc.bwitch.domain.astrology.horoscope.ZodiacSign
import com.agc.bwitch.domain.userprofile.ObserveUserProfileUseCase
import com.agc.bwitch.domain.userprofile.UserProfile
import com.agc.bwitch.domain.userprofile.UserProfileRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
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
        val observeUserProfileUseCase = ObserveUserProfileUseCase(FakeUserProfileRepo())

        val pullMarker = FakePullMarker(lastPulledDateIso = null) // fuerza pull (pero FakeSync no falla)

        // Pre-cargamos el valor que emitirá observe()
        repo.emit(
            DailyHoroscope(
                sign = ZodiacSign.aries,
                dateIso = "2026-02-25",
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
            observeUserProfileUseCase = observeUserProfileUseCase,
            dispatcher = dispatcher,
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.horoscope)
        assertNull(state.errorMessage)
        assertEquals(false, state.isLoading)
    }

    @Test
    fun onSelectSignUpdatesSelectedSignAndHoroscope() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)

        val repo = FakeRepo()
        val observeUseCase = ObserveDailyHoroscopeUseCase(repo)
        val getUseCase = GetDailyHoroscopeUseCase(repo)
        val pullUseCase = PullDailyHoroscopeUseCase(FakeSync())
        val observeUserProfileUseCase = ObserveUserProfileUseCase(FakeUserProfileRepo())

        val pullMarker = FakePullMarker(lastPulledDateIso = null)

        // Inicial: aries
        repo.emit(
            DailyHoroscope(
                sign = ZodiacSign.aries,
                dateIso = "2026-02-25",
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
            observeUserProfileUseCase = observeUserProfileUseCase,
            dispatcher = dispatcher,
        )

        advanceUntilIdle()

        // Cambiamos signo y emitimos el nuevo valor
        viewModel.onSelectSign(ZodiacSign.leo)
        repo.emit(
            DailyHoroscope(
                sign = ZodiacSign.leo,
                dateIso = "2026-02-25",
                text = "Texto Leo",
                mood = "Fuerte",
                luckyNumber = 1,
                luckyColor = "Rojo",
            )
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(ZodiacSign.leo, state.selectedSign)
        assertEquals(ZodiacSign.leo, state.horoscope?.sign)
        assertNull(state.errorMessage)
    }

    @Test
    fun initDoesNotPullIfAlreadyPulledToday_evenIfSyncWouldFail() = runTest {
        val dispatcher = StandardTestDispatcher(testScheduler)

        val repo = FakeRepo()
        val observeUseCase = ObserveDailyHoroscopeUseCase(repo)
        val getUseCase = GetDailyHoroscopeUseCase(repo)
        val observeUserProfileUseCase = ObserveUserProfileUseCase(FakeUserProfileRepo())

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
            observeUserProfileUseCase = observeUserProfileUseCase,
            dispatcher = dispatcher,
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        // Si no se llamó pull(), no debe haber error aunque el sync sea failing
        assertNull(state.errorMessage)
    }

    private class FakeRepo : HoroscopeRepository {
        private val state = MutableStateFlow<DailyHoroscope?>(null)

        fun emit(value: DailyHoroscope?) {
            state.value = value
        }

        override fun observeDaily(dateIso: String, sign: ZodiacSign): Flow<DailyHoroscope?> {
            // Simplificación: ignoramos filtros (date/sign) y emitimos lo que toque.
            return state.asStateFlow()
        }

        override suspend fun getDaily(dateIso: String, sign: ZodiacSign): DailyHoroscope? {
            return state.value
        }
    }

    private class FakeSync : HoroscopeDailySyncController {
        override suspend fun pull(dateIso: String) {
            // NO-OP
        }
    }

    private class FailingSync : HoroscopeDailySyncController {
        override suspend fun pull(dateIso: String) {
            throw RuntimeException("boom")
        }
    }

    private class FakePullMarker(
        private var lastPulledDateIso: String?
    ) : HoroscopePullMarker {

        override fun getLastPulledDateIso(): String? = lastPulledDateIso

        override fun setLastPulledDateIso(dateIso: String) {
            lastPulledDateIso = dateIso
        }
    }

    private class FakeUserProfileRepo : UserProfileRepository {
        override fun observeUserProfile(): Flow<UserProfile?> = MutableStateFlow(null)

        override suspend fun getUserProfile(): UserProfile? = null

        override suspend fun saveUserProfile(profile: UserProfile) = Unit
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
