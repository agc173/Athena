package com.agc.bwitch.ui.common

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.path
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
                    navigationIconContentColor = extras.topBarIconColor,
                    actionIconContentColor = colorScheme.onSurfaceVariant,
                ),
                modifier = Modifier.padding(horizontal = dimens.topBarHorizontalPadding),
                navigationIcon = {
                    if (canGoBack) {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = BackChevronIcon,
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

private val BackChevronIcon: ImageVector = ImageVector.Builder(
    name = "BackChevronIcon",
    defaultWidth = 24.dp,
    defaultHeight = 24.dp,
    viewportWidth = 24f,
    viewportHeight = 24f,
).apply {
    path {
        moveTo(20f, 11f)
        lineTo(7.83f, 11f)
        lineTo(13.42f, 5.41f)
        lineTo(12f, 4f)
        lineTo(4f, 12f)
        lineTo(12f, 20f)
        lineTo(13.41f, 18.59f)
        lineTo(7.83f, 13f)
        lineTo(20f, 13f)
        close()
    }
}.build()
