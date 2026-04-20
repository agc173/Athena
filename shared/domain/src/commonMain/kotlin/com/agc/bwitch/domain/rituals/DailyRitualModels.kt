package com.agc.bwitch.domain.rituals

enum class DailyRitualTheme {
    Calm,
    Clarity,
    Release,
    Energy,
}

enum class DailyRitualStepKind {
    Info,
    TextInput,
    SingleChoice,
    BinaryChoice,
    Confirmation,
}

data class DailyRitualStep(
    val id: String,
    val kind: DailyRitualStepKind,
    val textKey: String,
    val optionKeys: List<String> = emptyList(),
    val ctaLabelKey: String = "daily_ritual.cta.continue",
)

data class DailyRitualTemplate(
    val id: String,
    val theme: DailyRitualTheme,
    val titleKey: String,
    val subtitleKey: String,
    val introKey: String,
    val estimatedMinutes: Int,
    val steps: List<DailyRitualStep>,
    val branches: Map<String, List<DailyRitualStep>> = emptyMap(),
    val completionMessageKey: String,
)

fun dailyRitualBranchKey(stepId: String, optionKey: String): String = "${stepId}:${optionKey.trim()}"
