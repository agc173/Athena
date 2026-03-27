package com.agc.bwitch.ui.userprofile

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.agc.bwitch.domain.astrology.birthchart.BirthEssenceProfile
import com.agc.bwitch.domain.astrology.horoscope.ZodiacSign
import com.agc.bwitch.presentation.userprofile.UserProfileViewModel
import com.agc.bwitch.ui.common.toVisualResource
import com.agc.bwitch.ui.theme.BWitchThemeTokens
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject

@Composable
fun ProfileScreen(
    contentPadding: PaddingValues,
    onEditProfile: () -> Unit,
    onDiscoverEssence: () -> Unit,
    onOpenStore: (() -> Unit)? = null,
) {
    val vm: UserProfileViewModel = koinInject()
    val state by vm.uiState.collectAsState()
    val savedEssence = state.savedBirthEssence
    var showBirthEssenceDialog by remember { mutableStateOf(false) }

    val profile = state.profile
    val username = profile?.username?.takeIf { it.isNotBlank() }
    val usernameLine = username?.let { "@$it" } ?: "@perfil"
    val zodiacSign = profile?.zodiacSign
    val zodiacLabel = zodiacSign?.let { "${it.symbol()} ${it.label}" } ?: "✧ Signo pendiente"
    val avatarUrl = profile?.photoUrl?.takeIf { it.startsWith("http://") || it.startsWith("https://") }
    val profileDescription: String? = null
    val moonCredits: Int? = null

    val dimens = BWitchThemeTokens.dimens
    val extras = BWitchThemeTokens.extras

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
            if (avatarUrl != null) {
                KamelImage(
                    resource = asyncPainterResource(avatarUrl),
                    contentDescription = "Avatar de perfil",
                    modifier = Modifier
                        .size(96.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop,
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(96.dp)
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
                profileDescription?.takeIf { it.isNotBlank() }?.let { description ->
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodyMedium,
                        color = extras.textSecondary,
                    )
                }
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(dimens.spacingSm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconMiniAction(
                onClick = onEditProfile,
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
                    Text("Lunas", style = MaterialTheme.typography.labelMedium, color = extras.textSecondary)
                    Text(
                        text = moonCredits?.toString() ?: "0",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            MiniAction(
                label = "Tienda",
                subLabel = if (onOpenStore == null) "Pronto" else "Abrir",
                onClick = { onOpenStore?.invoke() },
                enabled = onOpenStore != null,
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
                    text = "Esencia natal",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = if (savedEssence != null) {
                        "SOL ${savedEssence.sunSign.label} · LUNA ${savedEssence.moonSign.label} · ASC ${savedEssence.risingSign.label}"
                    } else {
                        "Aún no has vinculado tu esencia. Te acompañamos a descubrirla."
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }

    savedEssence?.let { essence ->
        if (showBirthEssenceDialog) {
            BirthEssenceDialog(
                essence = essence,
                onDismiss = { showBirthEssenceDialog = false },
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
                drawLine(
                    color = iconColor,
                    start = Offset(size.width * 0.22f, size.height * 0.78f),
                    end = Offset(size.width * 0.76f, size.height * 0.24f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = iconColor,
                    start = Offset(size.width * 0.7f, size.height * 0.18f),
                    end = Offset(size.width * 0.84f, size.height * 0.32f),
                    strokeWidth = stroke,
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
        Column(
            modifier = Modifier.padding(
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
    }
}

@Composable
private fun BirthEssenceDialog(
    essence: BirthEssenceProfile,
    onDismiss: () -> Unit,
) {
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
                Text("Esencia natal", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "Sol: ${essence.sunSign.label} · Luna: ${essence.moonSign.label} · Ascendente: ${essence.risingSign.label}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                essence.archetype?.let { archetype ->
                    Text("Arquetipo", style = MaterialTheme.typography.labelLarge)
                    Text(archetype.displayNameEs, style = MaterialTheme.typography.titleMedium)
                    androidx.compose.foundation.Image(
                        painter = painterResource(archetype.toVisualResource()),
                        contentDescription = "Visual ${archetype.displayNameEs}",
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
                    Text("Cerrar")
                }
            }
        }
    }
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
