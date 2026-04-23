package com.agc.bwitch

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.agc.bwitch.domain.astrology.birthchart.BirthChartRepository
import com.agc.bwitch.domain.userprofile.GetUserProfileUseCase
import com.agc.bwitch.domain.userprofile.ObserveUserProfileUseCase
import com.agc.bwitch.domain.userprofile.UserProfile
import com.agc.bwitch.domain.userprofile.hasMinimumProfileCompleted
import com.agc.bwitch.localization.NavigationStrings
import com.agc.bwitch.localization.appStrings
import com.agc.bwitch.presentation.auth.SessionViewModel
import com.agc.bwitch.presentation.economy.EconomyViewModel
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
import com.agc.bwitch.ui.rituals.DailyRitualScreen
import com.agc.bwitch.ui.rituals.HabitsScreen
import com.agc.bwitch.ui.rituals.RitualDetailScreen
import com.agc.bwitch.ui.rituals.RitualsCategoryScreen
import com.agc.bwitch.ui.rituals.RitualsListScreen
import com.agc.bwitch.ui.rituals.RitualsPlaceholderScreen
import com.agc.bwitch.ui.store.MoonStoreScreen
import com.agc.bwitch.ui.store.MoonPaywallDialog
import com.agc.bwitch.ui.tarot.TarotHomeScreen
import com.agc.bwitch.ui.tarot.TarotScreen
import com.agc.bwitch.ui.userprofile.ProfileScreen
import com.agc.bwitch.ui.userprofile.SettingsScreen
import com.agc.bwitch.ui.theme.BWitchThemeTokens
import org.koin.compose.koinInject

@Composable
fun AppRoot() {
    val navigator: Navigator = koinInject()
    val dest by navigator.current.collectAsState()

    val sessionVm: SessionViewModel = koinInject()
    val session by sessionVm.uiState.collectAsState()
    val economyVm: EconomyViewModel = koinInject()
    val economyState by economyVm.uiState.collectAsState()
    val moonPaywallRequest by economyVm.moonPaywallRequest.collectAsState()

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
    val navigationStrings = appStrings.navigation

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
        title = destinationTitle(dest, navigationStrings),
        canGoBack = navigator.canGoBack(),
        onBack = { navigator.goBack() },
        actions = {
            if (dest == Destination.UserProfile) {
                TopBarSettingsAction(
                    onClick = { navigator.navigate(Destination.Settings) },
                )
            }
        },
        bottomBar = {
            if (currentMainTab != null) {
                MainBottomBar(
                    selectedTab = currentMainTab,
                    navigationStrings = navigationStrings,
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
                onEditProfile = { navigator.navigate(Destination.Settings) },
                onDiscoverEssence = { navigator.navigate(Destination.Astrology) },
                onOpenHabits = { navigator.navigate(Destination.Habits) },
                onOpenStore = { navigator.navigate(Destination.MoonStore) },
            )

            Destination.MoonStore -> MoonStoreScreen(contentPadding = padding)

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

            Destination.Rituals -> RitualsPlaceholderScreen(
                contentPadding = padding,
                onOpenDailyRitual = { navigator.navigate(Destination.DailyRitual) },
                onOpenRitualsCategories = { navigator.navigate(Destination.RitualsCategories) },
                onOpenHabits = { navigator.navigate(Destination.Habits) },
            )

            Destination.RitualsCategories -> RitualsCategoryScreen(
                contentPadding = padding,
                onOpenCategory = { category -> navigator.navigate(Destination.RitualsList(category)) },
            )

            is Destination.RitualsList -> RitualsListScreen(
                category = (dest as Destination.RitualsList).category,
                contentPadding = padding,
                onOpenRitual = { ritualId -> navigator.navigate(Destination.RitualDetail(ritualId)) },
            )

            is Destination.RitualDetail -> RitualDetailScreen(
                ritualId = (dest as Destination.RitualDetail).ritualId,
                contentPadding = padding,
            )

            Destination.DailyRitual -> DailyRitualScreen(
                contentPadding = padding,
                onBack = { navigator.goBack() },
            )

            Destination.Habits -> HabitsScreen(contentPadding = padding)
        }

        moonPaywallRequest?.let { paywall ->
            MoonPaywallDialog(
                economyState = economyState,
                requiredMoons = paywall.requiredMoons,
                onDismiss = economyVm::dismissMoonPaywall,
                onClaimDaily = economyVm::claimDailyLogin,
                onClaimRewardedAd = {
                    economyVm.claimRewardedAd(
                        placement = paywall.source ?: REWARDED_AD_PAYWALL_PLACEMENT,
                    )
                },
                onOpenStore = {
                    economyVm.dismissMoonPaywall()
                    navigator.navigate(Destination.MoonStore)
                },
            )
        }
    }
}

private const val REWARDED_AD_PAYWALL_PLACEMENT = "contextual_paywall"

private fun destinationTitle(
    destination: Destination,
    strings: NavigationStrings,
): String {
    return when (destination) {
        Destination.AuthGate -> ""
        Destination.OnboardingProfile -> strings.onboardingProfile
        Destination.Astrology -> strings.astrology
        Destination.BirthChart -> strings.birthChart
        Destination.Synastry -> strings.synastry
        Destination.UserProfile -> strings.profile
        Destination.MoonStore -> strings.profile
        Destination.Settings -> strings.settings
        Destination.Oracle -> strings.oracle
        Destination.OracleDebug -> strings.oracleDebug
        Destination.Guide -> strings.guide
        Destination.Rituals -> strings.rituals
        Destination.RitualsCategories -> strings.rituals
        is Destination.RitualsList -> strings.rituals
        is Destination.RitualDetail -> strings.ritual
        Destination.DailyRitual -> strings.dailyRitual
        Destination.Habits -> strings.habits
        Destination.TarotHome -> strings.tarot
        is Destination.Tarot -> strings.tarot
        Destination.Pendulum -> strings.pendulum
        is Destination.HoroscopeDaily -> strings.horoscopeDaily
    }
}

@Composable
private fun TopBarSettingsAction(onClick: () -> Unit) {
    val extras = BWitchThemeTokens.extras

    IconButton(onClick = onClick) {
        GearIcon(
            tint = extras.topBarIconColor,
            modifier = Modifier.size(20.dp),
        )
    }
}

@Composable
private fun GearIcon(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val outerRadius = size.minDimension * 0.42f
        val innerRadius = size.minDimension * 0.23f
        val stroke = size.minDimension * 0.09f

        for (i in 0 until 8) {
            val angle = (i * 45f) * (kotlin.math.PI.toFloat() / 180f)
            val cosAngle = kotlin.math.cos(angle)
            val sinAngle = kotlin.math.sin(angle)
            drawLine(
                color = tint,
                start = Offset(
                    center.x + cosAngle * (innerRadius + stroke),
                    center.y + sinAngle * (innerRadius + stroke),
                ),
                end = Offset(
                    center.x + cosAngle * outerRadius,
                    center.y + sinAngle * outerRadius,
                ),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }

        drawCircle(
            color = tint,
            radius = innerRadius,
            style = Stroke(width = stroke),
        )
    }
}

private data class MainTab(
    val rootDestination: Destination,
    val matches: (Destination) -> Boolean,
) {
    companion object {
        val profile = MainTab(
            rootDestination = Destination.UserProfile,
            matches = { destination ->
                destination == Destination.UserProfile || destination == Destination.Settings
                    || destination == Destination.MoonStore
            },
        )
        val astrology = MainTab(
            rootDestination = Destination.Astrology,
            matches = { destination ->
                destination == Destination.Astrology ||
                        destination == Destination.BirthChart ||
                        destination == Destination.Synastry ||
                        destination is Destination.HoroscopeDaily
            },
        )
        val guide = MainTab(
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
            rootDestination = Destination.Rituals,
            matches = { destination ->
                destination == Destination.Rituals ||
                    destination == Destination.Habits ||
                    destination == Destination.DailyRitual ||
                    destination == Destination.RitualsCategories ||
                    destination is Destination.RitualsList ||
                    destination is Destination.RitualDetail
            },
        )

        val items = listOf(profile, astrology, guide, rituals)
    }
}

@Composable
private fun MainBottomBar(
    selectedTab: MainTab,
    navigationStrings: NavigationStrings,
    onTabSelected: (MainTab) -> Unit,
) {
    val themeExtras = BWitchThemeTokens.extras
    val background = Color(0xFFFFFFFF)
    val activeColor = Color(0xFF6FAFC7)
    val inactiveColor = Color(0xFFAFA4B5)

    Surface(
        color = background,
        shadowElevation = 0.dp,
        tonalElevation = 0.dp,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(
                modifier = Modifier.fillMaxWidth(),
                thickness = 1.dp,
                color = themeExtras.navBarBorder,
            )
            Row(
                modifier = Modifier
                    .windowInsetsPadding(WindowInsets(bottom = 0.dp))
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                MainTab.items.forEach { tab ->
                    val isSelected = tab == selectedTab
                    val tint = if (isSelected) activeColor else inactiveColor

                    Column(
                        modifier = Modifier
                            .height(54.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onTabSelected(tab) },
                            )
                            .padding(horizontal = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(
                                    color = if (isSelected) activeColor.copy(alpha = 0.14f) else Color.Transparent,
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            BottomTabIcon(
                                tab = tab,
                                tint = tint,
                            )
                        }

                        Spacer(modifier = Modifier.height(1.dp))

                        Text(
                            text = tabLabel(tab, navigationStrings),
                            textAlign = TextAlign.Center,
                            style = MaterialTheme.typography.labelMedium.copy(
                                fontSize = 16.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                lineHeight = 19.sp,
                            ),
                            color = tint,
                        )
                    }
                }
            }
        }
    }
}

private fun tabLabel(
    tab: MainTab,
    strings: NavigationStrings,
): String = when (tab) {
    MainTab.profile -> strings.profile
    MainTab.astrology -> strings.astrology
    MainTab.guide -> strings.guide
    MainTab.rituals -> strings.rituals
    else -> strings.profile
}

@Composable
private fun BottomTabIcon(
    tab: MainTab,
    tint: Color,
) {
    when (tab) {
        MainTab.profile -> ProfileIcon(tint = tint)
        MainTab.astrology -> AstrologyIcon(tint = tint)
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
    Canvas(modifier = modifier.size(25.dp)) {
        val stroke = Stroke(width = size.minDimension * 0.09f, cap = StrokeCap.Round)
        drawCircle(
            color = tint,
            radius = size.minDimension * 0.24f,
            center = Offset(size.width / 2f, size.height * 0.62f),
            style = stroke,
        )
        drawCircle(
            color = tint,
            radius = size.minDimension * 0.105f,
            center = Offset(size.width / 2f, size.height * 0.24f),
            style = stroke,
        )
        drawArc(
            color = tint,
            startAngle = 206f,
            sweepAngle = 128f,
            useCenter = false,
            topLeft = Offset(size.width * 0.23f, size.height * 0.49f),
            size = Size(size.width * 0.54f, size.height * 0.33f),
            style = stroke,
        )
    }
}

@Composable
private fun AstrologyIcon(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(25.dp)) {
        val moonStroke = size.minDimension * 0.075f
        drawArc(
            color = tint,
            startAngle = 50f,
            sweepAngle = 220f,
            useCenter = false,
            topLeft = Offset(size.width * 0.14f, size.height * 0.13f),
            size = Size(size.width * 0.62f, size.height * 0.74f),
            style = Stroke(width = moonStroke, cap = StrokeCap.Round),
        )
        drawArc(
            color = tint,
            startAngle = 70f,
            sweepAngle = 190f,
            useCenter = false,
            topLeft = Offset(size.width * 0.31f, size.height * 0.11f),
            size = Size(size.width * 0.52f, size.height * 0.7f),
            style = Stroke(width = moonStroke, cap = StrokeCap.Round),
        )

        drawCircle(
            color = tint,
            radius = size.minDimension * 0.045f,
            center = Offset(size.width * 0.76f, size.height * 0.28f),
        )
        drawLine(
            color = tint,
            start = Offset(size.width * 0.76f, size.height * 0.2f),
            end = Offset(size.width * 0.76f, size.height * 0.36f),
            strokeWidth = size.minDimension * 0.07f,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = tint,
            start = Offset(size.width * 0.68f, size.height * 0.28f),
            end = Offset(size.width * 0.84f, size.height * 0.28f),
            strokeWidth = size.minDimension * 0.07f,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun GuideIcon(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(25.dp)) {
        val stroke = Stroke(width = size.minDimension * 0.085f, cap = StrokeCap.Round)
        val eyePath = Path().apply {
            moveTo(size.width * 0.16f, size.height * 0.72f)
            quadraticBezierTo(
                size.width * 0.5f,
                size.height * 0.45f,
                size.width * 0.84f,
                size.height * 0.72f,
            )
            quadraticBezierTo(
                size.width * 0.5f,
                size.height * 0.99f,
                size.width * 0.16f,
                size.height * 0.72f,
            )
            close()
        }
        drawPath(path = eyePath, color = tint, style = stroke)
        drawCircle(
            color = tint,
            radius = size.minDimension * 0.12f,
            center = Offset(size.width * 0.5f, size.height * 0.72f),
            style = stroke,
        )

        val rayStroke = size.minDimension * 0.075f
        drawLine(
            color = tint,
            start = Offset(size.width * 0.5f, size.height * 0.11f),
            end = Offset(size.width * 0.5f, size.height * 0.33f),
            strokeWidth = rayStroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = tint,
            start = Offset(size.width * 0.31f, size.height * 0.15f),
            end = Offset(size.width * 0.39f, size.height * 0.33f),
            strokeWidth = rayStroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = tint,
            start = Offset(size.width * 0.69f, size.height * 0.15f),
            end = Offset(size.width * 0.61f, size.height * 0.33f),
            strokeWidth = rayStroke,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
private fun RitualsIcon(
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier.size(25.dp)) {
        val stroke = Stroke(width = size.minDimension * 0.09f, cap = StrokeCap.Round)
        drawRoundRect(
            color = tint,
            topLeft = Offset(size.width * 0.36f, size.height * 0.46f),
            size = Size(size.width * 0.28f, size.height * 0.55f),
            style = stroke,
        )
        val flamePath = Path().apply {
            moveTo(size.width * 0.5f, size.height * 0.14f)
            quadraticBezierTo(size.width * 0.61f, size.height * 0.28f, size.width * 0.52f, size.height * 0.44f)
            quadraticBezierTo(size.width * 0.38f, size.height * 0.29f, size.width * 0.5f, size.height * 0.14f)
            close()
        }
        drawPath(path = flamePath, color = tint, style = stroke)
        drawLine(
            color = tint,
            start = Offset(size.width * 0.5f, size.height * 0.44f),
            end = Offset(size.width * 0.5f, size.height * 0.37f),
            strokeWidth = size.minDimension * 0.07f,
            cap = StrokeCap.Round,
        )
    }
}
