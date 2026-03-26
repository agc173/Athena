package com.agc.bwitch.ui.common.designsystem

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.agc.bwitch.ui.theme.BWitchThemeTokens

@Composable
fun BWitchScreen(
    contentPadding: PaddingValues,
    modifier: Modifier = Modifier,
    verticalArrangement: Arrangement.Vertical? = null,
    content: @Composable ColumnScope.() -> Unit,
) {
    val dimens = BWitchThemeTokens.dimens

    Column(
        modifier = modifier
            .padding(contentPadding)
            .padding(horizontal = dimens.screenHorizontalPadding, vertical = dimens.screenVerticalPadding),
        verticalArrangement = verticalArrangement ?: Arrangement.spacedBy(dimens.sectionSpacing),
        content = content,
    )
}
