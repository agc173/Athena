package com.agc.bwitch.ui.tarot

import bwitch.composeapp.generated.resources.Res
import com.agc.bwitch.domain.tarot.TarotDeckId
import org.jetbrains.compose.resources.DrawableResource
import bwitch.composeapp.generated.resources.*

object TarotCardArt {

    const val cardBackAssetKey: String = "tarot_back_1"
    const val placeholderFaceAssetKey: String = "tarot_placeholder_face"
    val backDrawable: DrawableResource = Res.drawable.tarot_back_1

    private val riderWaiteFaceDrawableByCardId: Map<String, DrawableResource> = mapOf(
        "major-00-fool" to Res.drawable.deck_rider_waite_major_00_fool,
        "major-01-magician" to Res.drawable.deck_rider_waite_major_01_magician,
        "major-02-high-priestess" to Res.drawable.deck_rider_waite_major_02_high_priestess,
        "major-03-empress" to Res.drawable.deck_rider_waite_major_03_empress,
        "major-04-emperor" to Res.drawable.deck_rider_waite_major_04_emperor,
        "major-05-hierophant" to Res.drawable.deck_rider_waite_major_05_hierophant,
        "major-06-lovers" to Res.drawable.deck_rider_waite_major_06_lovers,
        "major-07-chariot" to Res.drawable.deck_rider_waite_major_07_chariot,
        "major-08-strength" to Res.drawable.deck_rider_waite_major_08_strength,
        "major-09-hermit" to Res.drawable.deck_rider_waite_major_09_hermit,
        "major-10-wheel-of-fortune" to Res.drawable.deck_rider_waite_major_10_wheel_of_fortune,
        "major-11-justice" to Res.drawable.deck_rider_waite_major_11_justice,
        "major-12-hanged-man" to Res.drawable.deck_rider_waite_major_12_hanged_man,
        "major-13-death" to Res.drawable.deck_rider_waite_major_13_death,
        "major-14-temperance" to Res.drawable.deck_rider_waite_major_14_temperance,
        "major-15-devil" to Res.drawable.deck_rider_waite_major_15_devil,
        "major-16-tower" to Res.drawable.deck_rider_waite_major_16_tower,
        "major-17-star" to Res.drawable.deck_rider_waite_major_17_star,
        "major-18-moon" to Res.drawable.deck_rider_waite_major_18_moon,
        "major-19-sun" to Res.drawable.deck_rider_waite_major_19_sun,
        "major-20-judgement" to Res.drawable.deck_rider_waite_major_20_judgement,
        "major-21-world" to Res.drawable.deck_rider_waite_major_21_world,
        "minor-ace-cups" to Res.drawable.deck_rider_waite_minor_ace_cups,
        "minor-ace-pentacles" to Res.drawable.deck_rider_waite_minor_ace_pentacles,
        "minor-ace-swords" to Res.drawable.deck_rider_waite_minor_ace_swords,
        "minor-ace-wands" to Res.drawable.deck_rider_waite_minor_ace_wands,
        "minor-two-cups" to Res.drawable.deck_rider_waite_minor_two_cups,
        "minor-two-pentacles" to Res.drawable.deck_rider_waite_minor_two_pentacles,
        "minor-two-swords" to Res.drawable.deck_rider_waite_minor_two_swords,
        "minor-two-wands" to Res.drawable.deck_rider_waite_minor_two_wands,
        "minor-three-cups" to Res.drawable.deck_rider_waite_minor_three_cups,
        "minor-three-pentacles" to Res.drawable.deck_rider_waite_minor_three_pentacles,
        "minor-three-swords" to Res.drawable.deck_rider_waite_minor_three_swords,
        "minor-three-wands" to Res.drawable.deck_rider_waite_minor_three_wands,
        "minor-four-cups" to Res.drawable.deck_rider_waite_minor_four_cups,
        "minor-four-pentacles" to Res.drawable.deck_rider_waite_minor_four_pentacles,
        "minor-four-swords" to Res.drawable.deck_rider_waite_minor_four_swords,
        "minor-four-wands" to Res.drawable.deck_rider_waite_minor_four_wands,
        "minor-five-cups" to Res.drawable.deck_rider_waite_minor_five_cups,
        "minor-five-pentacles" to Res.drawable.deck_rider_waite_minor_five_pentacles,
        "minor-five-swords" to Res.drawable.deck_rider_waite_minor_five_swords,
        "minor-five-wands" to Res.drawable.deck_rider_waite_minor_five_wands,
        "minor-six-cups" to Res.drawable.deck_rider_waite_minor_six_cups,
        "minor-six-pentacles" to Res.drawable.deck_rider_waite_minor_six_pentacles,
        "minor-six-swords" to Res.drawable.deck_rider_waite_minor_six_swords,
        "minor-six-wands" to Res.drawable.deck_rider_waite_minor_six_wands,
        "minor-seven-cups" to Res.drawable.deck_rider_waite_minor_seven_cups,
        "minor-seven-pentacles" to Res.drawable.deck_rider_waite_minor_seven_pentacles,
        "minor-seven-swords" to Res.drawable.deck_rider_waite_minor_seven_swords,
        "minor-seven-wands" to Res.drawable.deck_rider_waite_minor_seven_wands,
        "minor-eight-cups" to Res.drawable.deck_rider_waite_minor_eight_cups,
        "minor-eight-pentacles" to Res.drawable.deck_rider_waite_minor_eight_pentacles,
        "minor-eight-swords" to Res.drawable.deck_rider_waite_minor_eight_swords,
        "minor-eight-wands" to Res.drawable.deck_rider_waite_minor_eight_wands,
        "minor-nine-cups" to Res.drawable.deck_rider_waite_minor_nine_cups,
        "minor-nine-pentacles" to Res.drawable.deck_rider_waite_minor_nine_pentacles,
        "minor-nine-swords" to Res.drawable.deck_rider_waite_minor_nine_swords,
        "minor-nine-wands" to Res.drawable.deck_rider_waite_minor_nine_wands,
        "minor-ten-cups" to Res.drawable.deck_rider_waite_minor_ten_cups,
        "minor-ten-pentacles" to Res.drawable.deck_rider_waite_minor_ten_pentacles,
        "minor-ten-swords" to Res.drawable.deck_rider_waite_minor_ten_swords,
        "minor-ten-wands" to Res.drawable.deck_rider_waite_minor_ten_wands,
        "minor-page-cups" to Res.drawable.deck_rider_waite_minor_page_cups,
        "minor-page-pentacles" to Res.drawable.deck_rider_waite_minor_page_pentacles,
        "minor-page-swords" to Res.drawable.deck_rider_waite_minor_page_swords,
        "minor-page-wands" to Res.drawable.deck_rider_waite_minor_page_wands,
        "minor-knight-cups" to Res.drawable.deck_rider_waite_minor_knight_cups,
        "minor-knight-pentacles" to Res.drawable.deck_rider_waite_minor_knight_pentacles,
        "minor-knight-swords" to Res.drawable.deck_rider_waite_minor_knight_swords,
        "minor-knight-wands" to Res.drawable.deck_rider_waite_minor_knight_wands,
        "minor-queen-cups" to Res.drawable.deck_rider_waite_minor_queen_cups,
        "minor-queen-pentacles" to Res.drawable.deck_rider_waite_minor_queen_pentacles,
        "minor-queen-swords" to Res.drawable.deck_rider_waite_minor_queen_swords,
        "minor-queen-wands" to Res.drawable.deck_rider_waite_minor_queen_wands,
        "minor-king-cups" to Res.drawable.deck_rider_waite_minor_king_cups,
        "minor-king-pentacles" to Res.drawable.deck_rider_waite_minor_king_pentacles,
        "minor-king-swords" to Res.drawable.deck_rider_waite_minor_king_swords,
        "minor-king-wands" to Res.drawable.deck_rider_waite_minor_king_wands,
    )

    private val arcanaNoctisFaceDrawableByCardId: Map<String, DrawableResource> = mapOf(
        "major-00-fool" to Res.drawable.deck_arcana_noctis_major_00_fool,
        "major-01-magician" to Res.drawable.deck_arcana_noctis_major_01_magician,
        "major-02-high-priestess" to Res.drawable.deck_arcana_noctis_major_02_high_priestess,
        "major-03-empress" to Res.drawable.deck_arcana_noctis_major_03_empress,
        "major-04-emperor" to Res.drawable.deck_arcana_noctis_major_04_emperor,
        "major-05-hierophant" to Res.drawable.deck_arcana_noctis_major_05_hierophant,
        "major-06-lovers" to Res.drawable.deck_arcana_noctis_major_06_lovers,
        "major-07-chariot" to Res.drawable.deck_arcana_noctis_major_07_chariot,
        "major-08-strength" to Res.drawable.deck_arcana_noctis_major_08_strength,
        "major-09-hermit" to Res.drawable.deck_arcana_noctis_major_09_hermit,
        "major-10-wheel-of-fortune" to Res.drawable.deck_arcana_noctis_major_10_wheel_of_fortune,
        "major-11-justice" to Res.drawable.deck_arcana_noctis_major_11_justice,
        "major-12-hanged-man" to Res.drawable.deck_arcana_noctis_major_12_hanged_man,
        "major-13-death" to Res.drawable.deck_arcana_noctis_major_13_death,
        "major-14-temperance" to Res.drawable.deck_arcana_noctis_major_14_temperance,
        "major-15-devil" to Res.drawable.deck_arcana_noctis_major_15_devil,
        "major-16-tower" to Res.drawable.deck_arcana_noctis_major_16_tower,
        "major-17-star" to Res.drawable.deck_arcana_noctis_major_17_star,
        "major-18-moon" to Res.drawable.deck_arcana_noctis_major_18_moon,
        "major-19-sun" to Res.drawable.deck_arcana_noctis_major_19_sun,
        "major-20-judgement" to Res.drawable.deck_arcana_noctis_major_20_judgement,
        "major-21-world" to Res.drawable.deck_arcana_noctis_major_21_world,
        "minor-ace-cups" to Res.drawable.deck_arcana_noctis_minor_ace_cups,
        "minor-ace-pentacles" to Res.drawable.deck_arcana_noctis_minor_ace_pentacles,
        "minor-ace-swords" to Res.drawable.deck_arcana_noctis_minor_ace_swords,
        "minor-ace-wands" to Res.drawable.deck_arcana_noctis_minor_ace_wands,
        "minor-two-cups" to Res.drawable.deck_arcana_noctis_minor_two_cups,
        "minor-two-pentacles" to Res.drawable.deck_arcana_noctis_minor_two_pentacles,
        "minor-two-swords" to Res.drawable.deck_arcana_noctis_minor_two_swords,
        "minor-two-wands" to Res.drawable.deck_arcana_noctis_minor_two_wands,
        "minor-three-cups" to Res.drawable.deck_arcana_noctis_minor_three_cups,
        "minor-three-pentacles" to Res.drawable.deck_arcana_noctis_minor_three_pentacles,
        "minor-three-swords" to Res.drawable.deck_arcana_noctis_minor_three_swords,
        "minor-three-wands" to Res.drawable.deck_arcana_noctis_minor_three_wands,
        "minor-four-cups" to Res.drawable.deck_arcana_noctis_minor_four_cups,
        "minor-four-pentacles" to Res.drawable.deck_arcana_noctis_minor_four_pentacles,
        "minor-four-swords" to Res.drawable.deck_arcana_noctis_minor_four_swords,
        "minor-four-wands" to Res.drawable.deck_arcana_noctis_minor_four_wands,
        "minor-five-cups" to Res.drawable.deck_arcana_noctis_minor_five_cups,
        "minor-five-pentacles" to Res.drawable.deck_arcana_noctis_minor_five_pentacles,
        "minor-five-swords" to Res.drawable.deck_arcana_noctis_minor_five_swords,
        "minor-five-wands" to Res.drawable.deck_arcana_noctis_minor_five_wands,
        "minor-six-cups" to Res.drawable.deck_arcana_noctis_minor_six_cups,
        "minor-six-pentacles" to Res.drawable.deck_arcana_noctis_minor_six_pentacles,
        "minor-six-swords" to Res.drawable.deck_arcana_noctis_minor_six_swords,
        "minor-six-wands" to Res.drawable.deck_arcana_noctis_minor_six_wands,
        "minor-seven-cups" to Res.drawable.deck_arcana_noctis_minor_seven_cups,
        "minor-seven-pentacles" to Res.drawable.deck_arcana_noctis_minor_seven_pentacles,
        "minor-seven-swords" to Res.drawable.deck_arcana_noctis_minor_seven_swords,
        "minor-seven-wands" to Res.drawable.deck_arcana_noctis_minor_seven_wands,
        "minor-eight-cups" to Res.drawable.deck_arcana_noctis_minor_eight_cups,
        "minor-eight-pentacles" to Res.drawable.deck_arcana_noctis_minor_eight_pentacles,
        "minor-eight-swords" to Res.drawable.deck_arcana_noctis_minor_eight_swords,
        "minor-eight-wands" to Res.drawable.deck_arcana_noctis_minor_eight_wands,
        "minor-nine-cups" to Res.drawable.deck_arcana_noctis_minor_nine_cups,
        "minor-nine-pentacles" to Res.drawable.deck_arcana_noctis_minor_nine_pentacles,
        "minor-nine-swords" to Res.drawable.deck_arcana_noctis_minor_nine_swords,
        "minor-nine-wands" to Res.drawable.deck_arcana_noctis_minor_nine_wands,
        "minor-ten-cups" to Res.drawable.deck_arcana_noctis_minor_ten_cups,
        "minor-ten-pentacles" to Res.drawable.deck_arcana_noctis_minor_ten_pentacles,
        "minor-ten-swords" to Res.drawable.deck_arcana_noctis_minor_ten_swords,
        "minor-ten-wands" to Res.drawable.deck_arcana_noctis_minor_ten_wands,
        "minor-page-cups" to Res.drawable.deck_arcana_noctis_minor_page_cups,
        "minor-page-pentacles" to Res.drawable.deck_arcana_noctis_minor_page_pentacles,
        "minor-page-swords" to Res.drawable.deck_arcana_noctis_minor_page_swords,
        "minor-page-wands" to Res.drawable.deck_arcana_noctis_minor_page_wands,
        "minor-knight-cups" to Res.drawable.deck_arcana_noctis_minor_knight_cups,
        "minor-knight-pentacles" to Res.drawable.deck_arcana_noctis_minor_knight_pentacles,
        "minor-knight-swords" to Res.drawable.deck_arcana_noctis_minor_knight_swords,
        "minor-knight-wands" to Res.drawable.deck_arcana_noctis_minor_knight_wands,
        "minor-queen-cups" to Res.drawable.deck_arcana_noctis_minor_queen_cups,
        "minor-queen-pentacles" to Res.drawable.deck_arcana_noctis_minor_queen_pentacles,
        "minor-queen-swords" to Res.drawable.deck_arcana_noctis_minor_queen_swords,
        "minor-queen-wands" to Res.drawable.deck_arcana_noctis_minor_queen_wands,
        "minor-king-cups" to Res.drawable.deck_arcana_noctis_minor_king_cups,
        "minor-king-pentacles" to Res.drawable.deck_arcana_noctis_minor_king_pentacles,
        "minor-king-swords" to Res.drawable.deck_arcana_noctis_minor_king_swords,
        "minor-king-wands" to Res.drawable.deck_arcana_noctis_minor_king_wands,
    )

    private val faceDrawableByDeckAndCardId: Map<TarotDeckId, Map<String, DrawableResource>> = mapOf(
        TarotDeckId.RIDER_WAITE to riderWaiteFaceDrawableByCardId,
        TarotDeckId.ARCANA_NOCTIS to arcanaNoctisFaceDrawableByCardId,
    )

    fun faceDrawableForCardId(cardId: String?): DrawableResource? =
        faceDrawableForCardId(deckId = TarotDeckId.RIDER_WAITE, cardId = cardId)

    fun faceDrawableForCardId(deckId: TarotDeckId, cardId: String?): DrawableResource? {
        val resolvedDeck = if (TarotDeckRegistry.getById(deckId) != null) deckId else TarotDeckRegistry.defaultDeck.id
        val deckMap = faceDrawableByDeckAndCardId[resolvedDeck]
            ?: faceDrawableByDeckAndCardId[TarotDeckRegistry.defaultDeck.id]
            ?: return null
        return cardId?.let(deckMap::get)
    }

    fun hasSpecificFace(cardId: String?): Boolean =
        hasSpecificFace(deckId = TarotDeckId.RIDER_WAITE, cardId = cardId)

    fun hasSpecificFace(deckId: TarotDeckId, cardId: String?): Boolean =
        faceDrawableForCardId(deckId = deckId, cardId = cardId) != null

    fun allCardIds(deckId: TarotDeckId): List<String> =
        faceDrawableByDeckAndCardId[deckId]?.keys?.sorted().orEmpty()
}
