package com.agc.bwitch.data.tarot

import com.agc.bwitch.domain.shared.ApiError
import com.agc.bwitch.domain.shared.ApiResult
import com.agc.bwitch.data.functions.FunctionsClient
import com.agc.bwitch.data.platform.BuildInfo
import com.agc.bwitch.domain.tarot.TarotCard
import com.agc.bwitch.domain.tarot.TarotDrawResponse
import com.agc.bwitch.domain.tarot.TarotReading
import com.agc.bwitch.domain.tarot.TarotRepository
import com.agc.bwitch.domain.tarot.TarotRequestType
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull

class TarotRepositoryImpl(
    private val functionsClient: FunctionsClient,
) : TarotRepository {

    override suspend fun tarotDraw(
        requestId: String,
        type: TarotRequestType,
        lang: String?,
        question: String?,
    ): ApiResult<TarotDrawResponse> {
        val payload = buildJsonObject {
            put("requestType", JsonPrimitive(type.name))
            put("requestId", JsonPrimitive(requestId))
            lang?.let { put("lang", JsonPrimitive(it)) }
            question?.let { put("question", JsonPrimitive(it)) }
            // Temporary dev-only hack until real rewarded ad proof / SSV validation is implemented.
            if (BuildInfo.isDebug) {
                put("adUnlock", buildJsonObject { put("rewardedProof", JsonPrimitive("dev-test-proof")) })
            }
        }

        return when (val result = functionsClient.call("tarotDraw", payload)) {
            is ApiResult.Err -> result
            is ApiResult.Ok -> parseResponse(result.value)
        }
    }

    private fun parseResponse(response: JsonObject): ApiResult<TarotDrawResponse> {
        val requestId = response.string("requestId")
            ?: return ApiResult.Err(ApiError.Internal("Invalid response: missing requestId"))
        val status = response.string("status")
            ?: return ApiResult.Err(ApiError.Internal("Invalid response: missing status"))

        if (status == "IN_PROGRESS" || status == "PROCESSING") {
            return ApiResult.Ok(
                TarotDrawResponse(
                    requestId = requestId,
                    status = status,
                    cards = emptyList(),
                    interpretation = "",
                )
            )
        }

        if (status == "FAILED") {
            val errorMessage = response.obj("error")?.string("message")
            return ApiResult.Err(ApiError.Internal(errorMessage ?: "Request failed"))
        }

        val draw = response.obj("draw")
            ?: return ApiResult.Err(ApiError.Internal("Invalid response: missing draw"))
        val reading = response.obj("reading")
            ?: return ApiResult.Err(ApiError.Internal("Invalid response: missing reading"))

        val cards = parseCards(
            draw = draw,
            requestTypeHint = response.string("requestType"),
            readingTypeHint = reading.string("type"),
        ) ?: return ApiResult.Err(ApiError.Internal("Invalid response: invalid draw"))

        val parsedReading = parseReading(reading)
            ?: return ApiResult.Err(ApiError.Internal("Invalid response: invalid reading"))

        return ApiResult.Ok(
            TarotDrawResponse(
                requestId = requestId,
                status = status,
                cards = cards,
                interpretation = parsedReading.text,
            )
        )
    }

    private fun parseCards(
        draw: JsonObject,
        requestTypeHint: String? = null,
        readingTypeHint: String? = null,
    ): List<TarotCard>? {
        val inferredType = draw.string("type")
            ?: requestTypeHint
            ?: readingTypeHint
            ?: when {
                draw.obj("card") != null -> TarotRequestType.TAROT_1.name
                draw.arr("cards") != null -> TarotRequestType.TAROT_3.name
                else -> null
            }

        return when (inferredType) {
            TarotRequestType.TAROT_1.name -> {
                val card = draw.obj("card") ?: return null
                val id = card.string("id") ?: return null
                val name = card.string("name") ?: return null
                val upright = when (card.string("orientation")) {
                    "upright" -> true
                    "reversed" -> false
                    else -> null
                }
                listOf(TarotCard(id = id, name = name, upright = upright))
            }

            TarotRequestType.TAROT_3.name -> {
                val cards = draw.arr("cards") ?: return null
                cards.map { cardRaw ->
                    val card = cardRaw as? JsonObject ?: return null
                    val id = card.string("id") ?: return null
                    val name = card.string("name") ?: return null
                    val upright = when (card.string("orientation")) {
                        "upright" -> true
                        "reversed" -> false
                        else -> null
                    }
                    TarotCard(id = id, name = name, upright = upright)
                }
            }

            else -> null
        }
    }

    private fun parseReading(reading: JsonObject): TarotReading? {
        val type = reading.string("type") ?: return null
        return when (type) {
            TarotRequestType.TAROT_1.name -> {
                val interpretation = reading.obj("interpretation") ?: return null
                val parts = listOf("theme", "meaning", "advice", "watchOut")
                    .mapNotNull { key -> interpretation.string(key) }
                if (parts.isEmpty()) return null
                TarotReading(parts.joinToString(separator = "\n\n"))
            }

            TarotRequestType.TAROT_3.name -> {
                val summary = reading.string("summary")
                val advice = reading.string("advice")
                val cards = reading.arr("cards")
                val cardMeanings = cards
                    ?.mapNotNull { it as? JsonObject }
                    ?.mapNotNull { card ->
                        val position = card.string("position")
                        val meaning = card.string("meaning")
                        if (position != null && meaning != null) "$position: $meaning" else null
                    }
                    .orEmpty()

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

    private fun JsonObject.string(key: String): String? =
        (this[key] as? JsonPrimitive)?.contentOrNull

    private fun JsonObject.obj(key: String): JsonObject? =
        this[key] as? JsonObject

    private fun JsonObject.arr(key: String): JsonArray? =
        this[key] as? JsonArray
}
