package com.agc.bwitch.ui.portal

import com.agc.bwitch.presentation.navigation.Destination

data class PortalItemConfig(
    val title: String,
    val subtitle: String,
    val symbol: String,
    val destination: Destination?,
    val enabled: Boolean = true,
)
