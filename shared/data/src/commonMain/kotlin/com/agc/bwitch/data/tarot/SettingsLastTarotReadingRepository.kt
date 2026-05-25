package com.agc.bwitch.data.tarot

import com.agc.bwitch.data.storage.SettingsFactory
import com.agc.bwitch.domain.tarot.LastTarotReadingRepository
import com.agc.bwitch.domain.tarot.Tarot3CardMeaning
import com.agc.bwitch.domain.tarot.TarotCard
import com.agc.bwitch.domain.tarot.TarotCardPosition
import com.agc.bwitch.domain.tarot.TarotDeckId
import com.agc.bwitch.domain.tarot.TarotDrawResponse
import com.agc.bwitch.domain.tarot.TarotReadingDetails
import com.russhwolf.settings.Settings
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

class SettingsLastTarotReadingRepository(
    settingsFactory: SettingsFactory,
) : LastTarotReadingRepository {
    private val settings: Settings = settingsFactory.create("bwitch_tarot")

    override fun get(): TarotDrawResponse? =
        settings.getStringOrNull(KEY_LAST_READING)?.let(::decode)

    override fun save(response: TarotDrawResponse) {
        settings.putString(KEY_LAST_READING, encode(response))
    }

    private fun encode(response: TarotDrawResponse): String =
        Json.encodeToString(
            JsonObject.serializer(),
            buildJsonObject {
                put("requestId", JsonPrimitive(response.requestId))
                put("status", JsonPrimitive(response.status))
                put("interpretation", JsonPrimitive(response.interpretation))
                put("deckId", JsonPrimitive(response.deckId.value))
                put("cards", buildJsonArray {
                    response.cards.forEach { card ->
                        add(buildJsonObject {
                            put("id", JsonPrimitive(card.id))
                            put("name", JsonPrimitive(card.name))
                            card.upright?.let { put("upright", JsonPrimitive(it)) }
                            card.position?.let { put("position", JsonPrimitive(it.name)) }
                        })
                    }
                })
                response.details?.let { details -> put("details", encodeDetails(details)) }
            },
        )

    private fun encodeDetails(details: TarotReadingDetails): JsonObject = when (details) {
        is TarotReadingDetails.Tarot1ReadingDetails -> buildJsonObject {
            put("type", JsonPrimitive("tarot1"))
            put("theme", JsonPrimitive(details.theme))
            put("meaning", JsonPrimitive(details.meaning))
            put("advice", JsonPrimitive(details.advice))
            put("watchOut", JsonPrimitive(details.watchOut))
        }

        is TarotReadingDetails.Tarot3ReadingDetails -> buildJsonObject {
            put("type", JsonPrimitive("tarot3"))
            put("cards", buildJsonArray {
                details.cards.forEach { meaning ->
                    add(buildJsonObject {
                        put("position", JsonPrimitive(meaning.position.name))
                        put("meaning", JsonPrimitive(meaning.meaning))
                    })
                }
            })
            put("summary", JsonPrimitive(details.summary))
            put("advice", JsonPrimitive(details.advice))
        }
    }

    private fun decode(raw: String): TarotDrawResponse? = runCatching {
        val root = Json.parseToJsonElement(raw).jsonObject
        val cards = root["cards"]?.jsonArray.orEmpty().mapNotNull { node ->
            val card = node.jsonObject
            val id = card["id"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val name = card["name"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            TarotCard(
                id = id,
                name = name,
                upright = card["upright"]?.jsonPrimitive?.booleanOrNull,
                position = card["position"]?.jsonPrimitive?.contentOrNull?.let { TarotCardPosition.valueOf(it) },
            )
        }
        TarotDrawResponse(
            requestId = root["requestId"]?.jsonPrimitive?.contentOrNull ?: return null,
            status = root["status"]?.jsonPrimitive?.contentOrNull ?: return null,
            cards = cards,
            details = root["details"]?.jsonObject?.let(::decodeDetails),
            interpretation = root["interpretation"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            deckId = TarotDeckId.fromValue(root["deckId"]?.jsonPrimitive?.contentOrNull) ?: TarotDeckId.RIDER_WAITE,
        )
    }.getOrNull()

    private fun decodeDetails(node: JsonObject): TarotReadingDetails? = when (node["type"]?.jsonPrimitive?.contentOrNull) {
        "tarot1" -> TarotReadingDetails.Tarot1ReadingDetails(
            theme = node["theme"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            meaning = node["meaning"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            advice = node["advice"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            watchOut = node["watchOut"]?.jsonPrimitive?.contentOrNull.orEmpty(),
        )

        "tarot3" -> TarotReadingDetails.Tarot3ReadingDetails(
            cards = node["cards"]?.jsonArray.orEmpty().mapNotNull { item ->
                val obj = item.jsonObject
                val position = obj["position"]?.jsonPrimitive?.contentOrNull?.let { TarotCardPosition.valueOf(it) } ?: return@mapNotNull null
                Tarot3CardMeaning(position = position, meaning = obj["meaning"]?.jsonPrimitive?.contentOrNull.orEmpty())
            },
            summary = node["summary"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            advice = node["advice"]?.jsonPrimitive?.contentOrNull.orEmpty(),
        )

        else -> null
    }

    private companion object {
        const val KEY_LAST_READING = "last_reading"
    }
}

private fun JsonArray?.orEmpty(): JsonArray = this ?: JsonArray(emptyList())
