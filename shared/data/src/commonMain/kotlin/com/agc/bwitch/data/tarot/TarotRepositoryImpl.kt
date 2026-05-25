package com.agc.bwitch.data.tarot

import com.agc.bwitch.data.functions.FunctionsClient
import com.agc.bwitch.data.platform.BuildInfo
import com.agc.bwitch.data.tarot.dto.AdUnlockDto
import com.agc.bwitch.data.tarot.dto.DrawDto
import com.agc.bwitch.data.tarot.dto.ReadingDto
import com.agc.bwitch.data.tarot.dto.TarotDrawRequestDto
import com.agc.bwitch.data.tarot.dto.TarotDrawResponseDto
import com.agc.bwitch.domain.shared.ApiError
import com.agc.bwitch.domain.shared.ApiResult
import com.agc.bwitch.domain.model.DeckCardUnlockReward
import com.agc.bwitch.domain.tarot.Tarot3CardMeaning
import com.agc.bwitch.domain.tarot.TarotCard
import com.agc.bwitch.domain.tarot.TarotCardPosition
import com.agc.bwitch.domain.tarot.TarotDeckId
import com.agc.bwitch.domain.tarot.TarotDrawResponse
import com.agc.bwitch.domain.tarot.TarotReadingDetails
import com.agc.bwitch.domain.tarot.TarotRepository
import com.agc.bwitch.domain.tarot.TarotRequestType

class TarotRepositoryImpl(
    private val functionsClient: FunctionsClient,
) : TarotRepository {

    override suspend fun tarotDraw(
        requestId: String,
        type: TarotRequestType,
        lang: String?,
        question: String?,
    ): ApiResult<TarotDrawResponse> {
        val normalizedLang = lang
            ?.trim()
            ?.takeIf { it.isNotEmpty() }

        val payload = TarotDrawRequestDto(
            requestType = type.name,
            requestId = requestId,
            lang = normalizedLang,
            question = question,
            adUnlock = if (BuildInfo.isDebug) AdUnlockDto(rewardedProof = "dev-test-proof") else null,
        )

        return when (
            val result = functionsClient.call(
                name = "tarotDraw",
                data = payload,
                requestSerializer = TarotDrawRequestDto.serializer(),
                responseSerializer = TarotDrawResponseDto.serializer(),
            )
        ) {
            is ApiResult.Err -> {
                println(
                    "BWITCH_TAROT callable=tarotDraw requestType=${type.name} requestId=$requestId error=${result.error::class.simpleName} message=${result.error.message}"
                )
                result
            }
            is ApiResult.Ok -> parseResponse(result.value)
        }
    }

    private fun parseResponse(response: TarotDrawResponseDto): ApiResult<TarotDrawResponse> {
        if (response.status == "IN_PROGRESS" || response.status == "PROCESSING") {
            return ApiResult.Ok(
                TarotDrawResponse(
                    requestId = response.requestId,
                    status = response.status,
                    cards = emptyList(),
                    details = null,
                    interpretation = "",
                    deckId = response.deckId.toDeckIdOrDefault(),
                    deckCardUnlockRewards = response.deckCardUnlockRewards.map { it.toDomain() },
                )
            )
        }

        if (response.status == "FAILED") {
            return ApiResult.Err(ApiError.Internal(response.error?.message ?: "Request failed"))
        }

        val draw = response.draw
            ?: return ApiResult.Err(ApiError.Internal("Invalid response: missing draw"))
        val reading = response.reading
            ?: return ApiResult.Err(ApiError.Internal("Invalid response: missing reading"))

        val cards = parseCards(
            draw = draw,
            requestTypeHint = response.requestType,
            readingTypeHint = reading.type,
        ) ?: return ApiResult.Err(ApiError.Internal("Invalid response: invalid draw"))

        val details = parseReadingDetails(reading)
            ?: return ApiResult.Err(ApiError.Internal("Invalid response: invalid reading"))

        return ApiResult.Ok(
            TarotDrawResponse(
                requestId = response.requestId,
                status = response.status,
                cards = cards,
                details = details,
                interpretation = details.toInterpretationText(),
                deckId = response.deckId.toDeckIdOrDefault(),
                deckCardUnlockRewards = response.deckCardUnlockRewards.map { it.toDomain() },
            )
        )
    }

    private fun parseCards(
        draw: DrawDto,
        requestTypeHint: String? = null,
        readingTypeHint: String? = null,
    ): List<TarotCard>? {
        val inferredType = draw.type
            ?: requestTypeHint
            ?: readingTypeHint
            ?: when {
                draw.card != null -> TarotRequestType.TAROT_1.name
                !draw.cards.isNullOrEmpty() -> TarotRequestType.TAROT_3.name
                else -> null
            }

        return when (inferredType) {
            TarotRequestType.TAROT_1.name -> {
                val card = draw.card ?: return null
                val upright = card.orientation.toUpright()
                listOf(TarotCard(id = card.id, name = card.name, upright = upright))
            }

            TarotRequestType.TAROT_3.name -> {
                val cards = draw.cards ?: return null
                cards.map { card ->
                    TarotCard(
                        id = card.id,
                        name = card.name,
                        upright = card.orientation.toUpright(),
                        position = card.position.toCardPosition(),
                    )
                }
            }

            else -> null
        }
    }

    private fun parseReadingDetails(reading: ReadingDto): TarotReadingDetails? {
        return when (reading.type) {
            TarotRequestType.TAROT_1.name -> {
                val interpretation = reading.interpretation ?: return null
                val theme = interpretation.theme ?: return null
                val meaning = interpretation.meaning ?: return null
                val advice = interpretation.advice ?: return null
                val watchOut = interpretation.watchOut ?: return null

                TarotReadingDetails.Tarot1ReadingDetails(
                    theme = theme,
                    meaning = meaning,
                    advice = advice,
                    watchOut = watchOut,
                )
            }

            TarotRequestType.TAROT_3.name -> {
                val summary = reading.summary ?: return null
                val advice = reading.advice ?: return null
                val cards = reading.cards.orEmpty().mapNotNull { card ->
                    val position = card.position.toCardPosition() ?: return@mapNotNull null
                    val meaning = card.meaning ?: return@mapNotNull null
                    Tarot3CardMeaning(position = position, meaning = meaning)
                }

                TarotReadingDetails.Tarot3ReadingDetails(
                    cards = cards,
                    summary = summary,
                    advice = advice,
                )
            }

            else -> null
        }
    }

    private fun TarotReadingDetails.toInterpretationText(): String {
        return when (this) {
            is TarotReadingDetails.Tarot1ReadingDetails -> listOf(
                theme,
                meaning,
                advice,
                watchOut,
            ).joinToString(separator = "\n\n")

            is TarotReadingDetails.Tarot3ReadingDetails -> {
                val cardMeanings = cards
                    .joinToString(separator = "\n") { "${it.position.toLabel()}: ${it.meaning}" }
                listOf(summary, cardMeanings, advice)
                    .filter { it.isNotBlank() }
                    .joinToString(separator = "\n\n")
            }
        }
    }

    private fun String?.toUpright(): Boolean? = when (this) {
        "upright" -> true
        "reversed" -> false
        else -> null
    }

    private fun String?.toCardPosition(): TarotCardPosition? = when (this) {
        "past" -> TarotCardPosition.PAST
        "present" -> TarotCardPosition.PRESENT
        "future" -> TarotCardPosition.FUTURE
        else -> null
    }

    private fun TarotCardPosition.toLabel(): String = when (this) {
        TarotCardPosition.PAST -> "past"
        TarotCardPosition.PRESENT -> "present"
        TarotCardPosition.FUTURE -> "future"
    }
}

private fun com.agc.bwitch.data.remote.economy.DeckCardUnlockRewardDto.toDomain(): DeckCardUnlockReward =
    DeckCardUnlockReward(deckId = deckId, trackId = trackId, rewardPoolId = rewardPoolId, cardId = cardId)

private fun String?.toDeckIdOrDefault(): TarotDeckId = TarotDeckId.fromValue(this) ?: TarotDeckId.RIDER_WAITE
