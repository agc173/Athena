package com.agc.bwitch.ui.portal

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.agc.bwitch.domain.session.ClearLocalUserDataUseCase
import com.agc.bwitch.presentation.auth.SessionViewModel
import com.agc.bwitch.presentation.navigation.Destination
import com.agc.bwitch.ui.common.AppScaffold
import com.agc.bwitch.ui.common.designsystem.BWitchCard
import com.agc.bwitch.ui.common.designsystem.BWitchScreen
import com.agc.bwitch.ui.common.designsystem.BWitchSectionHeader
import com.agc.bwitch.ui.theme.BWitchThemeTokens
import kotlinx.coroutines.launch
import org.koin.compose.koinInject

@Composable
fun PortalScreen(
    contentPadding: PaddingValues,
    onNavigate: (Destination) -> Unit,
    modifier: Modifier = Modifier
) {
    val sessionVm: SessionViewModel = koinInject()
    val clearLocalUserData: ClearLocalUserDataUseCase = koinInject()
    val scope = rememberCoroutineScope()

    val items = listOf(
        PortalItemConfig(
            title = "Astrología",
            subtitle = "Horóscopo, carta astral, compatibilidad…",
            destination = Destination.Astrology,
            enabled = true
        ),
        PortalItemConfig(
            title = "Perfil",
            subtitle = "Avatar, signo y ajustes",
            destination = Destination.UserProfile,
            enabled = true
        ),
        PortalItemConfig(
            title = "Guía",
            subtitle = "Tarot, Oráculo y El Péndulo",
            destination = Destination.Guide,
            enabled = true
        ),
        PortalItemConfig(
            title = "Oracle Debug",
            subtitle = "Estado de oracleGetStatus",
            destination = Destination.OracleDebug,
            enabled = true
        ),
        PortalItemConfig(
            title = "Luna",
            subtitle = "Fases y rituales",
            destination = null,
            enabled = false
        )
    )

    AppScaffold(
        title = "BWitch",
        canGoBack = false,
        onBack = {},
        modifier = modifier
    ) { scaffoldPadding ->
        BWitchScreen(
            contentPadding = scaffoldPadding,
            modifier = Modifier.padding(contentPadding),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                BWitchSectionHeader(
                    title = "Portal",
                    subtitle = "Elige un módulo",
                    titleStyle = MaterialTheme.typography.headlineSmall,
                    subtitleStyle = MaterialTheme.typography.bodyMedium,
                )

                Button(
                    onClick = {
                        scope.launch {
                            runCatching { sessionVm.signOut() }
                            runCatching { clearLocalUserData() }
                        }
                    }
                ) {
                    Text("Cerrar sesión")
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(BWitchThemeTokens.dimens.spacingSm)) {
                items.forEach { item ->
                    PortalItem(
                        title = item.title,
                        subtitle = item.subtitle,
                        enabled = item.enabled && item.destination != null,
                        onClick = {
                            val dest = item.destination ?: return@PortalItem
                            onNavigate(dest)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun PortalItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    BWitchCard(
        onClick = onClick,
        enabled = enabled,
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        Text(
            subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
