package com.agc.bwitch.ui.userprofile

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
actual fun AvatarPickerButton(
    onPicked: (uriString: String, mimeType: String?) -> Unit
) {
    // Placeholder: implementaremos picker iOS después
    Button(onClick = { /* no-op */ }) {
        Text("Seleccionar avatar (iOS pendiente)")
    }
}