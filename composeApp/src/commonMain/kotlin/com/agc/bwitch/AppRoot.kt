package com.agc.bwitch

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import com.agc.bwitch.ui.guide.GuideHomeScreen
import com.agc.bwitch.ui.guide.PendulumPlaceholderScreen
import com.agc.bwitch.ui.oracle.OracleDebugScreen
import com.agc.bwitch.ui.oracle.OracleScreen
import com.agc.bwitch.ui.portal.PortalScreen
import com.agc.bwitch.ui.tarot.TarotHomeScreen
import com.agc.bwitch.ui.tarot.TarotScreen
import com.agc.bwitch.ui.userprofile.UserProfileScreen
import org.koin.compose.koinInject
import com.agc.bwitch.domain.userprofile.GetUserProfileUseCase

@Composable
fun AppRoot() {
    val navigator: Navigator = koinInject()
    val dest by navigator.current.collectAsState()

    val sessionVm: SessionViewModel = koinInject()
    val session by sessionVm.uiState.collectAsState()

    // Repo para “warm up” sync post-login
    val birthChartRepository: BirthChartRepository = koinInject()

    val getUserProfile: GetUserProfileUseCase = koinInject()

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
            if (dest == Destination.AuthGate) {
                navigator.resetToRoot(Destination.Portal)
            }
        } else {
            navigator.resetToRoot(Destination.AuthGate)
        }
    }

    // 3) Hook post-login: dispara pulls una vez por UID
    LaunchedEffect(session.uid) {
        val uid = session.uid ?: return@LaunchedEffect
        if (!isAuthenticated) return@LaunchedEffect

        runCatching { birthChartRepository.getBirthData() }
        runCatching { getUserProfile() }
    }

    AppScaffold(
        title = dest.title,
        canGoBack = navigator.canGoBack(),
        onBack = { navigator.goBack() }
    ) { padding ->
        when (dest) {
            Destination.AuthGate -> AuthScreen()

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

            Destination.UserProfile -> UserProfileScreen(
                contentPadding = padding,
                onBack = { navigator.goBack() }
            )

            Destination.Guide -> GuideHomeScreen(
                contentPadding = padding,
                onNavigate = { navigator.navigate(it) }
            )

            Destination.TarotHome -> TarotHomeScreen(
                contentPadding = padding,
                onSelectRequestType = { requestType ->
                    navigator.navigate(Destination.Tarot(requestType = requestType))
                }
            )

            Destination.Oracle -> OracleScreen(contentPadding = padding)

            Destination.OracleDebug -> OracleDebugScreen(contentPadding = padding)

            is Destination.Tarot -> TarotScreen(
                contentPadding = padding,
                initialRequestType = (dest as Destination.Tarot).requestType,
            )

            Destination.Pendulum -> PendulumPlaceholderScreen(contentPadding = padding)
        }
    }
}