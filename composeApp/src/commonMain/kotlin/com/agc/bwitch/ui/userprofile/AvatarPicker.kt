package com.agc.bwitch.ui.userprofile

import androidx.compose.runtime.Composable

@Composable
expect fun AvatarPickerButton(
    enabled: Boolean = true,
    buttonText: String,
    onPicked: (uriString: String, mimeType: String?) -> Unit
)
