package com.agc.bwitch

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.Canvas
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import com.agc.bwitch.ui.astrology.SynastryScreen
import com.agc.bwitch.ui.auth.AuthScreen
import com.agc.bwitch.ui.common.AppScaffold
import com.agc.bwitch.ui.guide.GuideHomeScreen
import com.agc.bwitch.ui.guide.PendulumScreen
import com.agc.bwitch.ui.onboarding.OnboardingProfileScreen
import com.agc.bwitch.ui.oracle.OracleDebugScreen
import com.agc.bwitch.ui.oracle.OracleScreen
import com.agc.bwitch.ui.rituals.RitualsPlaceholderScreen
import com.agc.bwitch.ui.tarot.TarotHomeScreen
import com.agc.bwitch.ui.tarot.TarotScreen
import com.agc.bwitch.ui.userprofile.ProfileScreen
import com.agc.bwitch.ui.userprofile.SettingsScreen
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
            navigator.resetToRoot(Destination.UserProfile)
        }
    }

    LaunchedEffect(session.uid) {
        if (session.uid == null || !isAuthenticated) return@LaunchedEffect

        runCatching { birthChartRepository.getBirthEssence() }
    }

    val currentMainTab = remember(dest) { MainTab.items.firstOrNull { it.matches(dest) } }

    AppScaffold(
        title = dest.title,
        canGoBack = navigator.canGoBack(),
        onBack = { navigator.goBack() },
        bottomBar = {
            if (currentMainTab != null) {
                MainBottomBar(
                    selectedTab = currentMainTab,
                    onTabSelected = { selected ->
                        if (selected == currentMainTab) {
                            if (!navigator.isAtRootOf(selected.rootDestination)) {
                                navigator.popToRoot()
                            }
                        } else {
                            navigator.switchTopLevel(selected.rootDestination)
                        }
                    }
                )
            }
        }
    ) { padding ->
        when (dest) {
            Destination.AuthGate -> AuthScreen()

            Destination.OnboardingProfile -> OnboardingProfileScreen(contentPadding = padding)

            Destination.Astrology -> AstrologyScreen(
                contentPadding = padding,
                onNavigate = { navigator.navigate(it) }
            )

            Destination.BirthChart -> BirthChartScreen(contentPadding = padding)

            Destination.Synastry -> SynastryScreen(contentPadding = padding)

            is Destination.HoroscopeDaily -> HoroscopeScreen(
                contentPadding = padding,
                preselectedSign = (dest as Destination.HoroscopeDaily).preselectedSign
            )

            Destination.UserProfile -> ProfileScreen(
                contentPadding = padding,
                onOpenSettings = { navigator.navigate(Destination.Settings) }
            )

            Destination.Settings -> SettingsScreen(contentPadding = padding)

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

            Destination.Rituals -> RitualsPlaceholderScreen(contentPadding = padding)
        }
    }
}

private data class MainTab(
    val label: String,
    val rootDestination: Destination,
    val matches: (Destination) -> Boolean,
) {
    companion object {
        val profile = MainTab(
            label = "Perfil",
            rootDestination = Destination.UserProfile,
            matches = { destination ->
                destination == Destination.UserProfile || destination == Destination.Settings
            },
        )
        val astrology = MainTab(
            label = "Astro",
            rootDestination = Destination.Astrology,
            matches = { destination ->
                destination == Destination.Astrology ||
                    destination == Destination.BirthChart ||
                    destination == Destination.Synastry ||
                    destination is Destination.HoroscopeDaily
            },
        )
        val guide = MainTab(
            label = "Guía",
            rootDestination = Destination.Guide,
            matches = { destination ->
                destination == Destination.Guide ||
                    destination == Destination.TarotHome ||
                    destination == Destination.Oracle ||
                    destination == Destination.OracleDebug ||
                    destination == Destination.Pendulum ||
                    destination is Destination.Tarot
            },
        )
        val rituals = MainTab(
            label = "Rituales",
            rootDestination = Destination.Rituals,
            matches = { destination -> destination == Destination.Rituals },
        )

        val items = listOf(profile, astrology, guide, rituals)
    }
}

@Composable
private fun MainBottomBar(
    selectedTab: MainTab,
    onTabSelected: (MainTab) -> Unit,
) {
    val background = Color(0xFFFFFFFF)
    val activeColor = Color(0xFF6FAFC7)
    val inactiveColor = Color(0xFFAFA4B5)
    val itemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = activeColor,
        unselectedIconColor = inactiveColor,
        selectedTextColor = activeColor,
        unselectedTextColor = inactiveColor,
        indicatorColor = Color.Transparent,
    )

    Surface(
        color = background,
        shadowElevation = 4.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        NavigationBar(
            containerColor = background,
            tonalElevation = 0.dp,
            windowInsets = NavigationBarDefaults.windowInsets,
            modifier = Modifier.padding(vertical = 4.dp),
        ) {
            MainTab.items.forEach { tab ->
                val isSelected = tab == selectedTab
                val tint = if (isSelected) activeColor else inactiveColor

                NavigationBarItem(
                    selected = isSelected,
                    onClick = { onTabSelected(tab) },
                    colors = itemColors,
                    icon = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSelected) {
                                    Canvas(modifier = Modifier.size(34.dp)) {
                                        drawCircle(
                                            brush = Brush.radialGradient(
                                                colors = listOf(
                                                    activeColor.copy(alpha = 0.16f),
                                                    activeColor.copy(alpha = 0.04f),
                                                    Color.Transparent,
                                                ),
                                                center = center,
                                                radius = size.minDimension * 0.52f,
                                            ),
                                            radius = size.minDimension * 0.5f,
                                            center = center,
                                        )
                                    }
                                }
                                BottomTabIcon(
                                    tab = tab,
                                    tint = tint,
                                    cutoutColor = background,
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                        }
                    },
                    label = {
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                            ),
                        )
                    },
                )
            }
        }
    }
}

@Composable
private fun BottomTabIcon(
    tab: MainTab,
    tint: Color,
    cutoutColor: Color,
) {
    when (tab) {
        MainTab.profile -> ProfileIcon(tint = tint)
        MainTab.astrology -> AstrologyIcon(tint = tint, cutoutColor = cutoutColor)
        MainTab.guide -> GuideIcon(tint = tint)
        MainTab.rituals -> RitualsIcon(tint = tint)
        else -> ProfileIcon(tint = tint)
    }
}

@Composable
private fun ProfileIcon(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(19.dp)) {
        val stroke = Stroke(width = size.minDimension * 0.1f, cap = StrokeCap.Round)
        drawCircle(
            color = tint,
            radius = size.minDimension * 0.22f,
            center = Offset(size.width / 2f, size.height * 0.45f),
            style = stroke,
        )
        drawCircle(
            color = tint,
            radius = size.minDimension * 0.12f,
            center = Offset(size.width / 2f, size.height * 0.13f),
            style = stroke,
        )
        drawArc(
            color = tint,
            startAngle = 205f,
            sweepAngle = 130f,
            useCenter = false,
            topLeft = Offset(size.width * 0.2f, size.height * 0.5f),
            size = Size(size.width * 0.6f, size.height * 0.35f),
            style = stroke,
        )
    }
}

@Composable
private fun AstrologyIcon(
    tint: Color,
    cutoutColor: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(19.dp)) {
        val stroke = Stroke(width = size.minDimension * 0.1f, cap = StrokeCap.Round)
        drawArc(
            color = tint,
            startAngle = 65f,
            sweepAngle = 230f,
            useCenter = false,
            topLeft = Offset(size.width * 0.18f, size.height * 0.15f),
            size = Size(size.width * 0.55f, size.height * 0.7f),
            style = stroke,
        )
        drawCircle(
            color = cutoutColor,
            radius = size.minDimension * 0.2f,
            center = Offset(size.width * 0.55f, size.height * 0.48f),
        )
        drawCircle(
            color = tint,
            radius = size.minDimension * 0.07f,
            center = Offset(size.width * 0.78f, size.height * 0.28f),
        )
    }
}

@Composable
private fun GuideIcon(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(19.dp)) {
        val stroke = Stroke(width = size.minDimension * 0.1f, cap = StrokeCap.Round)
        val eyePath = Path().apply {
            moveTo(size.width * 0.15f, size.height * 0.5f)
            quadraticBezierTo(size.width * 0.5f, size.height * 0.15f, size.width * 0.85f, size.height * 0.5f)
            quadraticBezierTo(size.width * 0.5f, size.height * 0.85f, size.width * 0.15f, size.height * 0.5f)
            close()
        }
        drawPath(path = eyePath, color = tint, style = stroke)
        drawCircle(
            color = tint,
            radius = size.minDimension * 0.11f,
            center = Offset(size.width * 0.5f, size.height * 0.5f),
        )
    }
}

@Composable
private fun RitualsIcon(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(19.dp)) {
        val stroke = Stroke(width = size.minDimension * 0.1f, cap = StrokeCap.Round)
        drawRoundRect(
            color = tint,
            topLeft = Offset(size.width * 0.32f, size.height * 0.44f),
            size = Size(size.width * 0.36f, size.height * 0.36f),
            style = stroke,
        )
        val flamePath = Path().apply {
            moveTo(size.width * 0.5f, size.height * 0.18f)
            quadraticBezierTo(size.width * 0.66f, size.height * 0.34f, size.width * 0.5f, size.height * 0.44f)
            quadraticBezierTo(size.width * 0.34f, size.height * 0.34f, size.width * 0.5f, size.height * 0.18f)
            close()
        }
        drawPath(path = flamePath, color = tint, style = stroke)
        drawLine(
            color = tint,
            start = Offset(size.width * 0.5f, size.height * 0.44f),
            end = Offset(size.width * 0.5f, size.height * 0.38f),
            strokeWidth = size.minDimension * 0.08f,
            cap = StrokeCap.Round,
        )
    }
}
