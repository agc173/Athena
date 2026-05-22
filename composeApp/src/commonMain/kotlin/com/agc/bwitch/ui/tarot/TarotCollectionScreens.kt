package com.agc.bwitch.ui.tarot

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items as lazyItems
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.RoundedCornerShape
import com.agc.bwitch.domain.tarot.TarotDeckId
import com.agc.bwitch.localization.appStrings
import com.agc.bwitch.presentation.tarotcollection.TarotCollectionViewModel
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.koinInject

@Composable
fun TarotCollectionScreen(contentPadding: PaddingValues, onOpenDeck: (String) -> Unit) {
    val vm: TarotCollectionViewModel = koinInject()
    val state by vm.uiState.collectAsState()
    val strings = appStrings.profile
    LaunchedEffect(Unit) { vm.load(); vm.onGalleryOpened() }
    LazyColumn(
        modifier = Modifier.padding(contentPadding).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        lazyItems(TarotDeckRegistry.allDecks, key = { it.id.value }) { deck ->
            val progress = state.progressByTrackId[deck.progressTrackId]
            val unlocked = if (deck.isDefault) {
                TarotCardArt.allCardIds(deck.id).size
            } else {
                progress?.unlockedCards?.size ?: 0
            }
            val total = TarotCardArt.allCardIds(deck.id).size
            val status = when {
                deck.isDefault -> strings.arcanaDeckStatusAvailable
                unlocked >= total && total > 0 -> strings.arcanaDeckStatusCompleted
                else -> strings.arcanaDeckStatusInProgress
            }
            Surface(Modifier.fillMaxWidth().clickable { onOpenDeck(deck.id.value) }) {
                Column(Modifier.padding(12.dp)) {
                    val preview = TarotCardArt.faceDrawableForCardId(deck.id, deck.previewCardId) ?: TarotCardArt.backDrawable
                    Image(
                        painter = painterResource(preview),
                        contentDescription = null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(3f / 5f)
                            .clip(RoundedCornerShape(12.dp)),
                        contentScale = ContentScale.Crop,
                    )
                    Text(deck.displayNameLocalized(), style = MaterialTheme.typography.titleMedium)
                    Text("$unlocked / $total · $status", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

@Composable
fun TarotDeckDetailScreen(contentPadding: PaddingValues, deckRawId: String) {
    val vm: TarotCollectionViewModel = koinInject()
    val state by vm.uiState.collectAsState()
    val strings = appStrings.profile
    val deck = TarotDeckId.fromValue(deckRawId) ?: TarotDeckRegistry.defaultDeck.id
    val cards = TarotCardArt.allCardIds(deck)
    val trackId = TarotDeckRegistry.getById(deck)?.progressTrackId ?: deck.value
    val deckDefinition = TarotDeckRegistry.getById(deck)
    val unlocked = if (deckDefinition?.isDefault == true) {
        cards.toSet()
    } else {
        state.progressByTrackId[trackId]?.unlockedCards ?: emptySet()
    }
    val isFullyUnlocked = cards.isNotEmpty() && unlocked.size >= cards.size
    val canUseDeck = deckDefinition?.isDefault == true || isFullyUnlocked
    val isSelectedDeck = state.selectedDeckId == deck
    LaunchedEffect(deckRawId) { vm.load(); vm.onDeckDetailOpened(deckRawId) }
    Column(Modifier.padding(contentPadding).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(deckDefinition?.displayNameLocalized() ?: deck.value, style = MaterialTheme.typography.titleLarge)
        Text("${unlocked.size} / ${cards.size}")
        Text(strings.arcanaUnlockedCardsLabel + ": ${unlocked.size}")
        Text(strings.arcanaDeckRevealCopy)
        Button(onClick = { vm.selectDeck(deck, isFullyUnlocked) }, enabled = canUseDeck) {
            Text(if (isSelectedDeck) appStrings.tarot.deckInUse else appStrings.tarot.useThisDeck)
        }
        if (!canUseDeck) {
            Text(appStrings.tarot.completeDeckToUseReadings, style = MaterialTheme.typography.bodySmall)
        }
        if (isSelectedDeck) {
            Text(appStrings.tarot.selectedDeckLabel, style = MaterialTheme.typography.bodySmall)
        }
        LazyVerticalGrid(columns = GridCells.Fixed(3), verticalArrangement = Arrangement.spacedBy(8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            gridItems(cards) { cardId ->
                val image = TarotCardArt.faceDrawableForCardId(deck, cardId)
                if (image != null) {
                    Image(painterResource(image), null, colorFilter = if (unlocked.contains(cardId)) null else ColorFilter.tint(MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f)))
                }
            }
        }
    }
}

private fun com.agc.bwitch.domain.tarot.TarotDeckDefinition.displayNameLocalized(): String =
    when (id) {
        TarotDeckId.RIDER_WAITE -> "Rider-Waite"
        TarotDeckId.ARCANA_NOCTIS -> "Arcana Noctis"
    }
