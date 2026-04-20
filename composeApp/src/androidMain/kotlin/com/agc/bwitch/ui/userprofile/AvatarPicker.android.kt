package com.agc.bwitch.ui.userprofile

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import bwitch.composeapp.generated.resources.Res
import bwitch.composeapp.generated.resources.profile_select_avatar
import org.jetbrains.compose.resources.stringResource

@Composable
actual fun AvatarPickerButton(
    enabled: Boolean,
    onPicked: (uriString: String, mimeType: String?) -> Unit
) {
    val context: Context = LocalContext.current

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        val mime = context.contentResolver.getType(uri)
        onPicked(uri.toString(), mime)
    }

    Button(
        onClick = { launcher.launch("image/*") },
        enabled = enabled
    ) {
        Text(stringResource(Res.string.profile_select_avatar))
    }
}
