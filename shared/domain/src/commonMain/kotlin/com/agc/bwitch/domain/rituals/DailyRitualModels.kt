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
    val text: String,
    val options: List<String> = emptyList(),
    val ctaLabel: String = "Continuar",
)

data class DailyRitualTemplate(
    val id: String,
    val theme: DailyRitualTheme,
    val title: String,
    val subtitle: String,
    val intro: String,
    val estimatedMinutes: Int,
    val steps: List<DailyRitualStep>,
    val branches: Map<String, List<DailyRitualStep>> = emptyMap(),
    val completionMessage: String,
)

fun dailyRitualBranchKey(stepId: String, option: String): String = "${stepId}:${option.trim()}"
