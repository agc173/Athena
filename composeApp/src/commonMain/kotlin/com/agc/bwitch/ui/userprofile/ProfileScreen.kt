package com.agc.bwitch.ui.userprofile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.agc.bwitch.domain.astrology.birthchart.BirthEssenceProfile
import com.agc.bwitch.domain.astrology.horoscope.ConstellationProgressRules
import com.agc.bwitch.domain.astrology.horoscope.ZodiacSign
import com.agc.bwitch.domain.rituals.completedHabitBadgesForCycles
import com.agc.bwitch.localization.AppStrings
import com.agc.bwitch.localization.appStrings
import com.agc.bwitch.presentation.economy.EconomyViewModel
import com.agc.bwitch.presentation.userprofile.UserProfileViewModel
import com.agc.bwitch.presentation.userprofile.PROFILE_BIRTH_DATE_IN_FUTURE_ERROR_KEY
import com.agc.bwitch.presentation.userprofile.PROFILE_BIRTH_DATE_INVALID_ERROR_KEY
import com.agc.bwitch.presentation.userprofile.PROFILE_DESCRIPTION_TOO_LONG_ERROR_KEY
import com.agc.bwitch.ui.common.BirthDateSelector
import com.agc.bwitch.ui.common.ConstellationBadgeCard
import com.agc.bwitch.ui.common.ZodiacStylizedTemplates
import com.agc.bwitch.ui.common.toVisualResource
import com.agc.bwitch.ui.rituals.components.habitBadgeResourceFor
import com.agc.bwitch.ui.theme.BWitchThemeTokens
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

@Composable
fun ProfileScreen(
    contentPadding: PaddingValues,
    onEditProfile: () -> Unit,
    onDiscoverEssence: () -> Unit,
    onOpenHabits: () -> Unit,
    onOpenStore: (() -> Unit)? = null,
    onOpenArcanaCollection: (() -> Unit)? = null,
    onOpenTarot: (() -> Unit)? = null,
    onOpenHoroscope: (() -> Unit)? = null,
) {
    val strings = appStrings
    val profileStrings = strings.profile
    val vm: UserProfileViewModel = koinInject()
    val economyVm: EconomyViewModel = koinInject()
    val state by vm.uiState.collectAsState()
    val economyState by economyVm.uiState.collectAsState()
    val savedEssence = state.savedBirthEssence
    var showBirthEssenceDialog by remember { mutableStateOf(false) }
    var showEditDialog by remember { mutableStateOf(false) }
    var showConstellationsDialog by remember { mutableStateOf(false) }

    val profile = state.profile
    val username = profile?.username?.takeIf { it.isNotBlank() }
    val usernameLine = username?.let { "@$it" } ?: "@${profileStrings.defaultUsername}"
    val zodiacSign = profile?.zodiacSign
    val zodiacLabel = zodiacSign?.let { "${it.symbol()} ${it.localizedLabel(strings)}" } ?: "✧ ${profileStrings.pendingSign}"
    val avatarUrl = profile?.photoUrl?.takeIf(::isHttpAvatarUrl)
    val profileDescription: String? = profile?.description
    val moonCredits = state.moonBalance
    val habitsProgress = state.habitsProgress
    val completedBadges = completedHabitBadgesForCycles(habitsProgress.completedCycles)

    val dimens = BWitchThemeTokens.dimens
    val extras = BWitchThemeTokens.extras
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = dimens.screenHorizontalPadding, vertical = dimens.spacingLg),
        verticalArrangement = Arrangement.spacedBy(dimens.sectionSpacing)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dimens.spacingMd),
            verticalAlignment = Alignment.Top,
        ) {
            ProfileAvatarPreview(
                avatarUrl = avatarUrl,
                contentDescription = profileStrings.avatarContentDescription,
                size = 96.dp,
            )

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(dimens.spacingSm),
            ) {
                Text(
                    text = usernameLine,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                )

                ZodiacBadge(label = zodiacLabel)
                Text(
                    text = profileDescription?.takeIf { it.isNotBlank() } ?: profileStrings.descriptionPlaceholder,
                    style = MaterialTheme.typography.bodyMedium,
                    color = extras.textSecondary,
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dimens.spacingSm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconMiniAction(
                onClick = { showEditDialog = true },
                enabled = true,
            )

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = extras.surfaceElevated,
                modifier = Modifier.weight(0.8f),
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = dimens.spacingSm, vertical = dimens.spacingSm),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(2.dp),
                ) {
                    Text(profileStrings.moonCreditsTitle, style = MaterialTheme.typography.labelMedium, color = extras.textSecondary)
                    Text(
                        text = profileStrings.moonCreditsValueFormat.replaceFirst("%d", "$moonCredits"),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            MiniAction(
                label = profileStrings.storeLabel,
                subLabel = if (onOpenStore == null) profileStrings.storeSoon else profileStrings.storeOpen,
                onClick = { onOpenStore?.invoke() },
                enabled = onOpenStore != null,
                showBadge = economyState.hasStorePendingClaim,
                modifier = Modifier.weight(0.8f),
            )
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = extras.surfaceElevated,
            tonalElevation = 0.dp,
            onClick = {
                if (savedEssence != null) {
                    showBirthEssenceDialog = true
                } else {
                    onDiscoverEssence()
                }
            },
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimens.spacingMd, vertical = dimens.spacingMd),
                verticalArrangement = Arrangement.spacedBy(dimens.spacingXs),
            ) {
                Text(
                    text = profileStrings.birthEssenceTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (savedEssence != null) {
                        "${profileStrings.birthEssenceSunLabel} ${savedEssence.sunSign.localizedLabel(strings)} · " +
                            "${profileStrings.birthEssenceMoonLabel} ${savedEssence.moonSign.localizedLabel(strings)} · " +
                            "${profileStrings.birthEssenceAscLabel} ${savedEssence.risingSign.localizedLabel(strings)}"
                    } else {
                        profileStrings.birthEssenceEmpty
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = extras.surfaceElevated,
            tonalElevation = 0.dp,
            onClick = { onOpenArcanaCollection?.invoke() },
            enabled = onOpenArcanaCollection != null,
        ) {
            ProgressCtaCardContent(
                title = profileStrings.arcanaCollectionTitle,
                subtitle = profileStrings.arcanaCollectionSubtitle,
                iconText = "▣",
                iconContentDescription = profileStrings.arcanaCollectionTarotCtaContentDescription,
                onIconClick = onOpenTarot,
            )
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = extras.surfaceElevated,
            tonalElevation = 0.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimens.spacingMd, vertical = dimens.spacingMd),
                verticalArrangement = Arrangement.spacedBy(dimens.spacingSm),
            ) {
                Text(
                    text = profileStrings.progressTitle,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = profileStrings.progressSubtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                if (habitsProgress.hasStarted) {
                    Text(
                        text = "${habitsProgress.completedCycles} ${profileStrings.completedCyclesSuffix}",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )

                    if (completedBadges.isNotEmpty()) {
                        LazyRow(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(completedBadges) { badgeType ->
                                Image(
                                    painter = painterResource(habitBadgeResourceFor(badgeType)),
                                    contentDescription = null,
                                    modifier = Modifier.size(44.dp),
                                    alpha = 1f,
                                )
                            }
                        }
                    }
                } else {
                    Text(
                        text = profileStrings.progressNotStarted,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                Button(
                    onClick = onOpenHabits,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(profileStrings.openHabits)
                }
            }
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            color = extras.surfaceElevated,
            tonalElevation = 0.dp,
            onClick = { showConstellationsDialog = true },
        ) {
            ProgressCtaCardContent(
                title = profileStrings.constellationsTitle,
                subtitle = profileStrings.constellationsSubtitle,
                iconText = "✦",
                iconContentDescription = profileStrings.constellationsHoroscopeCtaContentDescription,
                onIconClick = onOpenHoroscope,
            )
        }
    }
    if (showEditDialog) {
        var descriptionDraft by remember(profile?.description) { mutableStateOf(profile?.description.orEmpty()) }
        var birthDateDraft by remember(profile?.birthDate) { mutableStateOf<LocalDate?>(profile?.birthDate) }
        Dialog(onDismissRequest = { showEditDialog = false }) {
            Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surface) {
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(profileStrings.editProfileTitle, style = MaterialTheme.typography.titleMedium)
                    Text(usernameLine, style = MaterialTheme.typography.bodySmall, color = extras.textSecondary)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(dimens.spacingSm),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        ProfileAvatarPreview(
                            avatarUrl = avatarUrl,
                            contentDescription = profileStrings.avatarContentDescription,
                            size = 56.dp,
                        )
                        AvatarPickerButton(
                            enabled = !state.isBusy,
                            buttonText = profileStrings.selectAvatarButton,
                        ) { uriString, mimeType ->
                            vm.uploadAvatarAndSave(uriString, mimeType)
                        }
                    }
                    OutlinedTextField(
                        value = descriptionDraft,
                        onValueChange = { if (it.length <= 160) descriptionDraft = it },
                        label = { Text(profileStrings.editProfileDescriptionLabel) },
                        supportingText = { Text("${descriptionDraft.length}/160") }
                    )
                    BirthDateSelector(
                        selectedDate = birthDateDraft,
                        onDateSelected = { birthDateDraft = it },
                        label = profileStrings.editProfileBirthDateLabel,
                        enabled = !state.isBusy,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    state.error?.let {
                        Text(
                            text = it.toProfileUiText(strings),
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = { showEditDialog = false }) { Text(profileStrings.editProfileCancel) }
                        Button(onClick = {
                            scope.launch {
                                val ok = vm.saveEditableProfile(descriptionDraft, birthDateDraft?.toString())
                                if (ok) showEditDialog = false
                            }
                        }, enabled = !state.isBusy) { Text(profileStrings.editProfileSave) }
                    }
                }
            }
        }
    }

    savedEssence?.let { essence ->
        if (showBirthEssenceDialog) {
            BirthEssenceDialog(
                essence = essence,
                strings = strings,
                onDismiss = { showBirthEssenceDialog = false },
            )
        }
    }

    if (showConstellationsDialog) {
        Dialog(
            onDismissRequest = { showConstellationsDialog = false },
            properties = DialogProperties(usePlatformDefaultWidth = false),
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 12.dp, vertical = 24.dp),
                shape = RoundedCornerShape(24.dp),
                color = Color(0xFF070913),
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Canvas(modifier = Modifier.matchParentSize()) {
                        drawRect(
                            brush = Brush.verticalGradient(
                                colors = listOf(Color(0xFF090D1C), Color(0xFF120B1F), Color(0xFF070913)),
                            ),
                        )
                        drawCircle(
                            brush = Brush.radialGradient(listOf(Color(0x223D5B96), Color.Transparent)),
                            radius = size.minDimension * 0.72f,
                            center = Offset(size.width * 0.24f, size.height * 0.18f),
                        )
                        drawCircle(
                            brush = Brush.radialGradient(listOf(Color(0x1CA860FF), Color.Transparent)),
                            radius = size.minDimension * 0.82f,
                            center = Offset(size.width * 0.84f, size.height * 0.32f),
                        )
                        drawCircle(
                            brush = Brush.radialGradient(listOf(Color(0x14FFE0AF), Color.Transparent)),
                            radius = size.minDimension * 0.56f,
                            center = Offset(size.width * 0.60f, size.height * 0.78f),
                        )
                        val stars = 60
                        repeat(stars) { index ->
                            val x = ((index * 37f) % 100f) / 100f
                            val y = ((index * 61f) % 100f) / 100f
                            val alpha = 0.18f + (index % 5) * 0.08f
                            val radius = 1.2f + (index % 3) * 0.9f
                            drawCircle(
                                color = Color.White.copy(alpha = alpha),
                                radius = radius,
                                center = Offset(size.width * x, size.height * y),
                            )
                        }
                    }
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                    ) {
                        Text(
                            text = profileStrings.constellationsTitle,
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFFF2E9FF),
                        )
                        Text(
                            text = profileStrings.constellationsDialogSubtitle,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFFD2C7EA).copy(alpha = 0.88f),
                        )
                        val templates = ZodiacStylizedTemplates
                        val maxTotalProgress = ConstellationProgressRules.maxTotalProgress
                        var remaining = state.totalConstellationProgress.coerceIn(0, maxTotalProgress)
                        val progressBySign = buildMap<ZodiacSign, Int> {
                            templates.forEach { template ->
                                val signMaxSteps = ConstellationProgressRules.stepsBySign.getValue(template.sign)
                                val current = remaining.coerceAtMost(signMaxSteps)
                                put(template.sign, current)
                                remaining = (remaining - current).coerceAtLeast(0)
                            }
                        }
                        LazyVerticalGrid(
                            columns = GridCells.Adaptive(minSize = 132.dp),
                            horizontalArrangement = Arrangement.spacedBy(22.dp),
                            verticalArrangement = Arrangement.spacedBy(20.dp),
                            modifier = Modifier.weight(1f),
                        ) {
                            items(templates) { template ->
                                ConstellationBadgeCard(
                                    template = template,
                                    progressSteps = progressBySign[template.sign] ?: 0,
                                    signName = template.sign.localizedLabel(strings),
                                )
                            }
                        }
                        Button(onClick = { showConstellationsDialog = false }, modifier = Modifier.align(Alignment.End)) { Text(profileStrings.constellationsCloseCta) }
                    }
                }
            }
        }
    }
}


private fun String.toProfileUiText(strings: AppStrings): String = when (this) {
    PROFILE_BIRTH_DATE_INVALID_ERROR_KEY -> strings.profile.birthDateFormatError
    PROFILE_BIRTH_DATE_IN_FUTURE_ERROR_KEY -> strings.profile.birthDateFutureError
    PROFILE_DESCRIPTION_TOO_LONG_ERROR_KEY -> strings.profile.descriptionTooLongError
    else -> this
}

private fun isHttpAvatarUrl(url: String): Boolean = url.startsWith("http://") || url.startsWith("https://")

@Composable
private fun ProfileAvatarPreview(
    avatarUrl: String?,
    contentDescription: String,
    size: Dp,
) {
    if (avatarUrl != null) {
        KamelImage(
            resource = asyncPainterResource(avatarUrl),
            contentDescription = contentDescription,
            modifier = Modifier
                .size(size)
                .clip(CircleShape),
            contentScale = ContentScale.Crop,
        )
    } else {
        Box(
            modifier = Modifier
                .size(size)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = "✨",
                style = MaterialTheme.typography.headlineMedium,
            )
        }
    }
}

@Composable
private fun ZodiacBadge(label: String) {
    val dimens = BWitchThemeTokens.dimens

    Surface(
        shape = RoundedCornerShape(999.dp),
        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = dimens.spacingSm + 2.dp, vertical = 6.dp),
        )
    }
}

@Composable
private fun IconMiniAction(
    onClick: () -> Unit,
    enabled: Boolean,
    modifier: Modifier = Modifier,
) {
    val extras = BWitchThemeTokens.extras
    val iconColor = MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .clickable(enabled = enabled, onClick = onClick),
        color = if (enabled) extras.surfaceElevated else extras.surfaceMuted,
        shape = MaterialTheme.shapes.small,
    ) {
        Box(
            modifier = Modifier
                .padding(10.dp)
                .size(22.dp),
            contentAlignment = Alignment.Center,
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val stroke = size.minDimension * 0.11f
                val start = Offset(size.width * 0.25f, size.height * 0.74f)
                val end = Offset(size.width * 0.74f, size.height * 0.25f)

                drawLine(
                    color = iconColor,
                    start = start,
                    end = end,
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = iconColor,
                    start = end,
                    end = Offset(size.width * 0.85f, size.height * 0.14f),
                    strokeWidth = stroke * 0.75f,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = iconColor,
                    start = end,
                    end = Offset(size.width * 0.86f, size.height * 0.36f),
                    strokeWidth = stroke * 0.75f,
                    cap = StrokeCap.Round,
                )
            }
        }
    }
}

@Composable
private fun MiniAction(
    label: String,
    subLabel: String,
    onClick: () -> Unit,
    enabled: Boolean,
    showBadge: Boolean,
    modifier: Modifier = Modifier,
) {
    val dimens = BWitchThemeTokens.dimens
    val extras = BWitchThemeTokens.extras

    Surface(
        modifier = modifier
            .clip(MaterialTheme.shapes.small)
            .clickable(enabled = enabled, onClick = onClick),
        color = if (enabled) extras.surfaceElevated else extras.surfaceMuted,
        shape = MaterialTheme.shapes.small,
    ) {
        Box(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = dimens.spacingSm,
                        vertical = dimens.spacingSm,
                    ),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = subLabel,
                    style = MaterialTheme.typography.bodySmall,
                    color = extras.textSecondary,
                )
            }

            if (showBadge) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 6.dp, end = 6.dp)
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.error),
                )
            }
        }
    }
}

@Composable
private fun BirthEssenceDialog(
    essence: BirthEssenceProfile,
    strings: AppStrings,
    onDismiss: () -> Unit,
) {
    val profileStrings = strings.profile
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = true),
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(profileStrings.birthEssenceTitle, style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "${strings.birthChart.triadSunLabel}: ${essence.sunSign.localizedLabel(strings)} · " +
                        "${strings.birthChart.triadMoonLabel}: ${essence.moonSign.localizedLabel(strings)} · " +
                        "${strings.birthChart.risingSignLabel}: ${essence.risingSign.localizedLabel(strings)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                essence.archetype?.let { archetype ->
                    val archetypeName = archetype.displayName(essence.languageCode)
                    Text(profileStrings.archetypeLabel, style = MaterialTheme.typography.labelLarge)
                    Text(archetypeName, style = MaterialTheme.typography.titleMedium)
                    androidx.compose.foundation.Image(
                        painter = painterResource(archetype.toVisualResource()),
                        contentDescription = "${profileStrings.birthEssenceVisualContentDescriptionPrefix} $archetypeName",
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.6f),
                        contentScale = ContentScale.Fit,
                    )
                }

                Text(
                    text = essence.interpretation,
                    style = MaterialTheme.typography.bodyMedium,
                )

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(profileStrings.birthEssenceDialogClose)
                }
            }
        }
    }
}

@Composable
private fun ProgressCtaCardContent(
    title: String,
    subtitle: String,
    iconText: String,
    iconContentDescription: String,
    onIconClick: (() -> Unit)?,
) {
    val dimens = BWitchThemeTokens.dimens
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = dimens.spacingMd, vertical = dimens.spacingMd),
        horizontalArrangement = Arrangement.spacedBy(dimens.spacingSm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(dimens.spacingXs),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        IconButton(
            onClick = { onIconClick?.invoke() },
            enabled = onIconClick != null,
            modifier = Modifier
                .size(44.dp)
                .clearAndSetSemantics { contentDescription = iconContentDescription },
        ) {
            Surface(
                modifier = Modifier.size(32.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = iconText,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = if (onIconClick != null) 0.72f else 0.32f),
                        style = MaterialTheme.typography.titleSmall,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

private fun ZodiacSign.localizedLabel(strings: AppStrings): String = when (this) {
    ZodiacSign.aries -> strings.zodiac.aries
    ZodiacSign.taurus -> strings.zodiac.taurus
    ZodiacSign.gemini -> strings.zodiac.gemini
    ZodiacSign.cancer -> strings.zodiac.cancer
    ZodiacSign.leo -> strings.zodiac.leo
    ZodiacSign.virgo -> strings.zodiac.virgo
    ZodiacSign.libra -> strings.zodiac.libra
    ZodiacSign.scorpio -> strings.zodiac.scorpio
    ZodiacSign.sagittarius -> strings.zodiac.sagittarius
    ZodiacSign.capricorn -> strings.zodiac.capricorn
    ZodiacSign.aquarius -> strings.zodiac.aquarius
    ZodiacSign.pisces -> strings.zodiac.pisces
}

private fun ZodiacSign.symbol(): String = when (this) {
    ZodiacSign.aries -> "♈"
    ZodiacSign.taurus -> "♉"
    ZodiacSign.gemini -> "♊"
    ZodiacSign.cancer -> "♋"
    ZodiacSign.leo -> "♌"
    ZodiacSign.virgo -> "♍"
    ZodiacSign.libra -> "♎"
    ZodiacSign.scorpio -> "♏"
    ZodiacSign.sagittarius -> "♐"
    ZodiacSign.capricorn -> "♑"
    ZodiacSign.aquarius -> "♒"
    ZodiacSign.pisces -> "♓"
}
