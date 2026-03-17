package com.agc.bwitch

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.agc.bwitch.domain.astrology.birthchart.BirthChartRepository
import com.agc.bwitch.domain.userprofile.GetUserProfileUseCase
import com.agc.bwitch.domain.userprofile.ObserveUserProfileUseCase
import com.agc.bwitch.domain.userprofile.UserProfile
import com.agc.bwitch.domain.userprofile.hasMinimumProfileCompleted
import com.agc.bwitch.presentation.auth.SessionViewModel
import com.agc.bwitch.presentation.navigation.Destination
import com.agc.bwitch.presentation.navigation.Navigator
import com.agc.bwitch.ui.astrology.AstrologyScreen
import com.agc.bwitch.ui.astrology.BirthChartScreen
import com.agc.bwitch.ui.astrology.HoroscopeScreen
import com.agc.bwitch.ui.auth.AuthScreen
import com.agc.bwitch.ui.common.AppScaffold
import com.agc.bwitch.ui.guide.GuideHomeScreen
import com.agc.bwitch.ui.guide.PendulumScreen
import com.agc.bwitch.ui.onboarding.OnboardingProfileScreen
import com.agc.bwitch.ui.oracle.OracleDebugScreen
import com.agc.bwitch.ui.oracle.OracleScreen
import com.agc.bwitch.ui.portal.PortalScreen
import com.agc.bwitch.ui.tarot.TarotHomeScreen
import com.agc.bwitch.ui.tarot.TarotScreen
import com.agc.bwitch.ui.userprofile.UserProfileScreen
import org.koin.compose.koinInject

@Composable
fun AppRoot() {
    val navigator: Navigator = koinInject()
    val dest by navigator.current.collectAsState()

    val sessionVm: SessionViewModel = koinInject()
    val session by sessionVm.uiState.collectAsState()

    val birthChartRepository: BirthChartRepository = koinInject()
    val getUserProfile: GetUserProfileUseCase = koinInject()
    val observeUserProfile: ObserveUserProfileUseCase = koinInject()

    var profileForGate by remember { mutableStateOf<UserProfile?>(null) }
    var hasProfileGateSnapshot by remember { mutableStateOf(false) }
    var isProfileGateLoading by remember { mutableStateOf(false) }

    if (session.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val isAuthenticated = session.isLoggedIn && !session.isAnonymous

    LaunchedEffect(isAuthenticated, session.uid) {
        if (!isAuthenticated || session.uid.isNullOrBlank()) {
            profileForGate = null
            hasProfileGateSnapshot = false
            isProfileGateLoading = false
            return@LaunchedEffect
        }

        isProfileGateLoading = true

        val initialProfile = runCatching { getUserProfile() }.getOrNull()
        profileForGate = initialProfile
        hasProfileGateSnapshot = true
        isProfileGateLoading = false

        observeUserProfile().collect { profile ->
            profileForGate = profile
        }
    }

    val hasMinimumProfile = profileForGate.hasMinimumProfileCompleted()

    LaunchedEffect(isAuthenticated, isProfileGateLoading, hasProfileGateSnapshot, hasMinimumProfile, dest) {
        if (!isAuthenticated) {
            if (dest != Destination.AuthGate) navigator.resetToRoot(Destination.AuthGate)
            return@LaunchedEffect
        }

        if (isProfileGateLoading || !hasProfileGateSnapshot) return@LaunchedEffect

        if (!hasMinimumProfile) {
            if (dest != Destination.OnboardingProfile) {
                navigator.resetToRoot(Destination.OnboardingProfile)
            }
            return@LaunchedEffect
        }

        if (dest == Destination.AuthGate || dest == Destination.OnboardingProfile) {
            navigator.resetToRoot(Destination.Portal)
        }
    }

    LaunchedEffect(session.uid) {
        if (session.uid == null || !isAuthenticated) return@LaunchedEffect

        runCatching { birthChartRepository.getBirthData() }
    }

    AppScaffold(
        title = dest.title,
        canGoBack = navigator.canGoBack(),
        onBack = { navigator.goBack() }
    ) { padding ->
        when (dest) {
            Destination.AuthGate -> AuthScreen()

            Destination.OnboardingProfile -> OnboardingProfileScreen(contentPadding = padding)

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

            Destination.Pendulum -> PendulumScreen(contentPadding = padding)
        }
    }
}
