package com.agc.bwitch.ui.tarot

import org.jetbrains.compose.resources.DrawableResource

object TarotCardArt {

    const val cardBackAssetKey: String = "tarot_back_bw"
    const val placeholderFaceAssetKey: String = "tarot_placeholder_face"

    fun faceDrawableForCardId(cardId: String?): DrawableResource? = null

    fun hasSpecificFace(cardId: String?): Boolean = false
}
