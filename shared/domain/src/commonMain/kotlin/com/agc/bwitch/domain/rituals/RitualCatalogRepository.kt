package com.agc.bwitch.domain.rituals

interface RitualCatalogRepository {
    fun getCategories(): List<RitualCategory>
    fun getRitualsByCategory(category: RitualCategoryType): List<RitualListItem>
    fun getRitualById(id: String): RitualDetail?
}
