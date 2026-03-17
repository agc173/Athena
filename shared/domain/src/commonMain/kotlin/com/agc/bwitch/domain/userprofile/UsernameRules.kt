package com.agc.bwitch.domain.userprofile

object UsernameRules {
    const val MIN_LENGTH = 3
    const val MAX_LENGTH = 30

    private val allowedRegex = Regex("^[a-z0-9._]+$")

    fun normalize(raw: String?): String? {
        val normalized = raw
            ?.trim()
            ?.removePrefix("@")
            ?.lowercase()
            ?.takeUnless { it.isBlank() }
            ?: return null

        return normalized
    }

    fun isValid(normalized: String): Boolean {
        if (normalized.length !in MIN_LENGTH..MAX_LENGTH) return false
        return allowedRegex.matches(normalized)
    }
}
