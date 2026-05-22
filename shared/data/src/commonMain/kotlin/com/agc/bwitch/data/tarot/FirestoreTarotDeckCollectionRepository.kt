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
        val uid = Firebase.auth.currentUser?.uid ?: return emptyMap()
        return runCatching {
            val progressCollection = firestore.collection("users").document(uid)
                .collection("tarotDeckProgress")
            val progressDocs = progressCollection
                .get()
                .documents
            val knownTrackIds = TarotDeckId.entries.map { it.value }.toSet()
            val trackIdsToRead = (progressDocs.map { it.id } + knownTrackIds).toSet()

            println("[TarotCollectionRepository] read progress root path=users/$uid/tarotDeckProgress trackIds=$trackIdsToRead")

            trackIdsToRead.associate { trackId ->
                val unlockedCardsPath = "users/$uid/tarotDeckProgress/$trackId/unlockedCards"
                val unlockedCards = runCatching {
                    firestore.collection("users").document(uid)
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
                }.getOrDefault(emptySet())
                println("[TarotCollectionRepository] read unlockedCards path=$unlockedCardsPath trackId=$trackId unlockedCount=${unlockedCards.size}")

                trackId to TarotDeckCollectionProgress(
                    trackId = trackId,
                    unlockedCards = unlockedCards,
                )
            }
        }.getOrElse {
            emptyMap()
        }
    }
}
