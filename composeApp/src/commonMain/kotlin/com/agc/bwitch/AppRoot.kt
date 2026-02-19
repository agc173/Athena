package com.agc.bwitch

import androidx.compose.runtime.*
import org.koin.compose.koinInject
import com.agc.bwitch.presentation.navigation.Destination
import com.agc.bwitch.presentation.navigation.Navigator
import com.agc.bwitch.ui.common.AppScaffold
import com.agc.bwitch.ui.portal.PortalScreen
import com.agc.bwitch.ui.astrology.HoroscopeScreen
import com.agc.bwitch.ui.astrology.AstrologyScreen
import com.agc.bwitch.ui.astrology.BirthChartScreen
import com.agc.bwitch.presentation.auth.SessionViewModel
import com.agc.bwitch.ui.auth.AuthScreen




@Composable
fun AppRoot() {
    val navigator: Navigator = koinInject()
    val destination by navigator.current.collectAsState()
    val dest = destination
    val sessionVm: SessionViewModel = koinInject()
    val session by sessionVm.uiState.collectAsState()

    if (!session.isLoggedIn) {
        AuthScreen()
        return
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

