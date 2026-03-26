package com.agc.bwitch.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import com.agc.bwitch.ui.theme.BWitchTopBarBackTitleTextStyle
import com.agc.bwitch.ui.theme.BWitchTopBarRootTitleTextStyle

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
    val topBarTitleStyle = if (canGoBack) {
        BWitchTopBarBackTitleTextStyle
    } else {
        BWitchTopBarRootTitleTextStyle()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = title,
                        style = topBarTitleStyle,
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
                        IconButton(
                            onClick = onBack,
                            modifier = Modifier
                                .size(dimens.topBarBackButtonSize)
                                .background(
                                    color = extras.topBarBackButtonContainer,
                                    shape = CircleShape,
                                ),
                        ) {
                            Icon(
                                imageVector = BackChevronIcon,
                                contentDescription = "Back",
                                tint = extras.topBarIconColor,
                                modifier = Modifier.size(dimens.topBarBackIconSize),
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
        moveTo(20f, 10f)
        lineTo(9.4f, 10f)
        lineTo(13.9f, 5.5f)
        lineTo(12f, 3.6f)
        lineTo(4f, 11.6f)
        lineTo(12f, 19.6f)
        lineTo(13.9f, 17.7f)
        lineTo(9.4f, 13.2f)
        lineTo(20f, 13.2f)
        close()
    }
}.build()
