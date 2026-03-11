package com.agc.bwitch.ui.tarot

import bwich.composeapp.generated.resources.Res
import org.jetbrains.compose.resources.DrawableResource

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

    fun faceDrawableForCardId(cardId: String?): DrawableResource? {
        val normalized = normalizeCardId(cardId)
        if (normalized.isBlank()) return null
        return Res.allDrawableResources[normalized]
    }

    fun hasSpecificFace(cardId: String?): Boolean = faceDrawableForCardId(cardId) != null

    private fun normalizeCardId(cardId: String?): String {
        return cardId
            .orEmpty()
            .trim()
            .lowercase()
            .replace('-', '_')
    }
}
