package com.agc.bwitch.domain.userprofile

fun UserProfile?.hasMinimumProfileCompleted(): Boolean {
    if (this == null) return false
    return !username.isNullOrBlank() && birthDate != null && zodiacSign != null
}
