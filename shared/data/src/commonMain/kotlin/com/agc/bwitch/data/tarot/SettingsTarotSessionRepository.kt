package com.agc.bwitch.data.tarot

import com.agc.bwitch.data.storage.SettingsFactory
import com.agc.bwitch.data.tarot.dto.TarotSessionSnapshotDto
import com.agc.bwitch.domain.tarot.Tarot3CardMeaning
import com.agc.bwitch.domain.tarot.TarotCard
import com.agc.bwitch.domain.tarot.TarotCardPosition
import com.agc.bwitch.domain.tarot.TarotDrawResponse
import com.agc.bwitch.domain.tarot.TarotReadingDetails
import com.agc.bwitch.domain.tarot.TarotRequestType
import com.agc.bwitch.domain.tarot.TarotSessionPhase
import com.agc.bwitch.domain.tarot.TarotSessionRepository
import com.agc.bwitch.domain.tarot.TarotSessionSnapshot
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

class SettingsTarotSessionRepository(
    settingsFactory: SettingsFactory,
    private val json: Json = Json { ignoreUnknownKeys = true },
) : TarotSessionRepository {
    private val settings = settingsFactory.create(SETTINGS_NAME)

    override suspend fun loadSession(): TarotSessionSnapshot? {
        val raw = settings.getStringOrNull(KEY) ?: return null
        val dto = runCatching { json.decodeFromString(TarotSessionSnapshotDto.serializer(), raw) }
            .getOrNull()
            ?: return clearAndNull()

        val now = Clock.System.now().toEpochMilliseconds()
        if (now - dto.createdAtEpochMillis > TTL_MS) return clearAndNull()

        val snapshot = dto.toDomain()
        if (snapshot.phase == TarotSessionPhase.PENDING_BACKEND_REQUEST && snapshot.response == null) {
            return clearAndNull()
        }
        return snapshot
    }

    override suspend fun saveSession(snapshot: TarotSessionSnapshot) {
        settings.putString(KEY, json.encodeToString(TarotSessionSnapshotDto.serializer(), snapshot.toDto()))
    }

    override suspend fun clearSession() {
        settings.remove(KEY)
    }

    private fun clearAndNull(): TarotSessionSnapshot? {
        settings.remove(KEY)
        return null
    }

    private fun TarotSessionSnapshot.toDto(): TarotSessionSnapshotDto = TarotSessionSnapshotDto(
        requestId = requestId,
        type = type.name,
        createdAtEpochMillis = createdAtEpochMillis,
        updatedAtEpochMillis = updatedAtEpochMillis,
        phase = phase.name,
        responseJson = response?.let { json.encodeToString(TarotDrawPersistedDto.serializer(), it.toPersistedDto()) },
        revealPhase = revealPhase,
        revealedCardCount = revealedCardCount,
        activeCardIndex = activeCardIndex,
        activeCardRevealed = activeCardRevealed,
        overlayVisible = overlayVisible,
        overlayCardIndex = overlayCardIndex,
        overlayCardRevealed = overlayCardRevealed,
        openedMiniCardIndex = openedMiniCardIndex,
    )

    private fun TarotSessionSnapshotDto.toDomain(): TarotSessionSnapshot {
        val response = responseJson?.let {
            runCatching { json.decodeFromString(TarotDrawPersistedDto.serializer(), it).toDomain() }.getOrNull()
        }
        return TarotSessionSnapshot(
            requestId = requestId,
            type = runCatching { TarotRequestType.valueOf(type) }.getOrDefault(TarotRequestType.TAROT_1),
            createdAtEpochMillis = createdAtEpochMillis,
            updatedAtEpochMillis = updatedAtEpochMillis,
            phase = runCatching { TarotSessionPhase.valueOf(phase) }.getOrDefault(TarotSessionPhase.PENDING_BACKEND_REQUEST),
            response = response,
            revealPhase = revealPhase,
            revealedCardCount = revealedCardCount,
            activeCardIndex = activeCardIndex,
            activeCardRevealed = activeCardRevealed,
            overlayVisible = overlayVisible,
            overlayCardIndex = overlayCardIndex,
            overlayCardRevealed = overlayCardRevealed,
            openedMiniCardIndex = openedMiniCardIndex,
        )
    }

    @Serializable
    private data class TarotDrawPersistedDto(
        val requestId: String,
        val status: String,
        val cards: List<TarotCardPersistedDto>,
        val details: TarotReadingDetailsPersistedDto? = null,
        val interpretation: String,
    )

    @Serializable
    private data class TarotCardPersistedDto(
        val id: String,
        val name: String,
        val upright: Boolean? = null,
        val position: String? = null,
    )

    @Serializable
    private data class TarotReadingDetailsPersistedDto(
        val kind: String,
        val theme: String? = null,
        val meaning: String? = null,
        val advice: String? = null,
        val watchOut: String? = null,
        val summary: String? = null,
        val cards: List<TarotMeaningPersistedDto> = emptyList(),
    )

    @Serializable
    private data class TarotMeaningPersistedDto(
        val position: String,
        val meaning: String,
    )

    private fun TarotDrawResponse.toPersistedDto() = TarotDrawPersistedDto(
        requestId = requestId,
        status = status,
        cards = cards.map { TarotCardPersistedDto(it.id, it.name, it.upright, it.position?.name) },
        details = when (val d = details) {
            is TarotReadingDetails.Tarot1ReadingDetails -> TarotReadingDetailsPersistedDto(
                kind = "TAROT_1", theme = d.theme, meaning = d.meaning, advice = d.advice, watchOut = d.watchOut
            )
            is TarotReadingDetails.Tarot3ReadingDetails -> TarotReadingDetailsPersistedDto(
                kind = "TAROT_3",
                summary = d.summary,
                advice = d.advice,
                cards = d.cards.map { TarotMeaningPersistedDto(it.position.name, it.meaning) },
            )
            null -> null
        },
        interpretation = interpretation,
    )

    private fun TarotDrawPersistedDto.toDomain() = TarotDrawResponse(
        requestId = requestId,
        status = status,
        cards = cards.map { TarotCard(it.id, it.name, it.upright, it.position?.let { p -> TarotCardPosition.valueOf(p) }) },
        details = when (details?.kind) {
            "TAROT_1" -> TarotReadingDetails.Tarot1ReadingDetails(
                theme = details.theme.orEmpty(),
                meaning = details.meaning.orEmpty(),
                advice = details.advice.orEmpty(),
                watchOut = details.watchOut.orEmpty(),
            )
            "TAROT_3" -> TarotReadingDetails.Tarot3ReadingDetails(
                cards = details.cards.mapNotNull {
                    runCatching { TarotCardPosition.valueOf(it.position) }.getOrNull()?.let { position ->
                        Tarot3CardMeaning(position, it.meaning)
                    }
                },
                summary = details.summary.orEmpty(),
                advice = details.advice.orEmpty(),
            )
            else -> null
        },
        interpretation = interpretation,
    )

    private companion object {
        const val SETTINGS_NAME = "bwitch_tarot_session"
        const val KEY = "tarot_session_v1"
        const val TTL_MS = 24 * 60 * 60 * 1000L
    }
}
