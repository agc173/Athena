package com.agc.bwitch.domain.localization

enum class AppLanguage(
    val code: String,
    val nativeLabel: String,
) {
    Spanish(code = "es", nativeLabel = "Español"),
    English(code = "en", nativeLabel = "English"),
    Portuguese(code = "pt", nativeLabel = "Português"),
    Russian(code = "ru", nativeLabel = "Русский"),
    French(code = "fr", nativeLabel = "Français"),
    Italian(code = "it", nativeLabel = "Italiano"),
    German(code = "de", nativeLabel = "Deutsch");

    companion object {
        val supported: List<AppLanguage> = listOf(
            Spanish,
            English,
            Portuguese,
            Russian,
            French,
            Italian,
            German,
        )

        val fallback: AppLanguage = English

        fun fromCodeOrNull(rawCode: String?): AppLanguage? {
            val normalized = rawCode
                ?.trim()
                ?.lowercase()
                ?.substringBefore('-')
                ?.substringBefore('_')
                ?: return null

            return supported.firstOrNull { it.code == normalized }
        }
    }
}
