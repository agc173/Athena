package com.agc.bwitch

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.agc.bwitch.localization.AppLocaleEnvironment
import com.agc.bwitch.localization.LocalAppStrings
import com.agc.bwitch.localization.resolveAppStrings
import com.agc.bwitch.presentation.localization.AppLanguageViewModel
import com.agc.bwitch.ui.theme.BWitchTheme
import com.agc.bwitch.ui.theme.BWitchThemeTokens
import org.koin.compose.koinInject

@Composable
fun App() {
    val appLanguageVm: AppLanguageViewModel = koinInject()
    val appLanguageState by appLanguageVm.uiState.collectAsState()
    val strings = resolveAppStrings(appLanguageState.currentLanguage)

    BWitchTheme {
        CompositionLocalProvider(LocalAppStrings provides strings) {
            AppLocaleEnvironment(languageCode = appLanguageState.currentLanguage.code) {
                Surface(
                    modifier = Modifier,
                    color = BWitchThemeTokens.extras.screenBackground,
                ) {
                    AppRoot()
                }
            }
        }
    }
}

@Preview
@Composable
fun AppPreview() {
    App()
}
