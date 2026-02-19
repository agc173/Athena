package com.agc.bwitch

import androidx.compose.runtime.*
import org.koin.compose.koinInject
import com.agc.bwitch.presentation.navigation.Destination
import com.agc.bwitch.presentation.navigation.Navigator
import com.agc.bwitch.ui.portal.PortalScreen
import com.agc.bwitch.ui.astrology.HoroscopeScreen

@Composable
fun AppRoot() {
    val navigator: Navigator = koinInject()
    val destination by navigator.current.collectAsState()

    when (destination) {
        Destination.Portal -> PortalScreen(
            onOpenDailyHoroscope = { navigator.navigate(Destination.HoroscopeDaily) }
        )

        Destination.HoroscopeDaily -> HoroscopeScreen(
            onBack = { navigator.goBack() }
        )
    }
}
