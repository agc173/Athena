package com.agc.bwitch.data.tarot

import com.agc.bwitch.domain.tarot.TarotDeckCollectionProgress
import com.agc.bwitch.domain.tarot.TarotDeckCollectionRepository
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.auth.auth
import dev.gitlive.firebase.firestore.firestore

class FirestoreTarotDeckCollectionRepository : TarotDeckCollectionRepository {
    private val firestore = Firebase.firestore

    override suspend fun getProgressByTrackId(): Map<String, TarotDeckCollectionProgress> {
        val uid = Firebase.auth.currentUser?.uid ?: return emptyMap()
        return runCatching {
            val progressDocs = firestore.collection("users").document(uid)
                .collection("tarotDeckProgress")
                .get()
                .documents

            progressDocs.associate { trackDoc ->
                val unlockedCards = runCatching {
                    firestore.collection("users").document(uid)
                        .collection("tarotDeckProgress").document(trackDoc.id)
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

                trackDoc.id to TarotDeckCollectionProgress(
                    trackId = trackDoc.id,
                    unlockedCards = unlockedCards,
                )
            }
        }.getOrElse {
            emptyMap()
        }
    }
}
