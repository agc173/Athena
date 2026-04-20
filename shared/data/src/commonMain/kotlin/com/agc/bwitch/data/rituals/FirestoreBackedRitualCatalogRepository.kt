package com.agc.bwitch.data.rituals

import com.agc.bwitch.data.storage.SettingsFactory
import com.agc.bwitch.domain.rituals.RitualCatalogRepository
import com.agc.bwitch.domain.rituals.RitualCategory
import com.agc.bwitch.domain.rituals.RitualCategoryType
import com.agc.bwitch.domain.rituals.RitualDetail
import com.agc.bwitch.domain.rituals.RitualListItem
import com.russhwolf.settings.Settings
import dev.gitlive.firebase.Firebase
import dev.gitlive.firebase.firestore.firestore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.datetime.Clock
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

class FirestoreBackedRitualCatalogRepository(
    private val local: LocalRitualCatalogRepository,
    settingsFactory: SettingsFactory,
) : RitualCatalogRepository {

    private val firestore = Firebase.firestore
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val settings: Settings = settingsFactory.create("bwitch_ritual_catalog")
    private val json = Json { ignoreUnknownKeys = true }

    private val categoriesKey = "ritual_categories_v1"
    private val ritualsKey = "rituals_v1"
    private val syncStaleMillis = 5 * 60 * 1_000L

    private val syncMutex = Mutex()

    private var lastSyncAtEpochMillis: Long? = null

    private var categoriesCache: List<RitualCategory> =
        readCategoriesFromSettings().ifEmpty { local.getCategories() }

    private var ritualDetailsCache: List<RitualDetail> =
        readRitualsFromSettings().ifEmpty { allLocalRituals() }


    private val localCategoriesByType: Map<RitualCategoryType, RitualCategory> by lazy {
        local.getCategories().associateBy { category -> category.type }
    }

    private val localListItemsById: Map<String, RitualListItem> by lazy {
        local.getCategories()
            .flatMap { category -> local.getRitualsByCategory(category.type) }
            .associateBy { item -> item.id }
    }

    private val localDetailsById: Map<String, RitualDetail> by lazy {
        allLocalRituals().associateBy { detail -> detail.id }
    }

    fun warmUp() {
        refreshIfNeeded()
    }

    fun refreshIfNeeded(force: Boolean = false) {
        val now = Clock.System.now().toEpochMilliseconds()
        if (!force && !shouldRefresh(now)) return

        if (!syncMutex.tryLock()) return

        scope.launch {
            var syncSucceeded = false
            val syncNow = Clock.System.now().toEpochMilliseconds()

            try {
                if (force || shouldRefresh(syncNow)) {
                    runCatching {
                        syncFromRemote()
                        syncSucceeded = true
                    }.onFailure { error ->
                        println("BWITCH_RITUAL_CATALOG sync failed: ${error.message}")
                    }
                }
                if (syncSucceeded) {
                    lastSyncAtEpochMillis = Clock.System.now().toEpochMilliseconds()
                }
            } finally {
                syncMutex.unlock()
            }
        }
    }

    override fun getCategories(): List<RitualCategory> {
        refreshIfNeeded()
        return categoriesCache
            .ifEmpty { local.getCategories() }
            .map { category -> category.withLocalContent() }
    }

    override fun getRitualsByCategory(category: RitualCategoryType): List<RitualListItem> {
        refreshIfNeeded()
        val source = ritualDetailsCache.ifEmpty { allLocalRituals() }
        return source
            .asSequence()
            .filter { ritual -> ritual.category == category }
            .map { ritual -> ritual.toListItem().withLocalContent() }
            .toList()
    }

    override fun getRitualById(id: String): RitualDetail? {
        refreshIfNeeded()
        val cached = ritualDetailsCache.firstOrNull { ritual -> ritual.id == id }
        return cached?.withLocalContent() ?: local.getRitualById(id)
    }

    private fun shouldRefresh(nowEpochMillis: Long): Boolean {
        val lastSync = lastSyncAtEpochMillis ?: return true
        return nowEpochMillis - lastSync >= syncStaleMillis
    }

    private suspend fun syncFromRemote() {
        val remoteCategories = fetchRemoteCategories()
        val categoryById = remoteCategories.associateBy { item -> item.id }

        if (remoteCategories.isNotEmpty()) {
            val categories = remoteCategories.map { item -> item.category }
            categoriesCache = categories
            saveCategoriesToSettings(categories)
        }

        val remoteRituals = fetchRemoteRituals(categoryById)
        if (remoteRituals.isNotEmpty()) {
            val rituals = remoteRituals.map { item -> item.detail }
            ritualDetailsCache = rituals
            saveRitualsToSettings(rituals)
        }
    }

    private suspend fun fetchRemoteCategories(): List<RemoteCategory> {
        return firestore.collection("ritualCategories").get().documents
            .mapNotNull { snap ->
                runCatching {
                    val dto = snap.data(FirestoreCategoryDto.serializer())
                    dto.toRemote(defaultId = snap.id)
                }.onFailure { error ->
                    println("BWITCH_RITUAL_CATALOG invalid category doc ${snap.id}: ${error.message}")
                }.getOrNull()
            }
            .filter { remote -> remote.isActive }
            .sortedWith(compareBy<RemoteCategory> { category -> category.sortOrder ?: Int.MAX_VALUE }
                .thenBy { category -> category.category.title })
    }

    private suspend fun fetchRemoteRituals(
        categoryById: Map<String, RemoteCategory>
    ): List<RemoteRitual> {
        return firestore.collection("rituals").get().documents
            .mapNotNull { snap ->
                runCatching {
                    val dto = snap.data(FirestoreRitualDto.serializer())
                    dto.toRemote(defaultId = snap.id, categoryById = categoryById)
                }.onFailure { error ->
                    println("BWITCH_RITUAL_CATALOG invalid ritual doc ${snap.id}: ${error.message}")
                }.getOrNull()
            }
            .filter { remote -> remote.isActive }
            .sortedWith(compareBy<RemoteRitual> { ritual -> ritual.sortOrder ?: Int.MAX_VALUE }
                .thenBy { ritual -> ritual.listItem.title })
    }

    private fun saveCategoriesToSettings(categories: List<RitualCategory>) {
        val payload = categories.map { category ->
            CachedCategoryDto(
                type = category.type.name,
                title = category.title,
                subtitle = category.subtitle,
            )
        }
        settings.putString(categoriesKey, json.encodeToString(ListSerializer(CachedCategoryDto.serializer()), payload))
    }

    private fun saveRitualsToSettings(rituals: List<RitualDetail>) {
        val payload = rituals.map { ritual ->
            CachedRitualDto(
                id = ritual.id,
                category = ritual.category.name,
                title = ritual.title,
                subtitle = ritual.subtitle,
                intention = ritual.intention,
                materials = ritual.materials,
                preparation = ritual.preparation,
                action = ritual.action,
                closing = ritual.closing,
                optionalNote = ritual.optionalNote,
            )
        }
        settings.putString(ritualsKey, json.encodeToString(ListSerializer(CachedRitualDto.serializer()), payload))
    }

    private fun readCategoriesFromSettings(): List<RitualCategory> {
        val raw = settings.getStringOrNull(categoriesKey) ?: return emptyList()
        return runCatching {
            json.decodeFromString(ListSerializer(CachedCategoryDto.serializer()), raw)
                .mapNotNull { dto -> dto.toDomain() }
        }.onFailure { error ->
            println("BWITCH_RITUAL_CATALOG categories cache parse failed: ${error.message}")
        }.getOrDefault(emptyList())
    }

    private fun readRitualsFromSettings(): List<RitualDetail> {
        val raw = settings.getStringOrNull(ritualsKey) ?: return emptyList()
        return runCatching {
            json.decodeFromString(ListSerializer(CachedRitualDto.serializer()), raw)
                .mapNotNull { dto -> dto.toDomain() }
        }.onFailure { error ->
            println("BWITCH_RITUAL_CATALOG rituals cache parse failed: ${error.message}")
        }.getOrDefault(emptyList())
    }

    private fun allLocalRituals(): List<RitualDetail> {
        return local.getCategories()
            .flatMap { category ->
                local.getRitualsByCategory(category.type)
                    .mapNotNull { item -> local.getRitualById(item.id) }
            }
    }

    private fun RitualCategory.withLocalContent(): RitualCategory =
        localCategoriesByType[type] ?: this

    private fun RitualListItem.withLocalContent(): RitualListItem =
        localListItemsById[id] ?: this

    private fun RitualDetail.withLocalContent(): RitualDetail =
        localDetailsById[id] ?: this
}

@Serializable
data class FirestoreCategoryDto(
    val id: String? = null,
    val type: String? = null,
    val categoryType: String? = null,
    val title: String,
    val subtitle: String,
    val sortOrder: Int? = null,
    val isActive: Boolean = true,
)

@Serializable
data class FirestoreRitualDto(
    val id: String? = null,
    val categoryId: String? = null,
    val categoryType: String? = null,
    val title: String,
    val subtitle: String,
    val intention: String,
    val materials: List<String> = emptyList(),
    val preparation: String? = null,
    val action: String,
    val closing: String,
    val optionalNote: String? = null,
    val materialsHint: String? = null,
    val sortOrder: Int? = null,
    val isActive: Boolean = true,
    val isPremium: Boolean = false,
)

private data class RemoteCategory(
    val id: String,
    val category: RitualCategory,
    val sortOrder: Int?,
    val isActive: Boolean,
)

private data class RemoteRitual(
    val detail: RitualDetail,
    val listItem: RitualListItem,
    val sortOrder: Int?,
    val isActive: Boolean,
)

private fun FirestoreCategoryDto.toRemote(defaultId: String): RemoteCategory? {
    val resolvedId = id ?: defaultId
    val rawType = type ?: categoryType ?: resolvedId
    val parsedType = rawType.toRitualCategoryTypeOrNull() ?: return null

    return RemoteCategory(
        id = resolvedId,
        category = RitualCategory(
            type = parsedType,
            title = title,
            subtitle = subtitle,
        ),
        sortOrder = sortOrder,
        isActive = isActive,
    )
}

private fun FirestoreRitualDto.toRemote(
    defaultId: String,
    categoryById: Map<String, RemoteCategory>,
): RemoteRitual? {
    val detail = toDetail(defaultId = defaultId, categoryById = categoryById) ?: return null

    return RemoteRitual(
        detail = detail,
        listItem = toListItem(defaultId = defaultId, categoryById = categoryById) ?: detail.toListItem(),
        sortOrder = sortOrder,
        isActive = isActive,
    )
}

private fun FirestoreRitualDto.toDetail(
    defaultId: String,
    categoryById: Map<String, RemoteCategory>,
): RitualDetail? {
    val parsedCategory = resolveCategoryType(categoryById) ?: return null
    val ritualId = id ?: defaultId

    return RitualDetail(
        id = ritualId,
        category = parsedCategory,
        title = title,
        subtitle = subtitle,
        intention = intention,
        materials = materials,
        preparation = preparation,
        action = action,
        closing = closing,
        optionalNote = optionalNote,
    )
}

private fun FirestoreRitualDto.toListItem(
    defaultId: String,
    categoryById: Map<String, RemoteCategory>,
): RitualListItem? {
    val parsedCategory = resolveCategoryType(categoryById) ?: return null
    val ritualId = id ?: defaultId

    return RitualListItem(
        id = ritualId,
        category = parsedCategory,
        title = title,
        subtitle = subtitle,
        materialsHint = materialsHint?.takeIf { it.isNotBlank() } ?: materials.toBwitchHint(),
    )
}

private fun FirestoreRitualDto.resolveCategoryType(
    categoryById: Map<String, RemoteCategory>
): RitualCategoryType? {
    val fromType = categoryType?.toRitualCategoryTypeOrNull()
    if (fromType != null) return fromType

    val linkedCategory = categoryId?.let(categoryById::get)?.category?.type
    if (linkedCategory != null) return linkedCategory

    return null
}

private fun RitualDetail.toListItem(): RitualListItem =
    RitualListItem(
        id = id,
        category = category,
        title = title,
        subtitle = subtitle,
        materialsHint = materials.toBwitchHint(),
    )

private fun String.toRitualCategoryTypeOrNull(): RitualCategoryType? {
    val normalized = lowercase()
    return RitualCategoryType.entries.firstOrNull { type -> type.name.lowercase() == normalized }
}

private fun List<String>.toBwitchHint(): String {
    if (isEmpty()) return "ritual_catalog.common.materials_simple"
    return take(2).joinToString(" · ")
}

@Serializable
private data class CachedCategoryDto(
    val type: String,
    val title: String,
    val subtitle: String,
) {
    fun toDomain(): RitualCategory? {
        val parsedType = type.toRitualCategoryTypeOrNull() ?: return null
        return RitualCategory(
            type = parsedType,
            title = title,
            subtitle = subtitle,
        )
    }
}

@Serializable
private data class CachedRitualDto(
    val id: String,
    val category: String,
    val title: String,
    val subtitle: String,
    val intention: String,
    val materials: List<String>,
    val preparation: String? = null,
    val action: String,
    val closing: String,
    val optionalNote: String? = null,
) {
    fun toDomain(): RitualDetail? {
        val parsedCategory = category.toRitualCategoryTypeOrNull() ?: return null
        return RitualDetail(
            id = id,
            category = parsedCategory,
            title = title,
            subtitle = subtitle,
            intention = intention,
            materials = materials,
            preparation = preparation,
            action = action,
            closing = closing,
            optionalNote = optionalNote,
        )
    }
}
