package com.agc.bwitch.data.tarot

import com.agc.bwitch.domain.tarot.TarotDeckCollectionProgress
import com.agc.bwitch.domain.tarot.TarotDeckCollectionRepository
import com.agc.bwitch.domain.tarot.TarotDeckId
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore

class FirestoreTarotDeckCollectionRepository : TarotDeckCollectionRepository {
    private val firestore = Firebase.firestore

    override suspend fun getProgressByTrackId(): Map<String, TarotDeckCollectionProgress> {
        val uid = Firebase.auth.currentUser?.uid ?: error("Tarot deck progress requires an authenticated user.")
        val knownTrackIds = TarotDeckId.entries.map { it.value }

        return knownTrackIds.associateWith { trackId ->
            val unlockedCards = firestore.collection("users").document(uid)
                .collection("tarotDeckProgress").document(trackId)
                .collection("unlockedCards")
                .get()
                .documents
                .map { cardDoc ->
                    (runCatching { cardDoc.get("cardId") as? String }.getOrNull())
                        ?.takeIf { it.isNotBlank() }
                        ?: cardDoc.id
                }
                .toSet()

            TarotDeckCollectionProgress(
                trackId = trackId,
                unlockedCards = unlockedCards,
            )
        }
    }
}
