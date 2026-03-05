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

class TarotRepositoryImpl(
    private val functionsClient: FunctionsClient,
) : TarotRepository {

    override suspend fun tarotDraw(
        requestId: String,
        type: TarotRequestType,
        lang: String?,
        question: String?,
    ): ApiResult<TarotDrawResponse> {
        val payload = buildMap<String, Any> {
            put("requestType", type.name)
            put("requestId", requestId)
            lang?.let { put("lang", it) }
            question?.let { put("question", it) }
            // Temporary dev-only hack until real rewarded ad proof / SSV validation is implemented.
            if (BuildInfo.isDebug) {
                put("adUnlock", mapOf("rewardedProof" to "dev-test-proof"))
            }
        }

        return when (val result = functionsClient.call("tarotDraw", payload)) {
            is ApiResult.Err -> result
            is ApiResult.Ok -> parseResponse(result.value)
        }
    }

    private fun parseResponse(response: Map<String, Any?>): ApiResult<TarotDrawResponse> {
        val requestId = response["requestId"] as? String
            ?: return ApiResult.Err(ApiError.Internal("Invalid response: missing requestId"))
        val status = response["status"] as? String
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
            val errorMessage = (response["error"] as? Map<*, *>)?.get("message") as? String
            return ApiResult.Err(ApiError.Internal(errorMessage ?: "Request failed"))
        }

        val draw = response["draw"] as? Map<*, *>
            ?: return ApiResult.Err(ApiError.Internal("Invalid response: missing draw"))
        val reading = response["reading"] as? Map<*, *>
            ?: return ApiResult.Err(ApiError.Internal("Invalid response: missing reading"))

        val cards = parseCards(
            draw = draw,
            requestTypeHint = response["requestType"] as? String,
            readingTypeHint = reading["type"] as? String,
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
        draw: Map<*, *>,
        requestTypeHint: String? = null,
        readingTypeHint: String? = null,
    ): List<TarotCard>? {
        val inferredType = (draw["type"] as? String)
            ?: requestTypeHint
            ?: readingTypeHint
            ?: when {
                draw["card"] is Map<*, *> -> TarotRequestType.TAROT_1.name
                draw["cards"] is List<*> -> TarotRequestType.TAROT_3.name
                else -> null
            }

        return when (inferredType) {
            TarotRequestType.TAROT_1.name -> {
                val card = draw["card"] as? Map<*, *> ?: return null
                val id = card["id"] as? String ?: return null
                val name = card["name"] as? String ?: return null
                val upright = when (card["orientation"] as? String) {
                    "upright" -> true
                    "reversed" -> false
                    else -> null
                }
                listOf(TarotCard(id = id, name = name, upright = upright))
            }

            TarotRequestType.TAROT_3.name -> {
                val cards = draw["cards"] as? List<*> ?: return null
                cards.map { cardRaw ->
                    val card = cardRaw as? Map<*, *> ?: return null
                    val id = card["id"] as? String ?: return null
                    val name = card["name"] as? String ?: return null
                    val upright = when (card["orientation"] as? String) {
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

    private fun parseReading(reading: Map<*, *>): TarotReading? {
        val type = reading["type"] as? String ?: return null
        return when (type) {
            TarotRequestType.TAROT_1.name -> {
                val interpretation = reading["interpretation"] as? Map<*, *> ?: return null
                val parts = listOf("theme", "meaning", "advice", "watchOut")
                    .mapNotNull { key -> interpretation[key] as? String }
                if (parts.isEmpty()) return null
                TarotReading(parts.joinToString(separator = "\n\n"))
            }

            TarotRequestType.TAROT_3.name -> {
                val summary = reading["summary"] as? String
                val advice = reading["advice"] as? String
                val cards = reading["cards"] as? List<*>
                val cardMeanings = cards
                    ?.mapNotNull { it as? Map<*, *> }
                    ?.mapNotNull { card ->
                        val position = card["position"] as? String
                        val meaning = card["meaning"] as? String
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
}
