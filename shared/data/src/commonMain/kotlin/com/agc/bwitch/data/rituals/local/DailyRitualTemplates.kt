package com.agc.bwitch.data.rituals.local

import com.agc.bwitch.domain.rituals.DailyRitualStep
import com.agc.bwitch.domain.rituals.DailyRitualStepKind
import com.agc.bwitch.domain.rituals.DailyRitualTemplate
import com.agc.bwitch.domain.rituals.DailyRitualTheme
import com.agc.bwitch.domain.rituals.dailyRitualBranchKey

internal val localDailyRitualTemplates: List<DailyRitualTemplate> = listOf(
    DailyRitualTemplate(
        id = "calm_breath_anchor",
        theme = DailyRitualTheme.Calm,
        titleKey = "daily_ritual.template.calm_breath_anchor.title",
        subtitleKey = "daily_ritual.theme.calm",
        introKey = "daily_ritual.template.calm_breath_anchor.intro",
        estimatedMinutes = 3,
        steps = listOf(
            DailyRitualStep("c1", DailyRitualStepKind.Info, "daily_ritual.step.c1.text"),
            DailyRitualStep("c2", DailyRitualStepKind.Confirmation, "daily_ritual.step.c2.text", ctaLabelKey = "daily_ritual.step.c2.cta"),
            DailyRitualStep("c3", DailyRitualStepKind.TextInput, "daily_ritual.step.c3.text", ctaLabelKey = "daily_ritual.step.c3.cta"),
        ),
        completionMessageKey = "daily_ritual.template.calm_breath_anchor.completion",
    ),
    DailyRitualTemplate(
        id = "calm_soft_body_scan",
        theme = DailyRitualTheme.Calm,
        titleKey = "daily_ritual.template.calm_soft_body_scan.title",
        subtitleKey = "daily_ritual.theme.calm",
        introKey = "daily_ritual.template.calm_soft_body_scan.intro",
        estimatedMinutes = 4,
        steps = listOf(
            DailyRitualStep("c4", DailyRitualStepKind.Info, "daily_ritual.step.c4.text"),
            DailyRitualStep(
                "c5",
                DailyRitualStepKind.SingleChoice,
                "daily_ritual.step.c5.text",
                optionKeys = listOf("neck", "chest", "jaw", "stomach"),
            ),
            DailyRitualStep("c6", DailyRitualStepKind.Confirmation, "daily_ritual.step.c6.text"),
        ),
        completionMessageKey = "daily_ritual.template.calm_soft_body_scan.completion",
    ),
    DailyRitualTemplate(
        id = "clarity_focus_one",
        theme = DailyRitualTheme.Clarity,
        titleKey = "daily_ritual.template.clarity_focus_one.title",
        subtitleKey = "daily_ritual.theme.clarity",
        introKey = "daily_ritual.template.clarity_focus_one.intro",
        estimatedMinutes = 3,
        steps = listOf(
            DailyRitualStep("cl1", DailyRitualStepKind.TextInput, "daily_ritual.step.cl1.text", ctaLabelKey = "daily_ritual.step.cl1.cta"),
            DailyRitualStep(
                "cl2",
                DailyRitualStepKind.SingleChoice,
                "daily_ritual.step.cl2.text",
                optionKeys = listOf("distraction", "fear_of_failure", "lack_of_time", "perfectionism"),
            ),
            DailyRitualStep("cl3", DailyRitualStepKind.Confirmation, "daily_ritual.step.cl3.text", ctaLabelKey = "daily_ritual.step.cl3.cta"),
        ),
        completionMessageKey = "daily_ritual.template.clarity_focus_one.completion",
    ),
    DailyRitualTemplate(
        id = "clarity_boundary",
        theme = DailyRitualTheme.Clarity,
        titleKey = "daily_ritual.template.clarity_boundary.title",
        subtitleKey = "daily_ritual.theme.clarity",
        introKey = "daily_ritual.template.clarity_boundary.intro",
        estimatedMinutes = 4,
        steps = listOf(
            DailyRitualStep("cl4", DailyRitualStepKind.Info, "daily_ritual.step.cl4.text"),
            DailyRitualStep("cl5", DailyRitualStepKind.TextInput, "daily_ritual.step.cl5.text", ctaLabelKey = "daily_ritual.step.cl5.cta"),
            DailyRitualStep("cl6", DailyRitualStepKind.Confirmation, "daily_ritual.step.cl6.text", ctaLabelKey = "daily_ritual.step.cl6.cta"),
        ),
        completionMessageKey = "daily_ritual.template.clarity_boundary.completion",
    ),
    DailyRitualTemplate(
        id = "release_small_let_go",
        theme = DailyRitualTheme.Release,
        titleKey = "daily_ritual.template.release_small_let_go.title",
        subtitleKey = "daily_ritual.theme.release",
        introKey = "daily_ritual.template.release_small_let_go.intro",
        estimatedMinutes = 4,
        steps = listOf(
            DailyRitualStep("r1", DailyRitualStepKind.TextInput, "daily_ritual.step.r1.text", ctaLabelKey = "daily_ritual.step.r1.cta"),
            DailyRitualStep(
                "r2",
                DailyRitualStepKind.BinaryChoice,
                "daily_ritual.step.r2.text",
                optionKeys = listOf("yes", "not_yet"),
            ),
        ),
        branches = mapOf(
            dailyRitualBranchKey("r2", "yes") to listOf(
                DailyRitualStep("r2_yes_1", DailyRitualStepKind.Confirmation, "daily_ritual.step.r2_yes_1.text", ctaLabelKey = "daily_ritual.step.r2_yes_1.cta"),
                DailyRitualStep("r2_yes_2", DailyRitualStepKind.TextInput, "daily_ritual.step.r2_yes_2.text"),
                DailyRitualStep("r2_yes_3", DailyRitualStepKind.Confirmation, "daily_ritual.step.r2_yes_3.text", ctaLabelKey = "daily_ritual.step.r2_yes_3.cta"),
            ),
            dailyRitualBranchKey("r2", "not_yet") to listOf(
                DailyRitualStep("r2_no_1", DailyRitualStepKind.Info, "daily_ritual.step.r2_no_1.text"),
                DailyRitualStep("r2_no_2", DailyRitualStepKind.TextInput, "daily_ritual.step.r2_no_2.text"),
                DailyRitualStep("r2_no_3", DailyRitualStepKind.Confirmation, "daily_ritual.step.r2_no_3.text", ctaLabelKey = "daily_ritual.step.r2_no_3.cta"),
            ),
        ),
        completionMessageKey = "daily_ritual.template.release_small_let_go.completion",
    ),
    DailyRitualTemplate(
        id = "release_close_cycle",
        theme = DailyRitualTheme.Release,
        titleKey = "daily_ritual.template.release_close_cycle.title",
        subtitleKey = "daily_ritual.theme.release",
        introKey = "daily_ritual.template.release_close_cycle.intro",
        estimatedMinutes = 3,
        steps = listOf(
            DailyRitualStep("r4", DailyRitualStepKind.Info, "daily_ritual.step.r4.text"),
            DailyRitualStep("r5", DailyRitualStepKind.TextInput, "daily_ritual.step.r5.text", ctaLabelKey = "daily_ritual.step.r5.cta"),
            DailyRitualStep("r6", DailyRitualStepKind.Confirmation, "daily_ritual.step.r6.text", ctaLabelKey = "daily_ritual.step.r6.cta"),
        ),
        completionMessageKey = "daily_ritual.template.release_close_cycle.completion",
    ),
    DailyRitualTemplate(
        id = "energy_activate_body",
        theme = DailyRitualTheme.Energy,
        titleKey = "daily_ritual.template.energy_activate_body.title",
        subtitleKey = "daily_ritual.theme.energy",
        introKey = "daily_ritual.template.energy_activate_body.intro",
        estimatedMinutes = 3,
        steps = listOf(
            DailyRitualStep(
                "e1",
                DailyRitualStepKind.SingleChoice,
                "daily_ritual.step.e1.text",
                optionKeys = listOf("focus", "motivation", "courage", "joy"),
            ),
            DailyRitualStep("e2", DailyRitualStepKind.Confirmation, "daily_ritual.step.e2.text", ctaLabelKey = "daily_ritual.step.e2.cta"),
            DailyRitualStep("e3", DailyRitualStepKind.TextInput, "daily_ritual.step.e3.text"),
        ),
        completionMessageKey = "daily_ritual.template.energy_activate_body.completion",
    ),
    DailyRitualTemplate(
        id = "energy_spark_start",
        theme = DailyRitualTheme.Energy,
        titleKey = "daily_ritual.template.energy_spark_start.title",
        subtitleKey = "daily_ritual.theme.energy",
        introKey = "daily_ritual.template.energy_spark_start.intro",
        estimatedMinutes = 4,
        steps = listOf(
            DailyRitualStep("e4", DailyRitualStepKind.Info, "daily_ritual.step.e4.text"),
            DailyRitualStep(
                "e5",
                DailyRitualStepKind.BinaryChoice,
                "daily_ritual.step.e5.text",
                optionKeys = listOf("soft", "intense"),
                ctaLabelKey = "daily_ritual.step.e5.cta",
            ),
            DailyRitualStep("e6", DailyRitualStepKind.Confirmation, "daily_ritual.step.e6.text", ctaLabelKey = "daily_ritual.step.e6.cta"),
        ),
        completionMessageKey = "daily_ritual.template.energy_spark_start.completion",
    ),
)
