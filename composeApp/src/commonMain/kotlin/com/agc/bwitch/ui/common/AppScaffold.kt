package com.agc.bwitch.ui.common

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.agc.bwitch.ui.theme.BWitchThemeTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppScaffold(
    title: String,
    canGoBack: Boolean,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    bottomBar: @Composable () -> Unit = {},
    actions: @Composable RowScope.() -> Unit = {},
    content: @Composable (padding: PaddingValues) -> Unit,
) {
    val colorScheme = MaterialTheme.colorScheme
    val extras = BWitchThemeTokens.extras
    val dimens = BWitchThemeTokens.dimens

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(start = dimens.topBarTitleStartPadding),
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = extras.topBarContainer,
                    titleContentColor = colorScheme.onSurface,
                    navigationIconContentColor = colorScheme.onSurface,
                    actionIconContentColor = colorScheme.onSurfaceVariant,
                ),
                modifier = Modifier.padding(horizontal = dimens.topBarHorizontalPadding),
                navigationIcon = {
                    if (canGoBack) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                            )
                        }
                    }
                },
                actions = actions,
            )
            HorizontalDivider(color = extras.topBarDivider.copy(alpha = 0.4f), thickness = 1.dp)
        },
        bottomBar = bottomBar,
        containerColor = extras.screenBackground,
        contentColor = colorScheme.onBackground,
        modifier = modifier,
        content = content,
    )
}
