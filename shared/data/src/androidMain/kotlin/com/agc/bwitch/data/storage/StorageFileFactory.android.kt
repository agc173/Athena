package com.agc.bwitch.data.storage

import android.net.Uri
import dev.gitlive.firebase.storage.File

actual fun storageFileFromUri(uri: String): File =
    File(Uri.parse(uri))