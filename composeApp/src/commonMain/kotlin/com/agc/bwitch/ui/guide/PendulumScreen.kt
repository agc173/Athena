package com.agc.bwitch.ui.guide

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import bwitch.composeapp.generated.resources.Res
import bwitch.composeapp.generated.resources.pendulum_board
import bwitch.composeapp.generated.resources.pendulum_crystal
import com.agc.bwitch.domain.pendulum.PendulumAnswer
import com.agc.bwitch.localization.PendulumStrings
import com.agc.bwitch.localization.appStrings
import com.agc.bwitch.presentation.pendulum.PendulumPhase
import com.agc.bwitch.presentation.pendulum.PendulumViewModel
import com.agc.bwitch.presentation.economy.EconomyViewModel
import com.agc.bwitch.presentation.economy.runWithEconomyGate
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject
import com.agc.bwitch.ui.common.economy.DailyLimitPaywallCard
import com.agc.bwitch.ui.common.economy.EconomyGateInfoRow
import com.agc.bwitch.ui.common.economy.isDailyLimitRejected

@Composable
fun PendulumScreen(
    contentPadding: PaddingValues,
    viewModel: PendulumViewModel = koinInject(),
    economyViewModel: EconomyViewModel = koinInject(),
    onOpenStore: () -> Unit = {},
) {
    val state by viewModel.uiState.collectAsState()
    val orbitProgress = remember { Animatable(0f) }
    val economyState by economyViewModel.uiState.collectAsState()
    val pendulumPreview = economyState.modulePreviews.firstOrNull { it.module == "PENDULUM" }
    val colors = MaterialTheme.colorScheme
    val showDailyLimitPaywall = pendulumPreview.isDailyLimitRejected() || state.error == "daily_limit"
    LaunchedEffect(state.phase, state.selectedAnswer) {
        when (state.phase) {
            PendulumPhase.ANIMATING -> {
                orbitProgress.snapTo(0f)
                orbitProgress.animateTo(
                    targetValue = 1f,
                    animationSpec = tween(durationMillis = 4100, easing = LinearOutSlowInEasing),
                )
                viewModel.onSwingFinished()
            }

            PendulumPhase.IDLE -> orbitProgress.animateTo(
                targetValue = 0f,
                animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing),
            )

            PendulumPhase.RESULT -> Unit
        }
    }

    val isAnimating = state.phase == PendulumPhase.ANIMATING
    val strings = appStrings.pendulum

    LaunchedEffect(state.phase, state.error) {
        if (state.phase == PendulumPhase.RESULT || state.error == "insufficient_moons" || state.error == "daily_limit") {
            economyViewModel.loadEconomy()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        colors.background,
                        colors.surface,
                        colors.background.copy(alpha = 0.94f),
                    ),
                ),
            )
            .padding(contentPadding)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {

        Text(
            strings.instruction,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
        )

        OutlinedTextField(
            value = state.question,
            onValueChange = viewModel::onQuestionChange,
            modifier = Modifier.fillMaxWidth(),
            label = { Text(strings.questionLabel) },
            enabled = !isAnimating,
        )

        EconomyGateInfoRow(
            preview = pendulumPreview,
            economyStrings = appStrings.economy,
            fallbackCost = 1,
            packUsesLabel = appStrings.economy.pendulumPackValueFormat,
        )

        if (showDailyLimitPaywall) {
            DailyLimitPaywallCard(
                economyStrings = appStrings.economy,
                onOpenStore = onOpenStore,
            )
        }

        PendulumBoard(
            modifier = Modifier
                .fillMaxWidth()
                .height(380.dp)
                .padding(horizontal = 2.dp)
                .clickable(
                    enabled = !isAnimating,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {
                    runWithEconomyGate(pendulumPreview, economyViewModel, "pendulum", 1) { viewModel.startSwing() }
                },
            phase = state.phase,
            selectedAnswer = state.selectedAnswer,
            animationProgress = orbitProgress.value,
            strings = strings,
        )

        state.error?.takeUnless { it == "daily_limit" }?.let {
            Text(
                text = appStrings.economy.notEnoughMoons,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
            )
        }

        if (state.phase == PendulumPhase.RESULT) {
            state.selectedAnswer?.let { answer ->
                Text(
                    text = answer.mysticalMessage(strings),
                    style = MaterialTheme.typography.titleMedium,
                    fontStyle = FontStyle.Italic,
                )
                Text(
                    text = strings.resultSentenceFormat.withTexts(strings.resultPrefix, answer.label(strings)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (state.phase == PendulumPhase.RESULT) {
            Button(
                onClick = viewModel::reset,
                enabled = !isAnimating,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(strings.resetCta)
            }
        }
    }
}

@Composable
private fun PendulumBoard(
    modifier: Modifier,
    phase: PendulumPhase,
    selectedAnswer: PendulumAnswer?,
    animationProgress: Float,
    strings: PendulumStrings,
) {
    var boardSize by remember { mutableStateOf(IntSize.Zero) }
    val boardImageScale = 0.98f
    val boardMinDimension = min(boardSize.width.toFloat(), boardSize.height.toFloat()).coerceAtLeast(1f)
    val boardRadiusPx = min(boardSize.width.toFloat(), boardSize.height.toFloat()) * 0.40f
    val crystalOffsetPx = crystalOffsetFor(
        phase = phase,
        selectedAnswer = selectedAnswer,
        animationProgress = animationProgress,
        boardRadius = boardRadiusPx,
    )

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .onSizeChanged { boardSize = it },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color(0xFF3B2A5A).copy(alpha = 0.40f),
                            Color(0xFF120D1F).copy(alpha = 0.04f),
                            Color.Transparent,
                        ),
                        radius = boardMinDimension * 0.6f,
                    ),
                ),
        )

        Image(
            painter = painterResource(Res.drawable.pendulum_board),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(boardImageScale),
        )

        AnswerMarker(
            text = strings.answerNo,
            isSelected = phase == PendulumPhase.RESULT && selectedAnswer == PendulumAnswer.NO,
            offsetX = (-90).dp,
            offsetY = (-80).dp,
        )
        AnswerMarker(
            text = strings.answerYes,
            isSelected = phase == PendulumPhase.RESULT && selectedAnswer == PendulumAnswer.YES,
            offsetX = 90.dp,
            offsetY = (-80).dp,
        )
        AnswerMarker(
            text = strings.answerNotNow,
            isSelected = phase == PendulumPhase.RESULT && selectedAnswer == PendulumAnswer.NOT_NOW,
            offsetX = (-90).dp,
            offsetY = 80.dp,
        )
        AnswerMarker(
            text = strings.answerMaybe,
            isSelected = phase == PendulumPhase.RESULT && selectedAnswer == PendulumAnswer.MAYBE,
            offsetX = 90.dp,
            offsetY = 80.dp,
        )

        Image(
            painter = painterResource(Res.drawable.pendulum_crystal),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .align(Alignment.Center)
                .offset {
                    IntOffset(
                        x = crystalOffsetPx.x.roundToInt(),
                        y = crystalOffsetPx.y.roundToInt(),
                    )
                }
                .size(62.dp),
        )
    }
}

@Composable
private fun AnswerMarker(
    text: String,
    isSelected: Boolean,
    offsetX: Dp,
    offsetY: Dp,
) {
    val mysticTextColor = Color(0xFFF4E5BC)
    val selectedMysticTextColor = Color(0xFFFFF4D8)

    Box(
        modifier = Modifier
            .offset(x = offsetX, y = offsetY)
            .size(width = 100.dp, height = 42.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall.copy(
                shadow = Shadow(
                    color = Color.Black.copy(alpha = if (isSelected) 0.85f else 0.72f),
                    offset = Offset(0f, 1.8f),
                    blurRadius = if (isSelected) 8f else 5.5f,
                ),
            ),
            fontFamily = FontFamily.Serif,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.SemiBold,
            letterSpacing = if (isSelected) 1.6.sp else 1.1.sp,
            color = if (isSelected) selectedMysticTextColor else mysticTextColor,
        )
    }
}

private fun crystalOffsetFor(
    phase: PendulumPhase,
    selectedAnswer: PendulumAnswer?,
    animationProgress: Float,
    boardRadius: Float,
): Offset {
    val target = answerTarget(selectedAnswer, boardRadius)

    if (phase == PendulumPhase.IDLE || selectedAnswer == null) return Offset.Zero
    if (phase == PendulumPhase.RESULT) return target

    val t = animationProgress.coerceIn(0f, 1f)
    val orbitCutoff = 0.70f
    val settleCutoff = 0.88f
    val preferredTarget = target * 0.82f

    return if (t <= orbitCutoff) {
        orbitPosition(t = t / orbitCutoff, boardRadius = boardRadius)
    } else if (t <= settleCutoff) {
        val local = ((t - orbitCutoff) / (settleCutoff - orbitCutoff)).coerceIn(0f, 1f)
        val eased = 1f - (1f - local).pow(2.5f)
        val approach = lerpOffset(
            start = orbitPosition(1f, boardRadius),
            end = preferredTarget,
            t = eased,
        )
        val targetAngle = atan2(target.y, target.x)
        val swirlRadius = boardRadius * 0.095f * (1f - local).pow(1.35f)
        approach + Offset(
            x = cos((local * 2.2f * 2f * PI).toFloat() + targetAngle) * swirlRadius,
            y = sin((local * 2.2f * 2f * PI).toFloat() + targetAngle) * swirlRadius,
        )
    } else {
        val local = ((t - settleCutoff) / (1f - settleCutoff)).coerceIn(0f, 1f)
        val eased = 1f - (1f - local).pow(3.6f)
        val settle = lerpOffset(
            start = preferredTarget,
            end = target,
            t = eased,
        )
        val targetAngle = atan2(target.y, target.x)
        val driftRadius = boardRadius * 0.028f * (1f - eased).pow(1.6f)
        settle + Offset(
            x = cos((local * 1.25f * 2f * PI).toFloat() + targetAngle + 0.45f) * driftRadius,
            y = sin((local * 1.25f * 2f * PI).toFloat() + targetAngle + 0.45f) * driftRadius,
        )
    }
}

private fun orbitPosition(t: Float, boardRadius: Float): Offset {
    val easedT = t.coerceIn(0f, 1f)
    val loops = 2.85f
    val angleBase = easedT * loops * 2f * PI
    val angleWobble = sin(easedT * 2.4f * PI) * 0.34f
    val angle = (angleBase + angleWobble).toFloat()

    val contraction = easedT.pow(1.28f)
    val startRadius = boardRadius * 0.44f
    val endRadius = boardRadius * 0.16f
    val radius = startRadius - (startRadius - endRadius) * contraction
    val noiseX = cos((easedT * 6.3f * PI).toFloat() + 0.8f) * boardRadius * 0.028f * (1f - easedT)
    val noiseY = sin((easedT * 5.1f * PI).toFloat() - 0.45f) * boardRadius * 0.024f * (1f - easedT)

    return Offset(
        x = cos(angle) * radius + noiseX,
        y = sin(angle) * radius + noiseY,
    )
}

private fun answerTarget(answer: PendulumAnswer?, boardRadius: Float): Offset {
    val diagonal = boardRadius * 0.56f
    return when (answer) {
        PendulumAnswer.NO -> Offset(-diagonal, -diagonal)
        PendulumAnswer.YES -> Offset(diagonal, -diagonal)
        PendulumAnswer.NOT_NOW -> Offset(-diagonal, diagonal)
        PendulumAnswer.MAYBE -> Offset(diagonal, diagonal)
        null -> Offset.Zero
    }
}

private fun lerpOffset(start: Offset, end: Offset, t: Float): Offset = Offset(
    x = start.x + (end.x - start.x) * t,
    y = start.y + (end.y - start.y) * t,
)

private fun PendulumAnswer.label(strings: PendulumStrings): String = when (this) {
    PendulumAnswer.YES -> strings.answerYes
    PendulumAnswer.NO -> strings.answerNo
    PendulumAnswer.MAYBE -> strings.answerMaybe
    PendulumAnswer.NOT_NOW -> strings.answerNotNow
}

private fun PendulumAnswer.mysticalMessage(strings: PendulumStrings): String = when (this) {
    PendulumAnswer.YES -> strings.messageYes
    PendulumAnswer.NO -> strings.messageNo
    PendulumAnswer.MAYBE -> strings.messageMaybe
    PendulumAnswer.NOT_NOW -> strings.messageNotNow
}

private fun String.withTexts(vararg values: String): String {
    var resolved = this
    values.forEach { value ->
        resolved = resolved.replaceFirst("%s", value)
    }
    return resolved
}
