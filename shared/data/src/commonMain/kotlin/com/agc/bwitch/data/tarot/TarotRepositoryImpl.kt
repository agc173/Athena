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
import com.agc.bwitch.domain.tarot.TarotCard
import com.agc.bwitch.domain.tarot.TarotDrawResponse
import com.agc.bwitch.domain.tarot.TarotReading
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
        val payload = TarotDrawRequestDto(
            requestType = type.name,
            requestId = requestId,
            lang = lang,
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
            is ApiResult.Err -> result
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
                    interpretation = "",
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

        val parsedReading = parseReading(reading)
            ?: return ApiResult.Err(ApiError.Internal("Invalid response: invalid reading"))

        return ApiResult.Ok(
            TarotDrawResponse(
                requestId = response.requestId,
                status = response.status,
                cards = cards,
                interpretation = parsedReading.text,
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
                    )
                }
            }

            else -> null
        }
    }

    private fun parseReading(reading: ReadingDto): TarotReading? {
        return when (reading.type) {
            TarotRequestType.TAROT_1.name -> {
                val interpretation = reading.interpretation ?: return null
                val parts = listOfNotNull(
                    interpretation.theme,
                    interpretation.meaning,
                    interpretation.advice,
                    interpretation.watchOut,
                )
                if (parts.isEmpty()) return null
                TarotReading(parts.joinToString(separator = "\n\n"))
            }

            TarotRequestType.TAROT_3.name -> {
                val summary = reading.summary
                val advice = reading.advice
                val cardMeanings = reading.cards
                    .orEmpty()
                    .mapNotNull { card ->
                        val position = card.position
                        val meaning = card.meaning
                        if (position != null && meaning != null) "$position: $meaning" else null
                    }

                val text = buildList {
                    summary?.let(::add)
                    if (cardMeanings.isNotEmpty()) add(cardMeanings.joinToString("\n"))
                    advice?.let(::add)
                }.joinToString(separator = "\n\n")

                if (text.isBlank()) return null
                TarotReading(text)
            }

            else -> null
        }
    }

    private fun String?.toUpright(): Boolean? = when (this) {
        "upright" -> true
        "reversed" -> false
        else -> null
    }
}
