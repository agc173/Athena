package com.agc.bwitch.ui.common

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.agc.bwitch.ui.theme.BWitchThemeTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    title: String,
    canGoBack: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable (padding: PaddingValues) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val extras = BWitchThemeTokens.extras

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = extras.surfaceElevated,
                    titleContentColor = colorScheme.onSurface,
                    navigationIconContentColor = colorScheme.onSurface,
                    actionIconContentColor = colorScheme.onSurfaceVariant,
                ),
                navigationIcon = {
                    if (canGoBack) {
                        IconButton(onClick = onBack) {
                            Text(text = "←", style = MaterialTheme.typography.titleLarge)
                        }
                    }
                }
            )
        },
        containerColor = extras.screenBackground,
        contentColor = colorScheme.onBackground,
        modifier = modifier,
        content = content,
    )
}
