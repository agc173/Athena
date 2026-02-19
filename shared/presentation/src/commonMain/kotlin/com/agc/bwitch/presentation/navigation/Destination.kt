package com.agc.bwitch.presentation.navigation

sealed class Destination {
    data object Portal : Destination()
    data object HoroscopeDaily : Destination()
}
