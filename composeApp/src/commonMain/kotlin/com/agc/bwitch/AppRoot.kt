package com.agc.bwitch

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.agc.bwitch.domain.astrology.birthchart.BirthChartRepository
import com.agc.bwitch.presentation.auth.SessionViewModel
import com.agc.bwitch.presentation.navigation.Destination
import com.agc.bwitch.presentation.navigation.Navigator
import com.agc.bwitch.ui.astrology.AstrologyScreen
import com.agc.bwitch.ui.astrology.BirthChartScreen
import com.agc.bwitch.ui.astrology.HoroscopeScreen
import com.agc.bwitch.ui.auth.AuthScreen
import com.agc.bwitch.ui.common.AppScaffold
import com.agc.bwitch.ui.portal.PortalScreen
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun AppRoot() {
    val navigator: Navigator = koinInject()
    val dest by navigator.current.collectAsState()

    val sessionVm: SessionViewModel = koinInject()
    val session by sessionVm.uiState.collectAsState()

    // Repo para “warm up” sync post-login (BirthChart hoy, UserProfile luego)
    val birthChartRepository: BirthChartRepository = koinInject()

    // 1) Splash mientras Firebase emite el primer authState
    if (session.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val isAuthenticated = session.isLoggedIn && !session.isAnonymous

    // 2) Mantener el root coherente con auth state
    LaunchedEffect(isAuthenticated) {
        if (isAuthenticated) {
            // Si ya estás en AuthGate/Login, salta al Portal
            if (dest == Destination.AuthGate) {
                navigator.resetToRoot(Destination.Portal)
            }
        } else {
            // Si pierdes sesión, vuelves al gate
            navigator.resetToRoot(Destination.AuthGate)
        }
    }

    // 3) Hook post-login: dispara pulls una vez por UID
    LaunchedEffect(session.uid) {
        val uid = session.uid ?: return@LaunchedEffect
        if (!isAuthenticated) return@LaunchedEffect

        // Opción B (sin tocar repos): esto ya dispara pull en background en tu SyncBirthChartRepository actual
        runCatching { birthChartRepository.getBirthData() }

        // Opción A (recomendada si expones pull()):
        // (birthChartRepository as? SyncBirthChartRepository)?.pull()
        // y luego replicamos igual con UserProfileRepository
    }

    AppScaffold(
        title = dest.title,
        canGoBack = navigator.canGoBack(),
        onBack = { navigator.goBack() }
    ) { padding ->
        when (dest) {
            Destination.AuthGate -> {
                // Gate simple: si no hay sesión, AuthScreen. Si hay sesión, LaunchedEffect ya te manda a Portal.
                AuthScreen()
            }

            Destination.Portal -> PortalScreen(
                contentPadding = padding,
                onNavigate = { navigator.navigate(it) }
            )

            Destination.Astrology -> AstrologyScreen(
                contentPadding = padding,
                onNavigate = { navigator.navigate(it) }
            )

            Destination.BirthChart -> BirthChartScreen(contentPadding = padding)

            is Destination.HoroscopeDaily -> HoroscopeScreen(
                contentPadding = padding,
                preselectedSign = (dest as Destination.HoroscopeDaily).preselectedSign
            )

            // Cuando lo tengas, lo enchufamos aquí:
            // Destination.UserProfile -> UserProfileScreen(contentPadding = padding, onBack = { navigator.goBack() })
        }
    }
}