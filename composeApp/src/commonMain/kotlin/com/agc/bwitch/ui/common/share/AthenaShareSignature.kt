package com.agc.bwitch.ui.common.share

private const val DEFAULT_APP_NAME = "ATHENA"

fun String.withAthenaShareSignature(appName: String): String {
    val trimmedText = trim()
    if (trimmedText.isEmpty()) return ""

    val normalizedAppName = appName.trim().ifEmpty { DEFAULT_APP_NAME }
    val signature = "Shared from $normalizedAppName"
    if (trimmedText.contains(signature)) return trimmedText

    return "$trimmedText\n\n$signature"
}
