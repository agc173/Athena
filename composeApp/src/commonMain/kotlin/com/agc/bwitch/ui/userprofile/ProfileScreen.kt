package com.agc.bwitch.ui.userprofile

import androidx.compose.foundation.background
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import bwitch.composeapp.generated.resources.Res
import bwitch.composeapp.generated.resources.major_01_magician
import bwitch.composeapp.generated.resources.major_02_high_priestess
import bwitch.composeapp.generated.resources.major_07_chariot
import bwitch.composeapp.generated.resources.major_09_hermit
import bwitch.composeapp.generated.resources.major_14_temperance
import bwitch.composeapp.generated.resources.major_19_sun
import com.agc.bwitch.domain.astrology.birthchart.BirthChartRepository
import com.agc.bwitch.domain.astrology.birthchart.BirthEssenceArchetype
import com.agc.bwitch.domain.astrology.birthchart.BirthEssenceProfile
import com.agc.bwitch.presentation.userprofile.UserProfileViewModel
import io.kamel.image.KamelImage
import io.kamel.image.asyncPainterResource
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject

@Composable
fun ProfileScreen(
    contentPadding: PaddingValues,
    onOpenSettings: () -> Unit,
) {
    val vm: UserProfileViewModel = koinInject()
    val birthChartRepository: BirthChartRepository = koinInject()
    val state by vm.uiState.collectAsState()
    val savedEssence by birthChartRepository.observeBirthEssence().collectAsState(initial = null)
    var showBirthEssenceDialog by remember { mutableStateOf(false) }

    val profile = state.profile
    val username = profile?.username?.takeIf { it.isNotBlank() } ?: "sin_username"
    val zodiac = profile?.zodiacSign?.label ?: "Signo pendiente"
    val avatarUrl = profile?.photoUrl?.takeIf { it.startsWith("http://") || it.startsWith("https://") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(contentPadding)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically)
    ) {
        if (avatarUrl != null) {
            KamelImage(
                resource = asyncPainterResource(avatarUrl),
                contentDescription = "Avatar de perfil",
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop,
            )
        } else {
            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "✨",
                    style = MaterialTheme.typography.headlineLarge
                )
            }
        }

        Text(
            text = "@$username",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.SemiBold
        )

        Text(
            text = zodiac,
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.primary
        )

        savedEssence?.let {
            OutlinedButton(
                onClick = { showBirthEssenceDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Esencia natal")
            }
        }

        Button(
            onClick = onOpenSettings,
            modifier = Modifier
                .padding(top = 8.dp)
                .fillMaxWidth()
        ) {
            Text("Ajustes")
        }
    }

    savedEssence?.let { essence ->
        if (showBirthEssenceDialog) {
            BirthEssenceDialog(
                essence = essence,
                onDismiss = { showBirthEssenceDialog = false }
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
        properties = DialogProperties(usePlatformDefaultWidth = true)
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
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Esencia natal", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "Sol: ${essence.sunSign.label} · Luna: ${essence.moonSign.label} · Ascendente: ${essence.risingSign.label}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                essence.archetype?.let { archetype ->
                    Text("Arquetipo", style = MaterialTheme.typography.labelLarge)
                    Text(archetype.displayNameEs, style = MaterialTheme.typography.titleMedium)
                    Image(
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
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cerrar")
                }
            }
        }
    }
}

private fun BirthEssenceArchetype.toVisualResource(): DrawableResource = when (this) {
    BirthEssenceArchetype.MISTICA -> Res.drawable.major_02_high_priestess
    BirthEssenceArchetype.GUERRERA -> Res.drawable.major_07_chariot
    BirthEssenceArchetype.SANADORA -> Res.drawable.major_14_temperance
    BirthEssenceArchetype.VIDENTE -> Res.drawable.major_09_hermit
    BirthEssenceArchetype.ALQUIMISTA -> Res.drawable.major_01_magician
    BirthEssenceArchetype.GUARDIANA -> Res.drawable.major_19_sun
}
