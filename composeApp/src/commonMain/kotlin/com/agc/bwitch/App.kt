package com.agc.bwitch

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.agc.bwitch.ui.theme.BWitchTheme
import com.agc.bwitch.ui.theme.BWitchThemeTokens

@Composable
fun App() {
    BWitchTheme {
        Surface(
            modifier = Modifier,
            color = BWitchThemeTokens.extras.screenBackground,
        ) {
            AppRoot()
        }
    }
}

@Preview
@Composable
fun AppPreview() {
    App()
}
