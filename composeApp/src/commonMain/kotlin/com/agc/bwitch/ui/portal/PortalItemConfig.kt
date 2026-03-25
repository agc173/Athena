package com.agc.bwitch.ui.portal

import com.agc.bwitch.presentation.navigation.Destination

data class PortalItemConfig(
    val title: String,
    val subtitle: String,
    val ornament: PortalOrnament,
    val destination: Destination?,
    val enabled: Boolean = true,
)

enum class PortalOrnament {
    PROFILE,
    ASTROLOGY,
    GUIDE,
    COMMUNITY,
    STORE,
    RITUALS,
}
