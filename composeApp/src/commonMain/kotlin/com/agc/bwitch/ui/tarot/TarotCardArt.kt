package com.agc.bwitch.ui.tarot

/**
 * Tarot art resolver.
 *
 * This phase is intentionally Compose-only (no hard dependency on binary image assets)
 * so the tarot flow compiles and runs even when final art files are not available yet.
 *
 * Future art integration:
 * - Place files under composeApp/src/commonMain/composeResources/drawable/
 * - Recommended naming: tarot_<normalized_card_id>.webp
 * - Wire real resources in the UI layer using these asset keys.
 */
object TarotCardArt {

    /**
     * Expected drawable key for a future real card-back asset.
     * Current UI uses a Compose-drawn back to avoid fragile binary dependencies.
     */
    const val cardBackAssetKey: String = "tarot_back_bw"

    /**
     * Expected drawable key for a future premium placeholder background art.
     */
    const val placeholderFaceAssetKey: String = "tarot_placeholder_face"

    /**
     * Maps normalized backend tarot ids to expected drawable keys.
     *
     * NOTE: backend ids may vary (e.g. "the_moon", "moon", "XVIII").
     */
    private val knownFaceAssetKeys: Map<String, String> = mapOf(
        "moon" to "tarot_moon",
        "the_moon" to "tarot_moon",
        "major_18_the_moon" to "tarot_moon",
        "xviii" to "tarot_moon",
        "18" to "tarot_moon",
    )

    fun faceAssetKeyForCardId(cardId: String?): String? {
        val normalized = normalizeCardId(cardId)
        return knownFaceAssetKeys[normalized]
    }

    fun hasSpecificFace(cardId: String?): Boolean = faceAssetKeyForCardId(cardId) != null

    private fun normalizeCardId(cardId: String?): String {
        return cardId
            .orEmpty()
            .trim()
            .lowercase()
            .replace("á", "a")
            .replace("é", "e")
            .replace("í", "i")
            .replace("ó", "o")
            .replace("ú", "u")
            .replace("ñ", "n")
            .replace(Regex("[^a-z0-9]+"), "_")
            .trim('_')
    }
}
