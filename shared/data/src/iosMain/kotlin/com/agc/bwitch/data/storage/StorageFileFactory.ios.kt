package com.agc.bwitch.data.storage

import dev.gitlive.firebase.storage.File
import platform.Foundation.NSURL

actual fun storageFileFromUri(uri: String): File =
    File(NSURL(string = uri) ?: error("Invalid uri: $uri"))