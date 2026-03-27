package com.agc.bwitch.ui.userprofile

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.agc.bwitch.domain.astrology.birthchart.BirthEssenceProfile
import com.agc.bwitch.domain.astrology.horoscope.ZodiacSign
import com.agc.bwitch.presentation.userprofile.UserProfileViewModel
import com.agc.bwitch.ui.common.designsystem.BWitchCard
import com.agc.bwitch.ui.common.designsystem.BWitchSectionHeader
import com.agc.bwitch.ui.common.toVisualResource
import com.agc.bwitch.ui.theme.BWitchThemeTokens
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject

@Composable
fun ProfileScreen(
    contentPadding: PaddingValues,
    onOpenSettings: () -> Unit,
    onEditProfile: () -> Unit,
    onDiscoverEssence: () -> Unit,
    onOpenStore: (() -> Unit)? = null,
) {
    val vm: UserProfileViewModel = koinInject()
    val state by vm.uiState.collectAsState()
    val savedEssence = state.savedBirthEssence
    var showBirthEssenceDialog by remember { mutableStateOf(false) }

    val profile = state.profile
    val displayName = profile?.displayName?.takeIf { it.isNotBlank() } ?: "Tu perfil"
    val username = profile?.username?.takeIf { it.isNotBlank() }
    val zodiacSign = profile?.zodiacSign
    val zodiacLine = zodiacSign?.let { "${it.symbol()} ${it.label}" } ?: "✧ Signo pendiente"
    val avatarUrl = profile?.photoUrl?.takeIf { it.startsWith("http://") || it.startsWith("https://") }
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
        BWitchCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(dimens.spacingMd),
            contentVerticalArrangement = Arrangement.spacedBy(dimens.spacingMd),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                HeaderAction(
                    icon = HeaderActionIcon.Edit,
                    onClick = onEditProfile,
                )
                Spacer(modifier = Modifier.size(dimens.spacingSm))
                HeaderAction(
                    icon = HeaderActionIcon.Settings,
                    onClick = onOpenSettings,
                )
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(dimens.spacingSm),
            ) {
                if (avatarUrl != null) {
                    KamelImage(
                        resource = asyncPainterResource(avatarUrl),
                        contentDescription = "Avatar de perfil",
                        modifier = Modifier
                            .size(104.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(104.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "✨",
                            style = MaterialTheme.typography.headlineLarge,
                        )
                    }
                }

                Text(
                    text = displayName,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )

                Text(
                    text = zodiacLine,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.primary,
                    textAlign = TextAlign.Center,
                )

                username?.let {
                    Text(
                        text = "@$it",
                        style = MaterialTheme.typography.bodyMedium,
                        color = extras.textSecondary,
                    )
                }
            }
        }

        BWitchCard(
            modifier = Modifier.fillMaxWidth(),
            onClick = {
                if (savedEssence != null) {
                    showBirthEssenceDialog = true
                } else {
                    onDiscoverEssence()
                }
            },
        ) {
            BWitchSectionHeader(
                title = "Tu esencia natal",
                subtitle = if (savedEssence != null) {
                    "Tócala para abrir tu interpretación"
                } else {
                    "Descubre tu esencia en Astrología"
                },
            )
            HorizontalDivider(color = extras.borderSubtle.copy(alpha = 0.45f))
            Text(
                text = if (savedEssence != null) {
                    "Sol ${savedEssence.sunSign.label} · Luna ${savedEssence.moonSign.label} · Asc ${savedEssence.risingSign.label}"
                } else {
                    "Aún no has vinculado tu esencia. Te acompañamos a descubrirla."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        BWitchCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(dimens.spacingXs)) {
                    Text("Lunas", style = MaterialTheme.typography.labelLarge)
                    Text(
                        text = moonCredits?.toString() ?: "0",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }

                MiniAction(
                    label = "Tienda",
                    subLabel = if (onOpenStore == null) "Próximamente" else "Abrir",
                    onClick = { onOpenStore?.invoke() },
                    enabled = onOpenStore != null,
                )
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(dimens.spacingSm)) {
            BWitchSectionHeader(
                title = "Tu progreso",
                subtitle = "Una vista suave de tu camino en BWitch",
            )

            ProgressPlaceholderCard(
                title = "Astrología",
                subtitle = "Tu evolución natal y próximos hitos",
            )
            ProgressPlaceholderCard(
                title = "Guía y Tarot",
                subtitle = "Rituales, consultas y seguimiento",
            )
            ProgressPlaceholderCard(
                title = "Colección",
                subtitle = "Espacio reservado para tu colección futura",
            )
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
private fun HeaderAction(
    icon: HeaderActionIcon,
    onClick: () -> Unit,
) {
    val dimens = BWitchThemeTokens.dimens
    val extras = BWitchThemeTokens.extras

    Surface(
        modifier = Modifier
            .clip(CircleShape)
            .clickable(onClick = onClick),
        color = extras.surfaceElevated,
        shape = CircleShape,
    ) {
        Box(
            modifier = Modifier.padding(dimens.spacingSm),
            contentAlignment = Alignment.Center,
        ) {
            HeaderActionIconView(
                icon = icon,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(18.dp),
            )
        }
    }
}

private enum class HeaderActionIcon {
    Edit,
    Settings,
}

@Composable
private fun HeaderActionIconView(
    icon: HeaderActionIcon,
    tint: Color,
    modifier: Modifier = Modifier,
) {
    Canvas(modifier = modifier) {
        when (icon) {
            HeaderActionIcon.Edit -> {
                val stroke = size.minDimension * 0.11f
                drawLine(
                    color = tint,
                    start = Offset(size.width * 0.22f, size.height * 0.78f),
                    end = Offset(size.width * 0.74f, size.height * 0.26f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = tint,
                    start = Offset(size.width * 0.68f, size.height * 0.2f),
                    end = Offset(size.width * 0.82f, size.height * 0.34f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = tint,
                    start = Offset(size.width * 0.2f, size.height * 0.8f),
                    end = Offset(size.width * 0.42f, size.height * 0.8f),
                    strokeWidth = stroke,
                    cap = StrokeCap.Round,
                )
            }

            HeaderActionIcon.Settings -> {
                val center = Offset(size.width / 2f, size.height / 2f)
                val outerRadius = size.minDimension * 0.42f
                val innerRadius = size.minDimension * 0.23f
                val stroke = size.minDimension * 0.09f

                for (i in 0 until 8) {
                    val angle = (i * 45f) * (PI.toFloat() / 180f)
                    val cosAngle = cos(angle)
                    val sinAngle = sin(angle)
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
    }
}

@Composable
private fun MiniAction(
    label: String,
    subLabel: String,
    onClick: () -> Unit,
    enabled: Boolean,
) {
    val dimens = BWitchThemeTokens.dimens
    val extras = BWitchThemeTokens.extras

    Surface(
        modifier = Modifier
            .clip(MaterialTheme.shapes.small)
            .clickable(enabled = enabled, onClick = onClick),
        color = if (enabled) extras.surfaceElevated else extras.surfaceMuted,
        shape = MaterialTheme.shapes.small,
    ) {
        Column(
            modifier = Modifier.padding(
                horizontal = dimens.spacingSm + dimens.spacingXs,
                vertical = dimens.spacingSm,
            ),
            horizontalAlignment = Alignment.CenterHorizontally,
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
private fun ProgressPlaceholderCard(
    title: String,
    subtitle: String,
) {
    val extras = BWitchThemeTokens.extras

    BWitchCard(
        modifier = Modifier.fillMaxWidth(),
        contentVerticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodySmall,
            color = extras.textSecondary,
        )
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
