package com.agc.bwitch.data.rituals

import com.agc.bwitch.data.rituals.local.localRitualCategories
import com.agc.bwitch.data.rituals.local.localRitualDetails
import com.agc.bwitch.domain.rituals.RitualCatalogRepository
import com.agc.bwitch.domain.rituals.RitualCategory
import com.agc.bwitch.domain.rituals.RitualCategoryType
import com.agc.bwitch.domain.rituals.RitualDetail
import com.agc.bwitch.domain.rituals.RitualListItem

class LocalRitualCatalogRepository : RitualCatalogRepository {
    override fun getCategories(): List<RitualCategory> = localRitualCategories

    override fun getRitualsByCategory(category: RitualCategoryType): List<RitualListItem> {
        return localRitualDetails
            .filter { ritual -> ritual.category == category }
            .map { ritual ->
                RitualListItem(
                    id = ritual.id,
                    category = ritual.category,
                    title = ritual.title,
                    subtitle = ritual.subtitle,
                    materialsHint = ritual.materials.toHint(),
                )
            }
    }

    override fun getRitualById(id: String): RitualDetail? {
        return localRitualDetails.firstOrNull { ritual -> ritual.id == id }
    }
}

private fun List<String>.toHint(): String {
    if (isEmpty()) return "Materiales simples"
    if (size == 1) return first()

    val preview = take(2).joinToString(" + ")
    return if (size > 2) "$preview y más" else preview
}
