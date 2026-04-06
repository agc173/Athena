package com.agc.bwitch.domain.rituals

enum class RitualCategoryType {
    Love,
    Prosperity,
    Protection,
    Cleansing,
}

data class RitualCategory(
    val type: RitualCategoryType,
    val title: String,
    val subtitle: String,
)

data class RitualListItem(
    val id: String,
    val category: RitualCategoryType,
    val title: String,
    val subtitle: String,
    val materialsHint: String,
)

data class RitualDetail(
    val id: String,
    val category: RitualCategoryType,
    val title: String,
    val subtitle: String,
    val intention: String,
    val materials: List<String>,
    val preparation: String? = null,
    val action: String,
    val closing: String,
    val optionalNote: String? = null,
)
