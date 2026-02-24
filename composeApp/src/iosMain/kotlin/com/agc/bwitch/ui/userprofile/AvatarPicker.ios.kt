package com.agc.bwitch.ui.userprofile

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable

@Composable
actual fun AvatarPickerButton(
    enabled: Boolean,
    onPicked: (uriString: String, mimeType: String?) -> Unit
) {
    // Placeholder: implementaremos picker iOS después
    Button(
        onClick = { /* no-op */ },
        enabled = enabled
    ) {
        Text("Seleccionar avatar (iOS pendiente)")
    }
}