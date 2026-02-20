package com.agc.bwitch

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.agc.bwitch.presentation.auth.SessionViewModel
import com.agc.bwitch.presentation.navigation.Destination
import com.agc.bwitch.presentation.navigation.Navigator
import com.agc.bwitch.ui.astrology.AstrologyScreen
import com.agc.bwitch.ui.astrology.BirthChartScreen
import com.agc.bwitch.ui.astrology.HoroscopeScreen
import com.agc.bwitch.ui.auth.AuthScreen
import com.agc.bwitch.ui.common.AppScaffold
import com.agc.bwitch.ui.portal.PortalScreen
import org.koin.compose.koinInject

@Composable
fun AppRoot() {
    val navigator: Navigator = koinInject()
    val destination by navigator.current.collectAsState()
    val dest = destination

    val sessionVm: SessionViewModel = koinInject()
    val session by sessionVm.uiState.collectAsState()

    when {
        session.isLoading -> {
            // Splash simple mientras Firebase emite el primer authState
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return
        }

        !session.isLoggedIn -> {
            AuthScreen()
            return
        }
    }

    AppScaffold(
        title = dest.title,
        canGoBack = navigator.canGoBack(),
        onBack = { navigator.goBack() }
    ) { padding ->
        when (dest) {
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
                preselectedSign = dest.preselectedSign
            )
        }
    }
}
