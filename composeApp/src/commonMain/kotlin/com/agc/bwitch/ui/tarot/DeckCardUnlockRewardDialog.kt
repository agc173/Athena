package com.agc.bwitch.ui.tarot

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.agc.bwitch.domain.model.DeckCardUnlockReward
import com.agc.bwitch.domain.tarot.TarotDeckId
import com.agc.bwitch.localization.AppStrings
import com.agc.bwitch.ui.common.designsystem.BWitchPrimaryButton
import com.agc.bwitch.ui.common.designsystem.BWitchSecondaryButton
import org.jetbrains.compose.resources.painterResource

@Composable
fun DeckCardUnlockRewardDialog(
    strings: AppStrings,
    rewards: List<DeckCardUnlockReward>,
    onDismiss: () -> Unit,
    onOpenCollection: () -> Unit,
) {
    val firstReward = rewards.firstOrNull() ?: return
    val resolvedDeckId = TarotDeckId.fromValue(firstReward.deckId) ?: TarotDeckRegistry.defaultDeck.id
    val deckName = TarotDeckRegistry.getById(resolvedDeckId)?.displayNameLocalized() ?: resolvedDeckId.value
    val cardArt = TarotCardArt.faceDrawableForCardId(resolvedDeckId, firstReward.cardId) ?: TarotCardArt.backDrawable
    val extraCount = (rewards.size - 1).coerceAtLeast(0)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(strings.horoscope.newDeckCardUnlockedTitle) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    strings.horoscope.newDeckCardUnlockedMessage.replace("{deckName}", deckName),
                    textAlign = TextAlign.Center,
                )
                Image(
                    painter = painterResource(cardArt),
                    contentDescription = null,
                    modifier = Modifier.width(150.dp).aspectRatio(0.62f),
                    contentScale = ContentScale.Crop,
                )
                if (extraCount > 0) {
                    Text(strings.horoscope.unlockedMoreCardsSuffix.replace("%d", extraCount.toString()))
                }
            }
        },
        confirmButton = { BWitchPrimaryButton(onClick = onOpenCollection) { Text(strings.horoscope.viewCollectionCta) } },
        dismissButton = { BWitchSecondaryButton(onClick = onDismiss) { Text(strings.horoscope.close) } },
    )
}
