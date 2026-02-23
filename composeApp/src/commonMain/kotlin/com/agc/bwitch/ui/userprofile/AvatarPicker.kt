package com.agc.bwitch.ui.userprofile

import androidx.compose.runtime.Composable

@Composable
expect fun AvatarPickerButton(
    onPicked: (uriString: String, mimeType: String?) -> Unit
)