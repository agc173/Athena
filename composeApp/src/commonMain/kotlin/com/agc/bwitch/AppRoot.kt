package com.agc.bwitch

import androidx.compose.runtime.*
import org.koin.compose.koinInject
import com.agc.bwitch.presentation.navigation.Destination
import com.agc.bwitch.presentation.navigation.Navigator
import com.agc.bwitch.ui.common.AppScaffold
import com.agc.bwitch.ui.portal.PortalScreen
import com.agc.bwitch.ui.astrology.HoroscopeScreen
import com.agc.bwitch.ui.astrology.AstrologyScreen


@Composable
fun AppRoot() {
    val navigator: Navigator = koinInject()
    val destination by navigator.current.collectAsState()
    val dest = destination

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


            is Destination.HoroscopeDaily -> HoroscopeScreen(
                contentPadding = padding,
                preselectedSign = dest.preselectedSign
            )
        }
    }

}

