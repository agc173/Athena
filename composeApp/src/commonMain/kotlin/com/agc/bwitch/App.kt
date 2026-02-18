package com.agc.bwitch

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.agc.bwitch.ui.astrology.HoroscopeScreen

@Composable
fun App() {
    MaterialTheme {
        Surface(modifier = Modifier) {
            HoroscopeScreen()
        }
    }
}

@Preview
@Composable
fun AppPreview() {
    App()
}

